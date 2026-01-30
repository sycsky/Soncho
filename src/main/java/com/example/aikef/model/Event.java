package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.*;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import jakarta.persistence.EntityListeners;
import com.example.aikef.saas.listener.TenantEntityListener;

/**
 * 事件配置实体
 * 用于配置外部事件hook，可以绑定到特定的工作流
 */
@Entity
@Table(name = "events")
public class Event extends AuditableEntity {

    /**
     * 事件名称（唯一标识，用于hook接收）
     * 移除全局唯一约束，改为在租户内唯一
     */
    @Column(nullable = false)
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
    @Column(name = "workflow_name", length = 255)
    private String workflowName;

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

    /**
     * 是否为模板事件
     */
    @Column(name = "is_template", nullable = false)
    private boolean isTemplate = false;

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

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
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

    public boolean isTemplate() {
        return isTemplate;
    }

    public void setTemplate(boolean template) {
        isTemplate = template;
    }
}





