-- Agent被@记录表数据库迁移脚本 (MySQL)
-- 创建日期: 2025-11-27

-- 创建 agent_mentions 表
CREATE TABLE IF NOT EXISTS agent_mentions (
    id CHAR(36) PRIMARY KEY,
    agent_id CHAR(36) NOT NULL COMMENT '被@的客服ID',
    session_id CHAR(36) NOT NULL COMMENT '会话ID',
    message_id CHAR(36) NULL COMMENT '关联的消息ID（可选）',
    is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_mentions_agent
        FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_mentions_session
        FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_mentions_message
        FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent被@记录表';

-- 创建索引
CREATE INDEX idx_agent_mentions_agent ON agent_mentions(agent_id);
CREATE INDEX idx_agent_mentions_session ON agent_mentions(session_id);
CREATE INDEX idx_agent_mentions_message ON agent_mentions(message_id);
CREATE INDEX idx_agent_mentions_agent_read ON agent_mentions(agent_id, is_read);
CREATE INDEX idx_agent_mentions_created_at ON agent_mentions(created_at);

