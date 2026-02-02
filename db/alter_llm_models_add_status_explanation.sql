-- 为 llm_models 表增加 status_explanation 字段
ALTER TABLE llm_models ADD COLUMN status_explanation BOOLEAN DEFAULT FALSE;
