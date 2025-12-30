package com.example.aikef.workflow.node;

import com.example.aikef.workflow.context.WorkflowContext;
import com.example.aikef.workflow.util.TemplateEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.core.NodeComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

/**
 * 工作流节点基类
 * 提供通用的上下文访问和日志记录功能
 */
public abstract class BaseWorkflowNode extends NodeComponent {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    static String resolveActualNodeId(String tag, String nodeId, WorkflowContext ctx) {
        if (tag != null && !tag.isEmpty()) {
            if (ctx != null) {
                JsonNode config = ctx.getNodeConfig(tag);
                if (config != null) {
                    return tag;
                }
            }
            return tag;
        }
        return nodeId;
    }

    static String readConfigString(JsonNode config, String key, String defaultValue) {
        if (config == null || !config.has(key) || config.get(key).isNull()) {
            return defaultValue;
        }
        return config.get(key).asText(defaultValue);
    }

    static int readConfigInt(JsonNode config, String key, int defaultValue) {
        if (config == null || !config.has(key) || config.get(key).isNull()) {
            return defaultValue;
        }
        return config.get(key).asInt(defaultValue);
    }

    static UUID parseUuidValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (Exception e) {
            return null;
        }
    }

    static void recordExecution(WorkflowContext ctx, String nodeId, String nodeType, String nodeName, Object input, Object output, long startTime, boolean success, String errorMessage) {
        WorkflowContext.NodeExecutionDetail detail = new WorkflowContext.NodeExecutionDetail();
        detail.setNodeId(nodeId);
        detail.setNodeType(nodeType);
        detail.setNodeName(nodeName);

        Map<String, String> nodeLabels = ctx.getNodeLabels();
        String nodeLabel = nodeLabels != null ? nodeLabels.get(nodeId) : null;
        detail.setNodeLabel(nodeLabel);

        detail.setInput(input);
        detail.setOutput(output);
        detail.setStartTime(startTime);
        detail.setEndTime(System.currentTimeMillis());
        detail.setDurationMs(detail.getEndTime() - startTime);
        detail.setSuccess(success);
        detail.setErrorMessage(errorMessage);
        ctx.addNodeExecutionDetail(detail);
    }

    /**
     * 获取工作流上下文
     */
    protected WorkflowContext getWorkflowContext() {
        return this.getContextBean(WorkflowContext.class);
    }
    
    /**
     * 获取实际的节点 ID（ReactFlow 节点 ID）
     * EL 表达式中使用 node("componentId").tag("instanceId") 格式
     * 通过 getTag() 获取 instanceId
     * 
     * 对于 SWITCH 节点的分支，如果 tag 不是有效的节点ID（不在 nodesConfig 中），
     * 则尝试从上下文获取实际节点ID（用于 tool 节点等返回状态值的情况）
     */
    protected String getActualNodeId() {
        // 使用 tag 获取 ReactFlow 节点 ID
        String tag = this.getTag();
        if (tag != null && !tag.isEmpty()) {
            // 检查 tag 是否是有效的节点ID（在 nodesConfig 中）
            WorkflowContext ctx = getWorkflowContext();
            if (ctx != null) {
                JsonNode config = ctx.getNodeConfig(tag);
                if (config != null) {
                    // tag 是有效的节点ID
                    return tag;
                }
                // tag 不是有效的节点ID，可能是状态值（如 "executed"）
                // 尝试从上下文获取实际节点ID
                // 注意：这需要转换器在生成 EL 时建立映射
            }
            // 如果上下文不可用，直接返回 tag
            return tag;
        }
        // 回退到 nodeId
        return this.getNodeId();
    }

    /**
     * 获取当前节点配置
     */
    protected JsonNode getNodeConfig() {
        return getWorkflowContext().getNodeConfig(getActualNodeId());
    }

    /**
     * 获取配置中的字符串值
     */
    protected String getConfigString(String key) {
        JsonNode config = getNodeConfig();
        if (config != null && config.has(key) && !config.get(key).isNull()) {
            return config.get(key).asText(null);
        }
        return null;
    }

    /**
     * 获取配置中的字符串值（带默认值）
     */
    protected String getConfigString(String key, String defaultValue) {
        String value = getConfigString(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取配置中的整数值
     */
    protected Integer getConfigInt(String key, Integer defaultValue) {
        JsonNode config = getNodeConfig();
        if (config != null && config.has(key) && !config.get(key).isNull()) {
            return config.get(key).asInt(defaultValue);
        }
        return defaultValue;
    }

    /**
     * 获取配置中的布尔值
     */
    protected Boolean getConfigBoolean(String key, Boolean defaultValue) {
        JsonNode config = getNodeConfig();
        if (config != null && config.has(key) && !config.get(key).isNull()) {
            return config.get(key).asBoolean(defaultValue);
        }
        return defaultValue;
    }

    protected Double getConfigDouble(String key, Double defaultValue) {
        JsonNode config = getNodeConfig();
        if (config == null || !config.has(key) || config.get(key).isNull()) {
            return defaultValue;
        }
        JsonNode value = config.get(key);
        if (value.isNumber()) {
            return value.doubleValue();
        }
        if (value.isTextual()) {
            try {
                return Double.parseDouble(value.asText());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return value.asDouble(defaultValue != null ? defaultValue : 0.0);
    }

    protected UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析模板字符串，替换其中的变量
     * 
     * 支持的变量格式:
     * - {{sys.query}} - 用户输入查询
     * - {{sys.lastOutput}} - 上一个节点的输出
     * - {{sys.intent}} - 识别的意图
     * - {{var.variableName}} - 自定义变量
     * - {{node.nodeId}} - 指定节点的输出
     * - {{customer.name}} - 客户信息
     * - {{entity.entityName}} - 提取的实体
     * 
     * @param template 模板字符串
     * @return 解析后的字符串
     */
    protected String renderTemplate(String template) {
        return TemplateEngine.render(template, getWorkflowContext());
    }

    /**
     * 获取输入（上一个节点的输出或用户查询）
     */
    protected String getInput() {
        WorkflowContext ctx = getWorkflowContext();
        String lastOutput = ctx.getLastOutput();
        if (lastOutput != null && !lastOutput.isEmpty()) {
            return lastOutput;
        }
        return ctx.getQuery();
    }

    /**
     * 设置节点输出
     */
    protected void setOutput(Object output) {
        getWorkflowContext().setOutput(getActualNodeId(), output);
    }

    /**
     * 记录节点执行详情
     */
    protected void recordExecution(Object input, Object output, long startTime, boolean success, String errorMessage) {
        WorkflowContext ctx = getWorkflowContext();
        WorkflowContext.NodeExecutionDetail detail = new WorkflowContext.NodeExecutionDetail();
        detail.setNodeId(getActualNodeId());
        detail.setNodeType(this.getNodeId()); // 组件类型（如 start, llm, intent）
        detail.setNodeName(this.getName());
        
        // 从上下文中获取节点标签（来自 data.label）
        String actualNodeId = getActualNodeId();
        Map<String, String> nodeLabels = ctx.getNodeLabels();
        String nodeLabel = nodeLabels != null ? nodeLabels.get(actualNodeId) : null;
        
        if (nodeLabel == null) {
            log.debug("未找到节点标签: nodeId={}, availableLabels={}", 
                    actualNodeId, nodeLabels != null ? nodeLabels.keySet() : "null");
        }
        
        detail.setNodeLabel(nodeLabel);
        
        detail.setInput(input);
        detail.setOutput(output);
        detail.setStartTime(startTime);
        detail.setEndTime(System.currentTimeMillis());
        detail.setDurationMs(detail.getEndTime() - startTime);
        detail.setSuccess(success);
        detail.setErrorMessage(errorMessage);
        ctx.addNodeExecutionDetail(detail);
    }
}
