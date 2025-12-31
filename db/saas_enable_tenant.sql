-- Enable Multi-tenancy for all tables
-- Add tenant_id column and index to each table

-- agents
ALTER TABLE agents ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_agents_tenant ON agents(tenant_id);

-- agent_mentions
ALTER TABLE agent_mentions ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_agent_mentions_tenant ON agent_mentions(tenant_id);

-- agent_sessions
ALTER TABLE agent_sessions ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_agent_sessions_tenant ON agent_sessions(tenant_id);

-- ai_workflows
ALTER TABLE ai_workflows ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_ai_workflows_tenant ON ai_workflows(tenant_id);

-- attachments
ALTER TABLE attachments ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_attachments_tenant ON attachments(tenant_id);

-- chat_sessions
ALTER TABLE chat_sessions ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_chat_sessions_tenant ON chat_sessions(tenant_id);

-- customers
ALTER TABLE customers ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_customers_tenant ON customers(tenant_id);

-- events
ALTER TABLE events ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_events_tenant ON events(tenant_id);

-- external_platforms
ALTER TABLE external_platforms ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_external_platforms_tenant ON external_platforms(tenant_id);

-- external_session_mappings
ALTER TABLE external_session_mappings ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_external_session_mappings_tenant ON external_session_mappings(tenant_id);

-- knowledge_bases
ALTER TABLE knowledge_bases ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_knowledge_bases_tenant ON knowledge_bases(tenant_id);

-- knowledge_documents
ALTER TABLE knowledge_documents ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_knowledge_documents_tenant ON knowledge_documents(tenant_id);

-- llm_models
ALTER TABLE llm_models ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_llm_models_tenant ON llm_models(tenant_id);

-- messages
ALTER TABLE messages ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_messages_tenant ON messages(tenant_id);

-- message_delivery
ALTER TABLE message_delivery ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_message_delivery_tenant ON message_delivery(tenant_id);

-- official_channel_configs
ALTER TABLE official_channel_configs ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_official_channel_configs_tenant ON official_channel_configs(tenant_id);

-- quick_replies
ALTER TABLE quick_replies ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_quick_replies_tenant ON quick_replies(tenant_id);

-- read_records
ALTER TABLE read_records ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_read_records_tenant ON read_records(tenant_id);

-- roles
ALTER TABLE roles ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_roles_tenant ON roles(tenant_id);

-- session_categories
ALTER TABLE session_categories ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_session_categories_tenant ON session_categories(tenant_id);

-- session_groups
ALTER TABLE session_groups ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_session_groups_tenant ON session_groups(tenant_id);

-- session_group_category_bindings
ALTER TABLE session_group_category_bindings ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_session_group_category_bindings_tenant ON session_group_category_bindings(tenant_id);

-- session_group_mappings
ALTER TABLE session_group_mappings ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_session_group_mappings_tenant ON session_group_mappings(tenant_id);

-- uploaded_files
ALTER TABLE uploaded_files ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_uploaded_files_tenant ON uploaded_files(tenant_id);

-- workflow_category_bindings
ALTER TABLE workflow_category_bindings ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_workflow_category_bindings_tenant ON workflow_category_bindings(tenant_id);

-- workflow_execution_logs
ALTER TABLE workflow_execution_logs ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_workflow_execution_logs_tenant ON workflow_execution_logs(tenant_id);

-- extraction_schemas
ALTER TABLE extraction_schemas ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_extraction_schemas_tenant ON extraction_schemas(tenant_id);

-- extraction_sessions
ALTER TABLE extraction_sessions ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_extraction_sessions_tenant ON extraction_sessions(tenant_id);

-- ai_tools
ALTER TABLE ai_tools ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_ai_tools_tenant ON ai_tools(tenant_id);

-- tool_executions
ALTER TABLE tool_executions ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_tool_executions_tenant ON tool_executions(tenant_id);

-- workflow_paused_states
ALTER TABLE workflow_paused_states ADD COLUMN tenant_id VARCHAR(50);
CREATE INDEX idx_workflow_paused_states_tenant ON workflow_paused_states(tenant_id);
