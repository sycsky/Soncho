package com.example.aikef.tool.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * 工具执行记录
 */
@Data
@Entity
@Table(name = "tool_executions", indexes = {
        @Index(name = "idx_tool_exec_tool", columnList = "tool_id"),
        @Index(name = "idx_tool_exec_status", columnList = "status"),
        @Index(name = "idx_tool_exec_session", columnList = "session_id"),
        @Index(name = "idx_tool_exec_created", columnList = "created_at")
})
public class ToolExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 关联的工具
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tool_id", nullable = false)
    private AiTool tool;

    /**
     * 关联的会话ID（可选）
     */
    @Column(name = "session_id")
    private UUID sessionId;

    /**
     * 执行状态
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status = ExecutionStatus.PENDING;

    /**
     * 输入参数（JSON 格式）
     */
    @Column(name = "input_params", columnDefinition = "TEXT")
    private String inputParams;

    /**
     * 输出结果（JSON 格式）
     */
    @Column(name = "output_result", columnDefinition = "TEXT")
    private String outputResult;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * HTTP 状态码（仅 API 类型）
     */
    @Column(name = "http_status")
    private Integer httpStatus;

    /**
     * 重试次数
     */
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    /**
     * 触发来源
     */
    @Column(name = "trigger_source", length = 50)
    private String triggerSource;

    /**
     * 执行者ID
     */
    @Column(name = "executed_by")
    private UUID executedBy;

    /**
     * 开始时间
     */
    @Column(name = "started_at")
    private Instant startedAt;

    /**
     * 完成时间
     */
    @Column(name = "finished_at")
    private Instant finishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * 执行状态
     */
    public enum ExecutionStatus {
        PENDING,        // 待执行
        RUNNING,        // 执行中
        SUCCESS,        // 成功
        FAILED,         // 失败
        TIMEOUT,        // 超时
        CANCELLED       // 已取消
    }
}

