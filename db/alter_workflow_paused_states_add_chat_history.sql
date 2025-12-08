-- 为 workflow_paused_states 表添加 chat_history_json 字段
-- 用于保存暂停时的 LLM 对话历史，恢复时继续对话

ALTER TABLE workflow_paused_states
ADD COLUMN IF NOT EXISTS chat_history_json LONGTEXT COMMENT 'LLM 对话历史 (JSON)';

