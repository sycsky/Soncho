-- ================================================================
-- PGVector 向量数据库初始化脚本
-- 用于知识库向量存储
-- ================================================================

-- 1. 创建数据库（如果不存在）
-- 需要以 postgres 超级用户执行
-- CREATE DATABASE aikef_vector;

-- 2. 连接到数据库后，启用 vector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 3. 验证扩展是否安装成功
SELECT * FROM pg_extension WHERE extname = 'vector';

-- ================================================================
-- 说明：
-- 
-- LangChain4j 的 PgVectorEmbeddingStore 会自动创建向量表
-- 表名由 KnowledgeBase.indexName 决定
-- 
-- 自动创建的表结构类似于：
-- CREATE TABLE IF NOT EXISTS {table_name} (
--     embedding_id UUID PRIMARY KEY,
--     embedding vector({dimension}),
--     text TEXT,
--     metadata JSONB
-- );
-- 
-- CREATE INDEX IF NOT EXISTS {table_name}_embedding_idx 
-- ON {table_name} USING ivfflat (embedding vector_cosine_ops);
-- ================================================================

-- 4. 查看已创建的向量表（调试用）
-- SELECT table_name FROM information_schema.tables 
-- WHERE table_schema = 'public' AND table_name LIKE 'kb_%';

-- 5. 查看向量表内容示例（调试用）
-- SELECT embedding_id, text, metadata, 
--        embedding <=> '[0.1, 0.2, ...]'::vector AS distance
-- FROM kb_default
-- ORDER BY embedding <=> '[0.1, 0.2, ...]'::vector
-- LIMIT 5;

