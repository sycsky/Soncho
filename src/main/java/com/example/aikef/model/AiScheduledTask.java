package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import com.example.aikef.model.enums.TaskCustomerMode;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.UUID;

/**
 * AI 定时任务实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_scheduled_tasks")
public class AiScheduledTask extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean enabled = true;

    /**
     * Cron 表达式
     */
    @Column(nullable = false)
    private String cronExpression;

    /**
     * 前端调度配置 JSON (存储 type, days, time 等)
     */
    @Column(columnDefinition = "TEXT")
    private String scheduleConfig;

    /**
     * 关联的工作流
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private AiWorkflow workflow;

    /**
     * 客户目标模式
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskCustomerMode customerMode;

    /**
     * 目标配置 JSON (存储客户ID列表或角色ID列表)
     */
    @Column(columnDefinition = "TEXT")
    private String targetConfig;

    /**
     * 初始启动输入
     */
    @Column(columnDefinition = "TEXT")
    private String initialInput;

    /**
     * 上次运行时间
     */
    private Instant lastRunAt;

    /**
     * 下次运行时间
     */
    private Instant nextRunAt;
}
