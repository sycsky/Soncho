package com.example.aikef.workflow.node;

import com.example.aikef.extraction.model.FieldDefinition;
import com.example.aikef.tool.model.AiTool;
import com.example.aikef.tool.repository.AiToolRepository;
import com.example.aikef.tool.service.AiToolService;
import com.example.aikef.workflow.context.WorkflowContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeSwitchComponent;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 工具节点（Switch 类型）
 * 可配置系统里面的工具，流转到此节点时，从上下文 toolsParams Map 中
 * 以工具 name 作为 key 查找工具所需要的参数
 * 如果参数匹配则调用工具，否则不执行工具
 * 
 * 有两条固定路径：
 * - "tag:executed": 执行了工具
 * - "tag:not_executed": 未匹配参数未执行工具
 * 
 * 配置示例:
 * {
 *   "toolId": "uuid-of-tool",
 *   "toolName": "tool_name"  // 可选，如果提供则优先使用 toolName 查找工具
 * }
 */
@LiteflowComponent("tool")
public class ToolNode extends NodeSwitchComponent {

    private static final Logger log = LoggerFactory.getLogger(ToolNode.class);

    @Resource
    private AiToolRepository toolRepository;

    @Resource
    private AiToolService toolService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public String processSwitch() throws Exception {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = this.getContextBean(WorkflowContext.class);
        String actualNodeId = getActualNodeId();
        JsonNode config = ctx.getNodeConfig(actualNodeId);

        // 获取工具ID或工具名称
        String toolIdStr = getConfigValue(config, "toolId", null);
        String toolName = getConfigValue(config, "toolName", null);

        if (toolIdStr == null && toolName == null) {
            log.error("工具节点未配置 toolId 或 toolName");
            recordExecution(ctx, actualNodeId, null, "error", startTime, false, "toolId or toolName not configured");
            return "tag:not_executed";
        }

        // 查找工具
        AiTool tool = null;
        if (toolName != null && !toolName.isEmpty()) {
            tool = toolRepository.findByNameWithSchema(toolName).orElse(null);
        }
        if (tool == null && toolIdStr != null && !toolIdStr.isEmpty()) {
            tool = toolRepository.findByIdWithSchema(UUID.fromString(toolIdStr)).orElse(null);
        }

        if (tool == null) {
            log.error("工具不存在: toolId={}, toolName={}", toolIdStr, toolName);
            recordExecution(ctx, actualNodeId, null, "error", startTime, false, "tool not found");
            return "tag:not_executed";
        }

        if (!tool.getEnabled()) {
            log.error("工具已禁用: toolName={}", tool.getName());
            recordExecution(ctx, actualNodeId, tool.getName(), "error", startTime, false, "tool is disabled");
            return "tag:not_executed";
        }

        // 从上下文 toolsParams 中获取工具参数
        // key 是工具名称（tool.getName()）
        Map<String, Object> toolParams = ctx.getToolParams(tool.getName());
        
        if (toolParams == null || toolParams.isEmpty()) {
            log.info("工具参数未找到: toolName={}", tool.getName());
            recordExecution(ctx, actualNodeId, tool.getName(), "tag:not_executed", startTime, true, null);
            return "tag:not_executed";
        }

        // 获取工具参数定义
        List<FieldDefinition> paramDefs = getToolParameters(tool);
        List<String> requiredParams = paramDefs.stream()
                .filter(FieldDefinition::isRequired)
                .map(FieldDefinition::getName)
                .toList();

        // 检查必填参数是否都存在
        boolean allRequiredParamsPresent = true;
        List<String> missingParams = new ArrayList<>();
        
        for (String param : requiredParams) {
            Object value = toolParams.get(param);
            boolean isMissing = !toolParams.containsKey(param) || value == null || 
                    "null".equals(value) || "Null".equals(value) ||
                    (value instanceof String && ((String) value).isEmpty());
            if (isMissing) {
                allRequiredParamsPresent = false;
                missingParams.add(param);
            }
        }

        if (!allRequiredParamsPresent) {
            log.info("工具必填参数缺失: toolName={}, missingParams={}", tool.getName(), missingParams);
            recordExecution(ctx, actualNodeId, tool.getName(), "tag:not_executed", startTime, true, 
                    "missing required params: " + missingParams);
            return "tag:not_executed";
        }

        // 参数匹配，执行工具
        try {
            log.info("执行工具: toolName={}, params={}", tool.getName(), toolParams);
            
            AiToolService.ToolExecutionResult result = toolService.executeTool(
                    tool.getId(),
                    toolParams,
                    ctx.getSessionId(),
                    null
            );

            String output = result.success() ? 
                    (result.output() != null ? result.output() : "工具执行成功") : 
                    ("工具执行失败: " + (result.errorMessage() != null ? result.errorMessage() : "未知错误"));

            setOutput(ctx, actualNodeId, output);
            recordExecution(ctx, actualNodeId, tool.getName(), "tag:executed", startTime, result.success(), 
                    result.errorMessage());

            if (result.success()) {
                log.info("工具执行成功: toolName={}, output={}", tool.getName(), result.output());
                return "tag:executed";
            } else {
                log.warn("工具执行失败: toolName={}, error={}", tool.getName(), result.errorMessage());
                return "tag:not_executed";
            }

        } catch (Exception e) {
            log.error("工具执行异常: toolName={}", tool.getName(), e);
            recordExecution(ctx, actualNodeId, tool.getName(), "error", startTime, false, e.getMessage());
            return "tag:not_executed";
        }
    }

    /**
     * 获取工具参数定义
     */
    private List<FieldDefinition> getToolParameters(AiTool tool) {
        if (tool.getSchema() == null || tool.getSchema().getFieldsJson() == null) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(
                    tool.getSchema().getFieldsJson(),
                    new TypeReference<List<FieldDefinition>>() {}
            );
        } catch (Exception e) {
            log.warn("解析工具参数失败: tool={}", tool.getName());
            return Collections.emptyList();
        }
    }

    /**
     * 获取配置值
     */
    private String getConfigValue(JsonNode config, String key, String defaultValue) {
        if (config == null || !config.has(key)) {
            return defaultValue;
        }
        JsonNode value = config.get(key);
        if (value.isNull()) {
            return defaultValue;
        }
        return value.asText();
    }

    /**
     * 获取实际节点ID
     */
    private String getActualNodeId() {
        String tag = this.getTag();
        if (tag != null && !tag.isEmpty()) {
            return tag;
        }
        return this.getNodeId();
    }

    /**
     * 设置节点输出
     */
    private void setOutput(WorkflowContext ctx, String nodeId, Object output) {
        ctx.setOutput(nodeId, output);
    }

    /**
     * 记录节点执行详情
     */
    private void recordExecution(WorkflowContext ctx, String nodeId, String toolName, 
                                Object output, long startTime, boolean success, String errorMessage) {
        WorkflowContext.NodeExecutionDetail detail = new WorkflowContext.NodeExecutionDetail();
        detail.setNodeId(nodeId);
        detail.setNodeType("tool");
        detail.setNodeName(this.getName());
        detail.setInput(toolName);
        detail.setOutput(output);
        detail.setStartTime(startTime);
        detail.setEndTime(System.currentTimeMillis());
        detail.setDurationMs(detail.getEndTime() - startTime);
        detail.setSuccess(success);
        detail.setErrorMessage(errorMessage);
        ctx.addNodeExecutionDetail(detail);
    }
}

