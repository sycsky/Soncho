package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.*;

/**
 * 会话分类实体
 * 用于对会话进行分类管理
 */
@Entity
@Table(name = "session_categories")
public class SessionCategory extends AuditableEntity {

    /**
     * 分类名称
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * 创建人（通常是admin用户）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_agent_id", nullable = false)
    private Agent createdByAgent;

    /**
     * 分类描述
     */
    @Column(name = "description")
    private String description;

    /**
     * 分类图标
     */
    @Column(name = "icon")
    private String icon;

    /**
     * 分类颜色
     */
    @Column(name = "color")
    private String color;

    /**
     * 排序顺序
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Agent getCreatedByAgent() {
        return createdByAgent;
    }

    public void setCreatedByAgent(Agent createdByAgent) {
        this.createdByAgent = createdByAgent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

