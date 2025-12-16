package com.example.aikef.workflow.context;

import com.example.aikef.workflow.tool.ToolCallState;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

/**
 * 工作流执行上下文
 * 在工作流执行过程中传递数据
 */
public class WorkflowContext {

    /**
     * 工作流ID
     */
    private UUID workflowId;

    /**
     * 会话ID
     */
    private UUID sessionId;

    /**
     * 触发工作流的消息ID
     */
    private UUID messageId;

    /**
     * 客户ID
     */
    private UUID customerId;

    /**
     * 用户输入的查询内容
     */
    private String query;

    /**
     * 初始输入文件
     */
    private List<String> files;

    /**
     * 用户意图（由意图识别节点设置）
     */
    private String intent;

    /**
     * 意图置信度
     */
    private Double intentConfidence;

    /**
     * 提取的实体信息
     */
    private Map<String, Object> entities = new HashMap<>();

    /**
     * 各节点的配置信息
     * key: nodeId, value: 节点配置JSON
     */
    private Map<String, JsonNode> nodesConfig = new HashMap<>();

    /**
     * 各节点的输出
     * key: nodeId, value: 节点输出
     */
    private Map<String, Object> nodeOutputs = new HashMap<>();

    /**
     * 节点执行详情（用于日志记录）
     */
    private List<NodeExecutionDetail> nodeExecutionDetails = new ArrayList<>();

    /**
     * 最终回复内容
     */
    private String finalReply;

    /**
     * 是否需要转人工
     */
    private boolean needHumanTransfer = false;

    /**
     * 转人工原因
     */
    private String humanTransferReason;

    /**
     * 自定义变量
     */
    private Map<String, Object> variables = new HashMap<>();

    /**
     * 对话历史（最近N条）
     */
    private List<ChatHistoryItem> chatHistory = new ArrayList<>();

    /**
     * 客户信息
     */
    private Map<String, Object> customerInfo = new HashMap<>();

    /**
     * 工具调用状态
     */
    private ToolCallState toolCallState;

    /**
     * AgentSession（特殊工作流会话）
     * 当 Agent 节点启动特殊工作流时，会注入此对象
     */
    private com.example.aikef.model.AgentSession agentSession;

    /**
     * 工具参数 Map
     * key: 工具名称（tool name）
     * value: 工具参数 Map（参数名 -> 参数值）
     * 由参数提取节点设置，供工具节点使用
     */
    private Map<String, Map<String, Object>> toolsParams = new HashMap<>();

    /**
     * 工作流是否暂停（等待用户输入）
     */
    private boolean paused = false;

    /**
     * 暂停原因
     */
    private String pauseReason;

    /**
     * 暂停时需要返回给用户的消息
     */
    private String pauseMessage;

    // ========== 辅助方法 ==========

    /**
     * 获取节点配置
     */
    public JsonNode getNodeConfig(String nodeId) {
        return nodesConfig.get(nodeId);
    }

    /**
     * 设置节点输出
     */
    public void setOutput(String nodeId, Object output) {
        nodeOutputs.put(nodeId, output);
    }

    /**
     * 设置节点输出（setOutput 的别名）
     */
    public void setNodeOutput(String nodeId, String output) {
        nodeOutputs.put(nodeId, output);
    }

    /**
     * 获取节点输出
     */
    @SuppressWarnings("unchecked")
    public <T> T getOutput(String nodeId) {
        return (T) nodeOutputs.get(nodeId);
    }

    /**
     * 获取上一个节点的输出（即最后执行的节点的输出）
     * 在串行工作流中，这就是当前节点的直接前置节点的输出
     * 如果没有前置节点输出，返回原始查询 query
     */
    public String getLastOutput() {
        if (nodeOutputs.isEmpty()) {
            return query;
        }
        // 按执行顺序获取最后的输出
        if (!nodeExecutionDetails.isEmpty()) {
            String lastNodeId = nodeExecutionDetails.get(nodeExecutionDetails.size() - 1).getNodeId();
            Object output = nodeOutputs.get(lastNodeId);
            return output != null ? output.toString() : query;
        }
        return nodeOutputs.values().stream()
                .reduce((first, second) -> second)
                .map(Object::toString)
                .orElse(query);
    }

    /**
     * 获取上一个节点的输出（getLastOutput 的别名）
     */
    public String getPreviousOutput() {
        return getLastOutput();
    }

    /**
     * 添加节点执行详情
     */
    public void addNodeExecutionDetail(NodeExecutionDetail detail) {
        nodeExecutionDetails.add(detail);
    }

    /**
     * 设置变量
     */
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    /**
     * 获取变量
     */
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key) {
        return (T) variables.get(key);
    }

    // ========== Getters and Setters ==========

    public UUID getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(UUID workflowId) {
        this.workflowId = workflowId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public Double getIntentConfidence() {
        return intentConfidence;
    }

    public void setIntentConfidence(Double intentConfidence) {
        this.intentConfidence = intentConfidence;
    }

    public Map<String, Object> getEntities() {
        return entities;
    }

    public void setEntities(Map<String, Object> entities) {
        this.entities = entities;
    }

    public Map<String, JsonNode> getNodesConfig() {
        return nodesConfig;
    }

    public void setNodesConfig(Map<String, JsonNode> nodesConfig) {
        this.nodesConfig = nodesConfig;
    }

    public Map<String, Object> getNodeOutputs() {
        return nodeOutputs;
    }

    public void setNodeOutputs(Map<String, Object> nodeOutputs) {
        this.nodeOutputs = nodeOutputs;
    }

    public List<NodeExecutionDetail> getNodeExecutionDetails() {
        return nodeExecutionDetails;
    }

    public void setNodeExecutionDetails(List<NodeExecutionDetail> nodeExecutionDetails) {
        this.nodeExecutionDetails = nodeExecutionDetails;
    }

    public String getFinalReply() {
        return finalReply;
    }

    public void setFinalReply(String finalReply) {
        this.finalReply = finalReply;
    }

    public boolean isNeedHumanTransfer() {
        return needHumanTransfer;
    }

    public void setNeedHumanTransfer(boolean needHumanTransfer) {
        this.needHumanTransfer = needHumanTransfer;
    }

    public String getHumanTransferReason() {
        return humanTransferReason;
    }

    public void setHumanTransferReason(String humanTransferReason) {
        this.humanTransferReason = humanTransferReason;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public List<ChatHistoryItem> getChatHistory() {
        return chatHistory;
    }

    public void setChatHistory(List<ChatHistoryItem> chatHistory) {
        this.chatHistory = chatHistory;
    }

    public Map<String, Object> getCustomerInfo() {
        return customerInfo;
    }

    public void setCustomerInfo(Map<String, Object> customerInfo) {
        this.customerInfo = customerInfo;
    }

    public ToolCallState getToolCallState() {
        return toolCallState;
    }

    public void setToolCallState(ToolCallState toolCallState) {
        this.toolCallState = toolCallState;
    }

    /**
     * 获取或创建工具调用状态
     */
    public ToolCallState getOrCreateToolCallState() {
        if (toolCallState == null) {
            toolCallState = new ToolCallState();
        }
        return toolCallState;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public String getPauseReason() {
        return pauseReason;
    }

    public void setPauseReason(String pauseReason) {
        this.pauseReason = pauseReason;
    }

    public String getPauseMessage() {
        return pauseMessage;
    }

    public void setPauseMessage(String pauseMessage) {
        this.pauseMessage = pauseMessage;
    }

    /**
     * 暂停工作流
     */
    public void pauseWorkflow(String reason, String message) {
        this.paused = true;
        this.pauseReason = reason;
        this.pauseMessage = message;
    }

    /**
     * 恢复工作流
     */
    public void resumeWorkflow() {
        this.paused = false;
        this.pauseReason = null;
        this.pauseMessage = null;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    public com.example.aikef.model.AgentSession getAgentSession() {
        return agentSession;
    }

    public void setAgentSession(com.example.aikef.model.AgentSession agentSession) {
        this.agentSession = agentSession;
    }

    public Map<String, Map<String, Object>> getToolsParams() {
        return toolsParams;
    }

    public void setToolsParams(Map<String, Map<String, Object>> toolsParams) {
        this.toolsParams = toolsParams;
    }

    /**
     * 设置工具参数
     */
    public void setToolParams(String toolName, Map<String, Object> params) {
        toolsParams.put(toolName, params);
    }

    /**
     * 获取工具参数
     */
    public Map<String, Object> getToolParams(String toolName) {
        return toolsParams.get(toolName);
    }

    // ========== 内部类 ==========

    /**
     * 节点执行详情
     */
    public static class NodeExecutionDetail {
        private String nodeId;
        private String nodeType;
        private String nodeName;
        private Object input;
        private Object output;
        private long startTime;
        private long endTime;
        private long durationMs;
        private boolean success;
        private String errorMessage;

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getNodeType() {
            return nodeType;
        }

        public void setNodeType(String nodeType) {
            this.nodeType = nodeType;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        public Object getInput() {
            return input;
        }

        public void setInput(Object input) {
            this.input = input;
        }

        public Object getOutput() {
            return output;
        }

        public void setOutput(Object output) {
            this.output = output;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    /**
     * 聊天历史条目
     */
    public static class ChatHistoryItem {
        private String role; // user / assistant
        private String content;
        private long timestamp;

        public ChatHistoryItem() {}

        public ChatHistoryItem(String role, String content, long timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}

