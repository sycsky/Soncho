package com.example.aikef.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * 知识库实体
 * 每个知识库包含多个文档，用于向量检索
 */
@Data
@Entity
@Table(name = "knowledge_bases")
public class KnowledgeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * Redis 向量索引名称（自动生成）
     */
    @Column(name = "index_name", nullable = false, unique = true)
    private String indexName;

    /**
     * 嵌入模型 ID（关联 llm_models 表）
     */
    @Column(name = "embedding_model_id")
    private UUID embeddingModelId;

    /**
     * 向量维度
     */
    @Column(name = "vector_dimension")
    private Integer vectorDimension = 1536;

    /**
     * 文档数量（缓存字段）
     */
    @Column(name = "document_count")
    private Integer documentCount = 0;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    private Boolean enabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_agent_id")
    private Agent createdByAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (indexName == null) {
            indexName = "kb_" + UUID.randomUUID().toString().replace("-", "");
        }
    }
}


