-- 为chat_sessions表添加note字段
-- 用于存储会话备注,每个会话对应一个备注

ALTER TABLE chat_sessions 
ADD COLUMN note TEXT DEFAULT NULL COMMENT '会话备注';
