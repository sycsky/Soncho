package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.*;

/**
 * Session 分组实体
 * 客服可以将会话分配到不同的组进行管理
 */
@Entity
@Table(name = "session_groups")
public class SessionGroup extends AuditableEntity {

    /**
     * 分组名称
     */
    @Column(nullable = false)
    private String name;

    /**
     * 是否为系统分组（系统分组不能被删除）
     */
    @Column(name = "is_system", nullable = false)
    private boolean system = false;

    /**
     * 分组所属客服
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    /**
     * 分组图标
     */
    @Column(name = "icon")
    private String icon;

    /**
     * 分组颜色
     */
    @Column(name = "color")
    private String color;

    /**
     * 排序顺序
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
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
}
