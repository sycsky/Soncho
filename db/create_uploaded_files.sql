-- 上传文件表
CREATE TABLE IF NOT EXISTS uploaded_files (
    id CHAR(36) PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    storage_path VARCHAR(512) NOT NULL UNIQUE COMMENT '存储路径',
    url VARCHAR(1024) NOT NULL COMMENT '访问URL',
    content_type VARCHAR(100) NOT NULL COMMENT 'MIME类型',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    extension VARCHAR(20) COMMENT '文件扩展名',
    storage_type VARCHAR(20) NOT NULL COMMENT '存储类型(LOCAL, S3)',
    category VARCHAR(20) NOT NULL COMMENT '文件分类(IMAGE, DOCUMENT, VIDEO, AUDIO, OTHER)',
    uploader_id CHAR(36) COMMENT '上传者ID',
    uploader_type VARCHAR(20) COMMENT '上传者类型(AGENT, CUSTOMER, SYSTEM)',
    reference_id CHAR(36) COMMENT '关联业务ID',
    reference_type VARCHAR(50) COMMENT '关联业务类型',
    md5_hash VARCHAR(32) COMMENT 'MD5哈希',
    is_public BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否公开访问',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_uploaded_files_storage_path (storage_path),
    INDEX idx_uploaded_files_uploader (uploader_id, uploader_type),
    INDEX idx_uploaded_files_category (category),
    INDEX idx_uploaded_files_created_at (created_at),
    INDEX idx_uploaded_files_reference (reference_id, reference_type),
    INDEX idx_uploaded_files_md5 (md5_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上传文件表';

