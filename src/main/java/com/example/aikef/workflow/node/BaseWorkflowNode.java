package com.example.aikef.workflow.node;

import com.example.aikef.workflow.context.WorkflowContext;
import com.example.aikef.workflow.util.TemplateEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.core.NodeComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 工作流节点基类
 * 提供通用的上下文访问和日志记录功能
 */
public abstract class BaseWorkflowNode extends NodeComponent {

    protected final Logger log = LoggerFactory.getLogger(getClass());

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
     */
    protected String getActualNodeId() {
        // 使用 tag 获取 ReactFlow 节点 ID
        String tag = this.getTag();
        if (tag != null && !tag.isEmpty()) {
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
        if (config != null && config.has(key)) {
            return config.get(key).asText();
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
        if (config != null && config.has(key)) {
            return config.get(key).asInt(defaultValue);
        }
        return defaultValue;
    }

    /**
     * 获取配置中的布尔值
     */
    protected Boolean getConfigBoolean(String key, Boolean defaultValue) {
        JsonNode config = getNodeConfig();
        if (config != null && config.has(key)) {
            return config.get(key).asBoolean(defaultValue);
        }
        return defaultValue;
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

