package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * 已读记录实体
 * 记录每个客服对每个会话的最后阅读时间
 */
@Entity
@Table(name = "read_records", uniqueConstraints = {
    @UniqueConstraint(name = "uk_session_agent", columnNames = {"session_id", "agent_id"})
})
public class ReadRecord extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "last_read_time", nullable = false)
    private Instant lastReadTime;

    public ChatSession getSession() {
        return session;
    }

    public void setSession(ChatSession session) {
        this.session = session;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public Instant getLastReadTime() {
        return lastReadTime;
    }

    public void setLastReadTime(Instant lastReadTime) {
        this.lastReadTime = lastReadTime;
    }
}
