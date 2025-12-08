package com.example.aikef.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * 知识库文档实体
 * 存储文档的原始内容和元数据
 */
@Data
@Entity
@Table(name = "knowledge_documents", indexes = {
    @Index(name = "idx_kb_doc_knowledge_base", columnList = "knowledge_base_id"),
    @Index(name = "idx_kb_doc_status", columnList = "status")
})
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_base_id", nullable = false)
    private KnowledgeBase knowledgeBase;

    /**
     * 文档标题
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 原始内容
     */
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    /**
     * 文档类型: TEXT, MARKDOWN, HTML, PDF, URL
     */
    @Column(name = "doc_type", length = 20)
    @Enumerated(EnumType.STRING)
    private DocumentType docType = DocumentType.TEXT;

    /**
     * 文档来源 URL（如果有）
     */
    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    /**
     * 分块大小（字符数）
     */
    @Column(name = "chunk_size")
    private Integer chunkSize = 500;

    /**
     * 分块重叠（字符数）
     */
    @Column(name = "chunk_overlap")
    private Integer chunkOverlap = 50;

    /**
     * 分块数量（向量化后）
     */
    @Column(name = "chunk_count")
    private Integer chunkCount = 0;

    /**
     * 处理状态
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ProcessStatus status = ProcessStatus.PENDING;

    /**
     * 错误信息（如果处理失败）
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * 自定义元数据（JSON 格式）
     */
    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum DocumentType {
        TEXT, MARKDOWN, HTML, PDF, URL
    }

    public enum ProcessStatus {
        PENDING,      // 待处理
        PROCESSING,   // 处理中
        COMPLETED,    // 完成
        FAILED        // 失败
    }
}


