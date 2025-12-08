-- 为 chat_sessions 表添加 metadata 字段
-- 用于存储会话创建时的自定义元数据（如来源、设备信息等）

ALTER TABLE chat_sessions
ADD COLUMN IF NOT EXISTS metadata JSON COMMENT '会话元数据（JSON格式）';

-- 创建索引以支持 JSON 查询（可选，根据查询需求）
-- CREATE INDEX idx_chat_sessions_metadata_category ON chat_sessions ((JSON_EXTRACT(metadata, '$.categoryId')));
-- CREATE INDEX idx_chat_sessions_metadata_source ON chat_sessions ((JSON_EXTRACT(metadata, '$.source')));

