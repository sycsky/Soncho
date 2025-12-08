package com.example.aikef.workflow.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * 工作流暂停状态
 * 用于保存因工具调用等待用户输入而暂停的工作流状态
 */
@Entity
@Table(name = "workflow_paused_states")
public class WorkflowPausedState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 会话ID - 用于关联用户会话
     */
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    /**
     * 工作流ID
     */
    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    /**
     * 当前暂停的子链ID
     */
    @Column(name = "sub_chain_id", nullable = false)
    private String subChainId;

    /**
     * LLM 节点ID
     */
    @Column(name = "llm_node_id", nullable = false)
    private String llmNodeId;

    /**
     * 暂停原因
     */
    @Column(name = "pause_reason")
    private String pauseReason;

    /**
     * 上下文数据 (JSON)
     * 保存 WorkflowContext 的序列化数据
     */
    @Column(name = "context_json", columnDefinition = "LONGTEXT")
    private String contextJson;

    /**
     * 工具调用状态 (JSON)
     * 保存 ToolCallState 的序列化数据
     */
    @Column(name = "tool_call_state_json", columnDefinition = "TEXT")
    private String toolCallStateJson;

    /**
     * 已收集的参数 (JSON)
     */
    @Column(name = "collected_params_json", columnDefinition = "TEXT")
    private String collectedParamsJson;

    /**
     * 当前轮次
     */
    @Column(name = "current_round")
    private Integer currentRound = 0;

    /**
     * 最大轮次
     */
    @Column(name = "max_rounds")
    private Integer maxRounds = 5;

    /**
     * 待执行的工具ID
     */
    @Column(name = "pending_tool_id")
    private UUID pendingToolId;

    /**
     * 待执行的工具名称
     */
    @Column(name = "pending_tool_name")
    private String pendingToolName;

    /**
     * 下一个追问问题
     */
    @Column(name = "next_question", columnDefinition = "TEXT")
    private String nextQuestion;

    /**
     * LLM 对话历史 (JSON)
     * 保存暂停时的 ChatMessage 列表，用于恢复时继续对话
     */
    @Column(name = "chat_history_json", columnDefinition = "LONGTEXT")
    private String chatHistoryJson;

    /**
     * 状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.WAITING_USER_INPUT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    public enum Status {
        WAITING_USER_INPUT,  // 等待用户输入
        RESUMED,             // 已恢复执行
        COMPLETED,           // 已完成
        EXPIRED,             // 已过期
        CANCELLED            // 已取消
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        // 默认 30 分钟过期
        if (expiredAt == null) {
            expiredAt = Instant.now().plusSeconds(30 * 60);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(UUID workflowId) {
        this.workflowId = workflowId;
    }

    public String getSubChainId() {
        return subChainId;
    }

    public void setSubChainId(String subChainId) {
        this.subChainId = subChainId;
    }

    public String getLlmNodeId() {
        return llmNodeId;
    }

    public void setLlmNodeId(String llmNodeId) {
        this.llmNodeId = llmNodeId;
    }

    public String getPauseReason() {
        return pauseReason;
    }

    public void setPauseReason(String pauseReason) {
        this.pauseReason = pauseReason;
    }

    public String getContextJson() {
        return contextJson;
    }

    public void setContextJson(String contextJson) {
        this.contextJson = contextJson;
    }

    public String getToolCallStateJson() {
        return toolCallStateJson;
    }

    public void setToolCallStateJson(String toolCallStateJson) {
        this.toolCallStateJson = toolCallStateJson;
    }

    public String getCollectedParamsJson() {
        return collectedParamsJson;
    }

    public void setCollectedParamsJson(String collectedParamsJson) {
        this.collectedParamsJson = collectedParamsJson;
    }

    public Integer getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(Integer currentRound) {
        this.currentRound = currentRound;
    }

    public Integer getMaxRounds() {
        return maxRounds;
    }

    public void setMaxRounds(Integer maxRounds) {
        this.maxRounds = maxRounds;
    }

    public UUID getPendingToolId() {
        return pendingToolId;
    }

    public void setPendingToolId(UUID pendingToolId) {
        this.pendingToolId = pendingToolId;
    }

    public String getPendingToolName() {
        return pendingToolName;
    }

    public void setPendingToolName(String pendingToolName) {
        this.pendingToolName = pendingToolName;
    }

    public String getNextQuestion() {
        return nextQuestion;
    }

    public void setNextQuestion(String nextQuestion) {
        this.nextQuestion = nextQuestion;
    }

    public String getChatHistoryJson() {
        return chatHistoryJson;
    }

    public void setChatHistoryJson(String chatHistoryJson) {
        this.chatHistoryJson = chatHistoryJson;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Instant expiredAt) {
        this.expiredAt = expiredAt;
    }

    /**
     * 检查是否已过期
     */
    public boolean isExpired() {
        return expiredAt != null && Instant.now().isAfter(expiredAt);
    }
}

