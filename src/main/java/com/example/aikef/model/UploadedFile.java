package com.example.aikef.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 上传文件实体
 */
@Data
@Entity
@Table(name = "uploaded_files", indexes = {
        @Index(name = "idx_uploaded_files_storage_path", columnList = "storage_path"),
        @Index(name = "idx_uploaded_files_uploader", columnList = "uploader_id, uploader_type"),
        @Index(name = "idx_uploaded_files_category", columnList = "category"),
        @Index(name = "idx_uploaded_files_created_at", columnList = "created_at")
})
public class UploadedFile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * 原始文件名
     */
    @Column(name = "original_name", nullable = false)
    private String originalName;
    
    /**
     * 存储路径（相对路径）
     */
    @Column(name = "storage_path", nullable = false, unique = true)
    private String storagePath;
    
    /**
     * 访问URL
     */
    @Column(name = "url", nullable = false, length = 1024)
    private String url;
    
    /**
     * 文件MIME类型
     */
    @Column(name = "content_type", nullable = false)
    private String contentType;
    
    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    /**
     * 文件扩展名
     */
    @Column(name = "extension", length = 20)
    private String extension;
    
    /**
     * 存储类型（LOCAL, S3）
     */
    @Column(name = "storage_type", nullable = false, length = 20)
    private String storageType;
    
    /**
     * 文件分类（IMAGE, DOCUMENT, VIDEO, AUDIO, OTHER）
     */
    @Column(name = "category", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FileCategory category;
    
    /**
     * 上传者ID
     */
    @Column(name = "uploader_id")
    private UUID uploaderId;
    
    /**
     * 上传者类型（AGENT, CUSTOMER, SYSTEM）
     */
    @Column(name = "uploader_type", length = 20)
    private String uploaderType;
    
    /**
     * 关联业务ID（可选，如消息ID、知识库文档ID等）
     */
    @Column(name = "reference_id")
    private UUID referenceId;
    
    /**
     * 关联业务类型
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;
    
    /**
     * 文件MD5哈希（用于去重）
     */
    @Column(name = "md5_hash", length = 32)
    private String md5Hash;
    
    /**
     * 是否公开访问
     */
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 文件分类枚举
     */
    public enum FileCategory {
        IMAGE,      // 图片
        DOCUMENT,   // 文档
        VIDEO,      // 视频
        AUDIO,      // 音频
        OTHER       // 其他
    }
}

