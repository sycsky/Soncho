-- 消息发送记录表
-- 用于追踪每条消息对每个客服的发送状态
-- 注意：只为客服创建发送记录，客户通过历史消息接口获取消息

CREATE TABLE IF NOT EXISTS message_delivery (
    id CHAR(36) PRIMARY KEY COMMENT '主键ID',
    message_id CHAR(36) NOT NULL COMMENT '消息ID',
    agent_id CHAR(36) NOT NULL COMMENT '接收客服ID',
    customer_id CHAR(36) COMMENT '预留字段（当前不使用，客户通过历史消息获取）',
    is_sent BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已发送',
    sent_at TIMESTAMP NULL COMMENT '发送时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    -- 外键约束
    CONSTRAINT fk_delivery_message FOREIGN KEY (message_id) REFERENCES message(id) ON DELETE CASCADE,
    
    -- 索引：优化查询未发送消息
    INDEX idx_agent_not_sent (agent_id, is_sent),
    INDEX idx_message (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息发送记录表（仅客服）';

-- 说明：
-- 1. 每条消息会为每个客服创建一条发送记录
-- 2. 客户不创建发送记录，客户通过历史消息接口获取所有消息
-- 3. isSent = false 表示该消息尚未推送给该客服
-- 4. 发送者（客服）也会创建一条记录，但默认 isSent = true（因为发送即代表已收到）
