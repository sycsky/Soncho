-- 结构化提取模式表
CREATE TABLE IF NOT EXISTS extraction_schemas (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE COMMENT '模式名称',
    description VARCHAR(500) COMMENT '模式描述',
    fields_json TEXT NOT NULL COMMENT '字段定义 JSON',
    extraction_prompt TEXT COMMENT '提取提示词',
    followup_prompt TEXT COMMENT '追问提示词',
    llm_model_id CHAR(36) COMMENT '使用的LLM模型ID',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by CHAR(36) COMMENT '创建者ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_extraction_schema_name (name),
    INDEX idx_extraction_schema_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结构化提取模式表';

-- 结构化提取会话表
CREATE TABLE IF NOT EXISTS extraction_sessions (
    id CHAR(36) PRIMARY KEY,
    schema_id CHAR(36) NOT NULL COMMENT '关联的模式ID',
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '会话状态',
    extracted_data TEXT COMMENT '已提取的数据 JSON',
    missing_fields TEXT COMMENT '缺失的必填字段 JSON',
    conversation_history TEXT COMMENT '对话历史 JSON',
    current_round INT DEFAULT 0 COMMENT '当前轮次',
    max_rounds INT DEFAULT 5 COMMENT '最大轮次',
    reference_id CHAR(36) COMMENT '关联业务ID',
    reference_type VARCHAR(50) COMMENT '关联业务类型',
    created_by CHAR(36) COMMENT '创建者ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_ext_session_schema (schema_id),
    INDEX idx_ext_session_status (status),
    INDEX idx_ext_session_reference (reference_id, reference_type),
    CONSTRAINT fk_ext_session_schema FOREIGN KEY (schema_id) 
        REFERENCES extraction_schemas(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结构化提取会话表';

-- 示例：创建一个订单信息提取模式
-- INSERT INTO extraction_schemas (id, name, description, fields_json, enabled) VALUES (
--     UUID(),
--     '订单信息提取',
--     '从用户消息中提取订单相关信息',
--     '[
--         {"name": "orderNumber", "displayName": "订单号", "type": "STRING", "required": true, "description": "订单编号", "validationPattern": "^[A-Z0-9]{10,20}$", "followupQuestion": "请提供您的订单号"},
--         {"name": "customerName", "displayName": "客户姓名", "type": "STRING", "required": true, "description": "客户姓名", "followupQuestion": "请问您的姓名是？"},
--         {"name": "phone", "displayName": "联系电话", "type": "PHONE", "required": true, "description": "联系电话", "followupQuestion": "请提供您的联系电话"},
--         {"name": "issue", "displayName": "问题描述", "type": "STRING", "required": true, "description": "问题或需求描述"},
--         {"name": "urgency", "displayName": "紧急程度", "type": "ENUM", "required": false, "enumValues": ["低", "中", "高", "紧急"], "defaultValue": "中"}
--     ]',
--     TRUE
-- );

