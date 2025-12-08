-- 工作流暂停状态表
-- 用于保存因工具调用等待用户输入而暂停的工作流状态

CREATE TABLE IF NOT EXISTS workflow_paused_states (
    id CHAR(36) PRIMARY KEY,
    session_id CHAR(36) NOT NULL COMMENT '会话ID',
    workflow_id CHAR(36) NOT NULL COMMENT '工作流ID',
    sub_chain_id VARCHAR(255) NOT NULL COMMENT '子链ID',
    llm_node_id VARCHAR(100) NOT NULL COMMENT 'LLM 节点ID',
    pause_reason VARCHAR(100) COMMENT '暂停原因',
    context_json LONGTEXT COMMENT '上下文数据 (JSON)',
    tool_call_state_json TEXT COMMENT '工具调用状态 (JSON)',
    collected_params_json TEXT COMMENT '已收集的参数 (JSON)',
    current_round INT DEFAULT 0 COMMENT '当前轮次',
    max_rounds INT DEFAULT 5 COMMENT '最大轮次',
    pending_tool_id CHAR(36) COMMENT '待执行的工具ID',
    pending_tool_name VARCHAR(255) COMMENT '待执行的工具名称',
    next_question TEXT COMMENT '下一个追问问题',
    status VARCHAR(50) NOT NULL DEFAULT 'WAITING_USER_INPUT' COMMENT '状态',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    expired_at TIMESTAMP COMMENT '过期时间',
    
    KEY idx_paused_session (session_id),
    KEY idx_paused_workflow (workflow_id),
    KEY idx_paused_status (status),
    KEY idx_paused_expired (expired_at),
    KEY idx_paused_session_status (session_id, status, expired_at),
    
    CONSTRAINT fk_paused_session FOREIGN KEY (session_id) 
        REFERENCES chat_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_paused_workflow FOREIGN KEY (workflow_id) 
        REFERENCES ai_workflows(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流暂停状态';

-- 创建定时清理过期和已完成状态的事件（可选）
-- CREATE EVENT IF NOT EXISTS cleanup_paused_states
-- ON SCHEDULE EVERY 1 HOUR
-- DO
--     DELETE FROM workflow_paused_states 
--     WHERE (status IN ('COMPLETED', 'CANCELLED', 'EXPIRED') AND updated_at < DATE_SUB(NOW(), INTERVAL 7 DAY))
--        OR (status = 'WAITING_USER_INPUT' AND expired_at < NOW());

