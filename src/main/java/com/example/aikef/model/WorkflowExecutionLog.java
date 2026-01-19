package com.example.aikef.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 工作流执行日志
 * 记录每次工作流执行的详细信息
 */
@Entity
@Table(name = "workflow_execution_logs")
public class WorkflowExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 关联的工作流
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private AiWorkflow workflow;

    /**
     * 关联的会话
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ChatSession session;

    @Column(name = "message_id")
    private UUID messageId;

    /**
     * 执行状态: SUCCESS, FAILED, TIMEOUT
     */
    @Column(nullable = false)
    private String status;

    /**
     * 用户输入消息
     */
    @Column(name = "user_input", columnDefinition = "TEXT")
    private String userInput;

    /**
     * 最终输出/回复
     */
    @Column(name = "final_output", columnDefinition = "TEXT")
    private String finalOutput;

    /**
     * 节点执行详情 (JSON)
     * 记录每个节点的输入、输出、耗时等
     */
    @Column(name = "node_details", columnDefinition = "LONGTEXT")
    private String nodeDetails;

    /**
     * 工具执行链 (JSON)
     * 记录 Agent 和 LLM 节点的工具调用详情
     */
    @Column(name = "tool_execution_chain", columnDefinition = "LONGTEXT")
    private String toolExecutionChain;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * 执行开始时间
     */
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    /**
     * 执行结束时间
     */
    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (startedAt == null) {
            startedAt = Instant.now();
        }
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AiWorkflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(AiWorkflow workflow) {
        this.workflow = workflow;
    }

    public ChatSession getSession() {
        return session;
    }

    public void setSession(ChatSession session) {
        this.session = session;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    public String getFinalOutput() {
        return finalOutput;
    }

    public void setFinalOutput(String finalOutput) {
        this.finalOutput = finalOutput;
    }

    public String getNodeDetails() {
        return nodeDetails;
    }

    public void setNodeDetails(String nodeDetails) {
        this.nodeDetails = nodeDetails;
    }

    public String getToolExecutionChain() {
        return toolExecutionChain;
    }

    public void setToolExecutionChain(String toolExecutionChain) {
        this.toolExecutionChain = toolExecutionChain;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

