-- 为 chat_sessions 表添加 customer_id 字段
ALTER TABLE chat_sessions 
ADD COLUMN customer_id CHAR(36) AFTER user_id,
ADD INDEX idx_customer_id (customer_id),
ADD CONSTRAINT fk_chat_sessions_customer 
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;

-- 为 messages 表添加 agent_metadata 字段（客服可见的隐藏元数据）
ALTER TABLE messages 
ADD COLUMN agent_metadata JSON AFTER translation_data;

-- 添加注释
ALTER TABLE chat_sessions 
MODIFY COLUMN customer_id CHAR(36) COMMENT '客户ID（新客户模块）';

ALTER TABLE messages 
MODIFY COLUMN agent_metadata JSON COMMENT '客服可见的元数据（对客户隐藏）';
