-- AI 工作流相关表
-- 执行前请确保数据库已存在

-- 1. AI 工作流表
CREATE TABLE IF NOT EXISTS ai_workflows (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '工作流名称',
    description TEXT COMMENT '工作流描述',
    nodes_json LONGTEXT COMMENT 'ReactFlow 节点数据 (JSON)',
    edges_json LONGTEXT COMMENT 'ReactFlow 边数据 (JSON)',
    liteflow_el TEXT COMMENT 'LiteFlow EL 表达式（主链）',
    sub_chains_json TEXT COMMENT 'LLM子链信息 (JSON)，用于工具调用暂停/恢复',
    llm_node_ids TEXT COMMENT 'LLM节点ID列表 (JSON)',
    version INT NOT NULL DEFAULT 1 COMMENT '版本号',
    enabled BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否启用',
    is_default BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为默认工作流',
    created_by_agent_id CHAR(36) COMMENT '创建者客服ID',
    trigger_type VARCHAR(50) DEFAULT 'ALL' COMMENT '触发条件类型: ALL, CATEGORY, KEYWORD',
    trigger_config TEXT COMMENT '触发条件配置 (JSON)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_workflow_name (name),
    KEY idx_workflow_enabled (enabled),
    KEY idx_workflow_default (is_default),
    KEY idx_workflow_trigger_type (trigger_type),
    KEY idx_workflow_created_by (created_by_agent_id),
    
    CONSTRAINT fk_workflow_agent FOREIGN KEY (created_by_agent_id) 
        REFERENCES agents(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 工作流';

-- 2. 工作流执行日志表
CREATE TABLE IF NOT EXISTS workflow_execution_logs (
    id CHAR(36) PRIMARY KEY,
    workflow_id CHAR(36) NOT NULL COMMENT '工作流ID',
    session_id CHAR(36) COMMENT '关联会话ID',
    status VARCHAR(20) NOT NULL COMMENT '执行状态: SUCCESS, FAILED, TIMEOUT',
    user_input TEXT COMMENT '用户输入消息',
    final_output TEXT COMMENT '最终输出/回复',
    node_details LONGTEXT COMMENT '节点执行详情 (JSON)',
    error_message TEXT COMMENT '错误信息',
    duration_ms BIGINT COMMENT '执行耗时（毫秒）',
    started_at TIMESTAMP NOT NULL COMMENT '执行开始时间',
    finished_at TIMESTAMP COMMENT '执行结束时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    KEY idx_log_workflow (workflow_id),
    KEY idx_log_session (session_id),
    KEY idx_log_status (status),
    KEY idx_log_created (created_at),
    
    CONSTRAINT fk_log_workflow FOREIGN KEY (workflow_id) 
        REFERENCES ai_workflows(id) ON DELETE CASCADE,
    CONSTRAINT fk_log_session FOREIGN KEY (session_id) 
        REFERENCES chat_sessions(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流执行日志';

-- 3. 为已存在的表添加子链相关字段（如果表已存在）
-- ALTER TABLE ai_workflows ADD COLUMN sub_chains_json TEXT COMMENT 'LLM子链信息 (JSON)' AFTER liteflow_el;
-- ALTER TABLE ai_workflows ADD COLUMN llm_node_ids TEXT COMMENT 'LLM节点ID列表 (JSON)' AFTER sub_chains_json;

-- 4. 创建一个示例工作流（可选）
-- INSERT INTO ai_workflows (id, name, description, nodes_json, edges_json, liteflow_el, enabled, is_default, trigger_type)
-- VALUES (
--     UUID(),
--     '默认智能客服工作流',
--     '基础的智能客服工作流，包含意图识别和LLM回复',
--     '[{"id":"start_1","type":"start","data":{"label":"开始"},"position":{"x":100,"y":100}},{"id":"llm_1","type":"llm","data":{"label":"LLM对话","config":{"model":"gpt-3.5-turbo","systemPrompt":"你是一个友好的智能客服助手。"}},"position":{"x":100,"y":200}},{"id":"reply_1","type":"reply","data":{"label":"回复","config":{"replyType":"lastOutput"}},"position":{"x":100,"y":300}},{"id":"end_1","type":"end","data":{"label":"结束"},"position":{"x":100,"y":400}}]',
--     '[{"id":"e1","source":"start_1","target":"llm_1"},{"id":"e2","source":"llm_1","target":"reply_1"},{"id":"e3","source":"reply_1","target":"end_1"}]',
--     'start_1, llm_1, reply_1, end_1',
--     TRUE,
--     TRUE,
--     'ALL'
-- );

