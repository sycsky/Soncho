-- AI 工具表
CREATE TABLE IF NOT EXISTS ai_tools (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE COMMENT '工具名称（唯一标识）',
    display_name VARCHAR(100) COMMENT '显示名称',
    description TEXT COMMENT '工具描述',
    tool_type VARCHAR(20) NOT NULL COMMENT '工具类型: API, MCP',
    
    -- 关联 ExtractionSchema（1对1关系）
    schema_id CHAR(36) COMMENT '关联的 ExtractionSchema ID',
    
    -- API 配置
    api_method VARCHAR(10) COMMENT 'HTTP 方法',
    api_url VARCHAR(500) COMMENT 'API URL',
    api_headers TEXT COMMENT '请求头 JSON',
    api_body_template TEXT COMMENT '请求体模板',
    api_response_path VARCHAR(200) COMMENT '响应解析路径',
    api_timeout INT DEFAULT 30 COMMENT '超时时间（秒）',
    
    -- MCP 配置
    mcp_endpoint VARCHAR(500) COMMENT 'MCP 端点',
    mcp_tool_name VARCHAR(100) COMMENT 'MCP 工具名称',
    mcp_server_type VARCHAR(20) COMMENT 'MCP 服务器类型',
    mcp_config TEXT COMMENT 'MCP 配置 JSON',
    
    -- 认证配置
    auth_type VARCHAR(20) DEFAULT 'NONE' COMMENT '认证类型',
    auth_config TEXT COMMENT '认证配置 JSON',
    
    -- 其他配置
    input_example TEXT COMMENT '输入示例',
    output_example TEXT COMMENT '输出示例',
    result_description TEXT COMMENT '返回结果描述（帮助 AI 理解返回数据）',
    result_metadata TEXT COMMENT '返回字段元数据 JSON',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    require_confirmation BOOLEAN DEFAULT FALSE COMMENT '是否需要确认',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT DEFAULT 0 COMMENT '排序',
    tags TEXT COMMENT '标签 JSON',
    
    created_by CHAR(36) COMMENT '创建者ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_ai_tool_name (name),
    INDEX idx_ai_tool_type (tool_type),
    INDEX idx_ai_tool_enabled (enabled),
    INDEX idx_ai_tool_schema (schema_id),
    CONSTRAINT fk_ai_tool_schema FOREIGN KEY (schema_id) 
        REFERENCES extraction_schemas(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 工具表';

-- 工具执行记录表
CREATE TABLE IF NOT EXISTS tool_executions (
    id CHAR(36) PRIMARY KEY,
    tool_id CHAR(36) NOT NULL COMMENT '工具ID',
    session_id CHAR(36) COMMENT '会话ID',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '执行状态',
    input_params TEXT COMMENT '输入参数 JSON',
    output_result TEXT COMMENT '输出结果 JSON',
    error_message TEXT COMMENT '错误信息',
    duration_ms BIGINT COMMENT '执行耗时（毫秒）',
    http_status INT COMMENT 'HTTP 状态码',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    trigger_source VARCHAR(50) COMMENT '触发来源',
    executed_by CHAR(36) COMMENT '执行者ID',
    started_at DATETIME COMMENT '开始时间',
    finished_at DATETIME COMMENT '完成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_tool_exec_tool (tool_id),
    INDEX idx_tool_exec_status (status),
    INDEX idx_tool_exec_session (session_id),
    INDEX idx_tool_exec_created (created_at),
    CONSTRAINT fk_tool_exec_tool FOREIGN KEY (tool_id) 
        REFERENCES ai_tools(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具执行记录表';

-- 示例：创建一个天气查询工具
-- INSERT INTO ai_tools (id, name, display_name, description, tool_type, api_method, api_url, api_timeout, enabled) 
-- VALUES (
--     UUID(),
--     'get_weather',
--     '天气查询',
--     '查询指定城市的天气信息',
--     'API',
--     'GET',
--     'https://api.weather.com/v1/weather?city={{city}}',
--     30,
--     TRUE
-- );

