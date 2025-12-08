package com.example.aikef.extraction.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * 结构化提取模式定义
 * 定义要从文本中提取的数据结构
 */
@Data
@Entity
@Table(name = "extraction_schemas")
public class ExtractionSchema {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 模式名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 模式描述
     */
    @Column(length = 500)
    private String description;

    /**
     * 字段定义 (JSON 格式)
     * 格式: [{"name": "field1", "type": "string", "required": true, "description": "字段描述", ...}]
     */
    @Column(name = "fields_json", columnDefinition = "TEXT", nullable = false)
    private String fieldsJson;

    /**
     * 提取提示词模板
     */
    @Column(name = "extraction_prompt", columnDefinition = "TEXT")
    private String extractionPrompt;

    /**
     * 追问提示词模板（当缺少必填字段时使用）
     */
    @Column(name = "followup_prompt", columnDefinition = "TEXT")
    private String followupPrompt;

    /**
     * 使用的LLM模型ID
     */
    @Column(name = "llm_model_id")
    private UUID llmModelId;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    private Boolean enabled = true;

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
}

