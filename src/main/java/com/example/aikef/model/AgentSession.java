package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.*;
import java.util.UUID;

/**
 * Agent 会话
 * 记录特殊工作流的状态，直到走到 Agent End 节点
 */
@Entity
@Table(name = "agent_sessions", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "workflow_id"}))
public class AgentSession extends AuditableEntity {

    /**
     * 会话ID
     */
    @Column(name = "session_id", nullable = false, length = 36)
    private UUID sessionId;

    /**
     * 工作流ID（Agent 节点配置的工作流）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private AiWorkflow workflow;

    /**
     * 系统提示词（sysPrompt）
     * 由 Agent 节点创建时设置，可通过 AgentUpdate 节点更新
     */
    @Column(name = "sys_prompt", columnDefinition = "TEXT")
    private String sysPrompt;

    /**
     * 是否已结束（走到 Agent End 节点）
     */
    @Column(name = "is_ended", nullable = false)
    private boolean ended = false;

    /**
     * 结束时间
     */
    @Column(name = "ended_at")
    private java.time.Instant endedAt;

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public AiWorkflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(AiWorkflow workflow) {
        this.workflow = workflow;
    }

    public String getSysPrompt() {
        return sysPrompt;
    }

    public void setSysPrompt(String sysPrompt) {
        this.sysPrompt = sysPrompt;
    }

    public boolean isEnded() {
        return ended;
    }

    public void setEnded(boolean ended) {
        this.ended = ended;
    }

    public java.time.Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(java.time.Instant endedAt) {
        this.endedAt = endedAt;
    }
}

