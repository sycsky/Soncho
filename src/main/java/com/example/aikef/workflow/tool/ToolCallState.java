package com.example.aikef.workflow.tool;

import lombok.Data;

import java.util.*;

/**
 * 工具调用状态
 * 用于跟踪工作流中的工具调用过程
 */
@Data
public class ToolCallState {

    /**
     * 当前状态
     */
    private Status status = Status.IDLE;

    private UUID toolId;

    /**
     * 待执行的工具请求列表
     */
    private List<ToolCallRequest> pendingToolCalls = new ArrayList<>();

    /**
     * 当前正在处理的工具请求
     */
    private ToolCallRequest currentToolCall;

    /**
     * 已完成的工具执行结果
     */
    private List<ToolCallResult> completedResults = new ArrayList<>();

    /**
     * 参数收集会话ID（如果正在进行多轮对话）
     */
    private UUID extractionSessionId;

    /**
     * 已收集的参数
     */
    private Map<String, Object> collectedParams = new HashMap<>();

    /**
     * 缺失的参数
     */
    private List<String> missingParams = new ArrayList<>();

    /**
     * 下一个追问问题
     */
    private String nextQuestion;

    /**
     * 当前轮次
     */
    private int currentRound = 0;

    /**
     * 最大轮次
     */
    private int maxRounds = 5;

    /**
     * 暂停节点ID（工作流暂停时记录）
     */
    private String pausedNodeId;

    /**
     * LLM 原始消息（包含工具调用请求）
     */
    private String llmMessageWithToolCall;

    public enum Status {
        IDLE,                    // 空闲状态
        TOOL_CALL_DETECTED,      // 检测到工具调用
        EXTRACTING_PARAMS,       // 正在提取/收集参数
        WAITING_USER_INPUT,      // 等待用户输入（工作流暂停）
        EXECUTING_TOOL,          // 正在执行工具
        TOOL_COMPLETED,          // 工具执行完成
        TOOL_FAILED,             // 工具执行失败
        SKIPPED                  // 跳过工具执行
    }

    // ==================== 辅助方法 ====================

    /**
     * 重置状态
     */
    public void reset() {
        this.status = Status.IDLE;
        this.pendingToolCalls.clear();
        this.currentToolCall = null;
        this.completedResults.clear();
        this.extractionSessionId = null;
        this.collectedParams.clear();
        this.missingParams.clear();
        this.nextQuestion = null;
        this.currentRound = 0;
        this.pausedNodeId = null;
        this.llmMessageWithToolCall = null;
    }

    /**
     * 是否需要暂停工作流
     */
    public boolean shouldPauseWorkflow() {
        return status == Status.WAITING_USER_INPUT;
    }

    /**
     * 是否所有参数都已收集
     */
    public boolean hasAllParams() {
        return missingParams.isEmpty();
    }

    /**
     * 添加工具调用请求
     */
    public void addToolCall(ToolCallRequest request) {
        pendingToolCalls.add(request);
    }

    /**
     * 添加执行结果
     */
    public void addResult(ToolCallResult result) {
        completedResults.add(result);
    }

    // ==================== Getters and Setters ====================

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<ToolCallRequest> getPendingToolCalls() {
        return pendingToolCalls;
    }

    public void setPendingToolCalls(List<ToolCallRequest> pendingToolCalls) {
        this.pendingToolCalls = pendingToolCalls;
    }

    public ToolCallRequest getCurrentToolCall() {
        return currentToolCall;
    }

    public void setCurrentToolCall(ToolCallRequest currentToolCall) {
        this.currentToolCall = currentToolCall;
    }

    public List<ToolCallResult> getCompletedResults() {
        return completedResults;
    }

    public void setCompletedResults(List<ToolCallResult> completedResults) {
        this.completedResults = completedResults;
    }

    public UUID getExtractionSessionId() {
        return extractionSessionId;
    }

    public void setExtractionSessionId(UUID extractionSessionId) {
        this.extractionSessionId = extractionSessionId;
    }

    public Map<String, Object> getCollectedParams() {
        return collectedParams;
    }

    public void setCollectedParams(Map<String, Object> collectedParams) {
        this.collectedParams = collectedParams;
    }

    public List<String> getMissingParams() {
        return missingParams;
    }

    public void setMissingParams(List<String> missingParams) {
        this.missingParams = missingParams;
    }

    public String getNextQuestion() {
        return nextQuestion;
    }

    public void setNextQuestion(String nextQuestion) {
        this.nextQuestion = nextQuestion;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public int getMaxRounds() {
        return maxRounds;
    }

    public void setMaxRounds(int maxRounds) {
        this.maxRounds = maxRounds;
    }

    public String getPausedNodeId() {
        return pausedNodeId;
    }

    public void setPausedNodeId(String pausedNodeId) {
        this.pausedNodeId = pausedNodeId;
    }

    public String getLlmMessageWithToolCall() {
        return llmMessageWithToolCall;
    }

    public void setLlmMessageWithToolCall(String llmMessageWithToolCall) {
        this.llmMessageWithToolCall = llmMessageWithToolCall;
    }

    // ==================== 内部数据类 ====================

    /**
     * 工具调用请求
     */
    public static class ToolCallRequest {
        private String id;           // 调用ID
        private String toolName;     // 工具名称
        private UUID toolId;         // 工具ID
        private Map<String, Object> arguments;  // 参数

        public ToolCallRequest() {}

        public ToolCallRequest(String id, String toolName, UUID toolId, Map<String, Object> arguments) {
            this.id = id;
            this.toolName = toolName;
            this.toolId = toolId;
            this.arguments = arguments != null ? arguments : new HashMap<>();
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getToolName() {
            return toolName;
        }

        public void setToolName(String toolName) {
            this.toolName = toolName;
        }

        public UUID getToolId() {
            return toolId;
        }

        public void setToolId(UUID toolId) {
            this.toolId = toolId;
        }

        public Map<String, Object> getArguments() {
            return arguments;
        }

        public void setArguments(Map<String, Object> arguments) {
            this.arguments = arguments;
        }
    }

    /**
     * 工具调用结果
     */
    public static class ToolCallResult {
        private String toolCallId;   // 对应的调用ID
        private String toolName;     // 工具名称
        private boolean success;     // 是否成功
        private String result;       // 执行结果
        private String errorMessage; // 错误信息
        private long durationMs;     // 执行耗时

        public ToolCallResult() {}

        public ToolCallResult(String toolCallId, String toolName, boolean success, String result, String errorMessage, long durationMs) {
            this.toolCallId = toolCallId;
            this.toolName = toolName;
            this.success = success;
            this.result = result;
            this.errorMessage = errorMessage;
            this.durationMs = durationMs;
        }

        public String getToolCallId() {
            return toolCallId;
        }

        public void setToolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
        }

        public String getToolName() {
            return toolName;
        }

        public void setToolName(String toolName) {
            this.toolName = toolName;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }
    }
}

