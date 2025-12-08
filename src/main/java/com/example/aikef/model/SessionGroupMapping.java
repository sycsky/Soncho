package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.*;

/**
 * Session分组映射实体
 * 记录每个客服将session分配到哪个分组
 * 一个session可以被不同客服分到不同的分组
 */
@Entity
@Table(name = "session_group_mappings")
public class SessionGroupMapping extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_group_id", nullable = false)
    private SessionGroup sessionGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    public ChatSession getSession() {
        return session;
    }

    public void setSession(ChatSession session) {
        this.session = session;
    }

    public SessionGroup getSessionGroup() {
        return sessionGroup;
    }

    public void setSessionGroup(SessionGroup sessionGroup) {
        this.sessionGroup = sessionGroup;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }
}
