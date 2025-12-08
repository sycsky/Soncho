-- 为 chat_sessions 表添加 customer_language 字段
-- 用于存储客户使用的语言代码（如 zh-TW, en, ja）

ALTER TABLE chat_sessions 
ADD COLUMN customer_language VARCHAR(10) DEFAULT NULL;

-- 添加注释
ALTER TABLE chat_sessions 
MODIFY COLUMN customer_language VARCHAR(10) COMMENT '客户使用的语言代码，如 zh-TW, en, ja';

-- 创建索引（可选，如果需要按语言筛选会话）
CREATE INDEX idx_chat_sessions_customer_language ON chat_sessions(customer_language);

