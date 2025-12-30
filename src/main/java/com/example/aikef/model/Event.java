package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.*;

/**
 * 事件配置实体
 * 用于配置外部事件hook，可以绑定到特定的工作流
 */
@Entity
@Table(name = "events")
public class Event extends AuditableEntity {

    /**
     * 事件名称（唯一标识，用于hook接收）
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * 事件显示名称
     */
    @Column(name = "display_name")
    private String displayName;

    /**
     * 事件描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 绑定的工作流ID
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "workflow_id", nullable = false)
    private AiWorkflow workflow;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    private boolean enabled = true;

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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AiWorkflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(AiWorkflow workflow) {
        this.workflow = workflow;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}




