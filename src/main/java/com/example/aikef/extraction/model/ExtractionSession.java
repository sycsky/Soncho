package com.example.aikef.extraction.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * 结构化提取会话
 * 跟踪多轮对话的提取进度
 */
@Data
@Entity
@Table(name = "extraction_sessions", indexes = {
        @Index(name = "idx_ext_session_schema", columnList = "schema_id"),
        @Index(name = "idx_ext_session_status", columnList = "status")
})
public class ExtractionSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 关联的提取模式
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schema_id", nullable = false)
    private ExtractionSchema schema;

    /**
     * 会话状态
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    /**
     * 已提取的数据 (JSON 格式)
     */
    @Column(name = "extracted_data", columnDefinition = "TEXT")
    private String extractedData;

    /**
     * 缺失的必填字段 (JSON 数组)
     */
    @Column(name = "missing_fields", columnDefinition = "TEXT")
    private String missingFields;

    /**
     * 对话历史 (JSON 数组)
     */
    @Column(name = "conversation_history", columnDefinition = "TEXT")
    private String conversationHistory;

    /**
     * 当前轮次
     */
    @Column(name = "current_round")
    private Integer currentRound = 0;

    /**
     * 最大轮次限制
     */
    @Column(name = "max_rounds")
    private Integer maxRounds = 5;

    /**
     * 关联的业务ID（可选）
     */
    @Column(name = "reference_id")
    private UUID referenceId;

    /**
     * 关联的业务类型
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    /**
     * 创建者ID
     */
    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum SessionStatus {
        IN_PROGRESS,    // 进行中
        COMPLETED,      // 已完成（所有必填字段已填充）
        CANCELLED,      // 已取消
        MAX_ROUNDS_REACHED  // 达到最大轮次
    }
}

