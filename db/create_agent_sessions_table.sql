-- 创建 agent_sessions 表
-- 用于记录特殊工作流的状态，直到走到 Agent End 节点
-- 
-- 执行顺序：
-- 1. 先执行 create_ai_workflows.sql（如果 ai_workflows 表不存在）
-- 2. 再执行此脚本

-- 创建表（不包含外键约束，避免依赖问题）
CREATE TABLE IF NOT EXISTS agent_sessions (
    id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY,
    session_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID',
    workflow_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工作流ID（Agent 节点配置的工作流）',
    sys_prompt TEXT COMMENT '系统提示词（sysPrompt）',
    is_ended BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已结束（走到 Agent End 节点）',
    ended_at TIMESTAMP NULL COMMENT '结束时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_session_workflow (session_id, workflow_id),
    INDEX idx_session_id (session_id),
    INDEX idx_workflow_id (workflow_id),
    INDEX idx_is_ended (is_ended)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 会话表';

-- 添加外键约束
-- 如果 ai_workflows 表不存在，此语句会失败
-- 请先执行 create_ai_workflows.sql，然后再执行下面的语句
-- 
-- 如果遇到 "incompatible" 错误，请先执行以下语句修改字段定义：
-- ALTER TABLE agent_sessions MODIFY COLUMN workflow_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;
-- 然后再执行下面的外键约束语句

-- 如果外键已存在，可以忽略此错误
ALTER TABLE agent_sessions 
ADD CONSTRAINT fk_agent_sessions_workflow 
FOREIGN KEY (workflow_id) REFERENCES ai_workflows(id) ON DELETE CASCADE;

