-- 删除旧的 knowledge_entries 表（如果存在且不需要）
-- DROP TABLE IF EXISTS knowledge_entries;

-- 知识库表
CREATE TABLE IF NOT EXISTS knowledge_bases (
    id BINARY(16) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    index_name VARCHAR(100) NOT NULL UNIQUE COMMENT 'Redis 向量索引名称',
    embedding_model_id BINARY(16) COMMENT '嵌入模型 ID',
    vector_dimension INT DEFAULT 1536 COMMENT '向量维度',
    document_count INT DEFAULT 0 COMMENT '文档数量',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_by_agent_id BINARY(16),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_kb_enabled (enabled),
    INDEX idx_kb_name (name),
    CONSTRAINT fk_kb_created_by FOREIGN KEY (created_by_agent_id) REFERENCES agents(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 知识库文档表
CREATE TABLE IF NOT EXISTS knowledge_documents (
    id BINARY(16) PRIMARY KEY,
    knowledge_base_id BINARY(16) NOT NULL COMMENT '所属知识库',
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    content LONGTEXT NOT NULL COMMENT '文档内容',
    doc_type VARCHAR(20) DEFAULT 'TEXT' COMMENT '文档类型: TEXT, MARKDOWN, HTML, PDF, URL',
    source_url VARCHAR(500) COMMENT '来源 URL',
    chunk_size INT DEFAULT 500 COMMENT '分块大小',
    chunk_overlap INT DEFAULT 50 COMMENT '分块重叠',
    chunk_count INT DEFAULT 0 COMMENT '分块数量',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态: PENDING, PROCESSING, COMPLETED, FAILED',
    error_message VARCHAR(1000) COMMENT '错误信息',
    metadata_json TEXT COMMENT '自定义元数据 JSON',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_kb_doc_knowledge_base (knowledge_base_id),
    INDEX idx_kb_doc_status (status),
    INDEX idx_kb_doc_title (title),
    CONSTRAINT fk_doc_knowledge_base FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 为 llm_models 表添加嵌入模型支持
-- 添加 model_type 字段区分 LLM 和 Embedding 模型
ALTER TABLE llm_models 
ADD COLUMN model_type VARCHAR(20) DEFAULT 'CHAT' COMMENT '模型类型: CHAT, EMBEDDING' AFTER provider;

-- 更新现有记录为 CHAT 类型
UPDATE llm_models SET model_type = 'CHAT' WHERE model_type IS NULL;

-- 添加索引
ALTER TABLE llm_models ADD INDEX idx_llm_model_type (model_type);

-- 插入默认的嵌入模型配置
INSERT INTO llm_models (id, name, code, provider, model_type, model_name, base_url, api_key, 
    default_temperature, default_max_tokens, context_window, enabled, is_default, sort_order, description)
VALUES 
    (UUID_TO_BIN(UUID()), 'OpenAI text-embedding-3-small', 'openai-embedding-small', 'OPENAI', 'EMBEDDING',
     'text-embedding-3-small', 'https://api.openai.com/v1', '', 
     NULL, NULL, NULL, 0, 0, 100, 'OpenAI 小型嵌入模型，1536 维度，性价比高'),
    (UUID_TO_BIN(UUID()), 'OpenAI text-embedding-3-large', 'openai-embedding-large', 'OPENAI', 'EMBEDDING',
     'text-embedding-3-large', 'https://api.openai.com/v1', '', 
     NULL, NULL, NULL, 0, 0, 101, 'OpenAI 大型嵌入模型，3072 维度，效果最佳'),
    (UUID_TO_BIN(UUID()), 'Ollama nomic-embed-text', 'ollama-nomic-embed', 'OLLAMA', 'EMBEDDING',
     'nomic-embed-text', 'http://localhost:11434', '', 
     NULL, NULL, NULL, 0, 0, 200, 'Ollama 本地嵌入模型，768 维度');

