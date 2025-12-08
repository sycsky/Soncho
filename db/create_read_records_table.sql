-- 创建已读记录表
-- 用于记录每个客服对每个会话的最后阅读时间,以计算未读消息数

CREATE TABLE IF NOT EXISTS read_records (
    id CHAR(36) PRIMARY KEY COMMENT '主键ID',
    session_id CHAR(36) NOT NULL COMMENT '会话ID',
    agent_id CHAR(36) NOT NULL COMMENT '客服ID',
    last_read_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后阅读时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_session_agent (session_id, agent_id) COMMENT '会话和客服唯一索引',
    KEY idx_agent_id (agent_id) COMMENT '客服ID索引',
    KEY idx_session_id (session_id) COMMENT '会话ID索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='已读记录表';

-- 说明:
-- 1. 每个客服对每个会话只有一条已读记录
-- 2. last_read_time记录客服最后一次打开该会话的时间
-- 3. 未读消息数 = COUNT(messages WHERE created_at > last_read_time)
-- 4. 如果没有已读记录,则该会话所有消息都是未读的
