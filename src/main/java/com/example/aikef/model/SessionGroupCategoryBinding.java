package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.*;

/**
 * 分组与分类绑定实体
 * 记录客服的分组绑定了哪些分类
 * 约束：同一个Agent下，不同分组不能重复绑定同一分类
 */
@Entity
@Table(
    name = "session_group_category_bindings",
    uniqueConstraints = {
        // 同一个Agent下，一个分类只能绑定到一个分组
        @UniqueConstraint(
            name = "uk_agent_category",
            columnNames = {"agent_id", "category_id"}
        )
    }
)
public class SessionGroupCategoryBinding extends AuditableEntity {

    /**
     * 绑定的分组
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_group_id", nullable = false)
    private SessionGroup sessionGroup;

    /**
     * 绑定的分类
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private SessionCategory category;

    /**
     * 所属客服（冗余字段，用于唯一约束校验）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    // Getters and Setters

    public SessionGroup getSessionGroup() {
        return sessionGroup;
    }

    public void setSessionGroup(SessionGroup sessionGroup) {
        this.sessionGroup = sessionGroup;
    }

    public SessionCategory getCategory() {
        return category;
    }

    public void setCategory(SessionCategory category) {
        this.category = category;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }
}

