package com.example.aikef.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * 工作流与会话分类的绑定关系
 * 一个工作流可以绑定多个分类
 * 一个分类只能绑定一个工作流（category_id 唯一）
 */
@Entity
@Table(name = "workflow_category_bindings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wcb_category", columnNames = {"category_id"})  // 一个分类只能绑定一个工作流
        },
        indexes = {
                @Index(name = "idx_wcb_workflow", columnList = "workflow_id"),
                @Index(name = "idx_wcb_category", columnList = "category_id")
        })
public class WorkflowCategoryBinding {

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
     * 关联的会话分类
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private SessionCategory category;

    /**
     * 优先级（数字越小优先级越高）
     */
    @Column(name = "priority")
    private Integer priority = 0;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
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

    public SessionCategory getCategory() {
        return category;
    }

    public void setCategory(SessionCategory category) {
        this.category = category;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

