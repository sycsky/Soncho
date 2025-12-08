-- 为 messages 表添加已读状态字段
-- 用于离线消息功能

ALTER TABLE messages 
ADD COLUMN read_by_customer BOOLEAN NOT NULL DEFAULT FALSE COMMENT '客户是否已读',
ADD COLUMN read_by_agent BOOLEAN NOT NULL DEFAULT FALSE COMMENT '客服是否已读';

-- 创建索引以提高查询性能
CREATE INDEX idx_messages_read_by_customer ON messages(session_id, read_by_customer, sender_type);
CREATE INDEX idx_messages_read_by_agent ON messages(session_id, read_by_agent, sender_type);

-- 将现有消息标记为已读（避免推送历史消息）
UPDATE messages SET read_by_customer = TRUE WHERE sender_type = 'USER';
UPDATE messages SET read_by_agent = TRUE WHERE sender_type IN ('AGENT', 'SYSTEM');

COMMIT;
