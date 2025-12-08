-- 第三方平台配置表
CREATE TABLE IF NOT EXISTS external_platforms (
    id BINARY(16) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '平台名称（唯一标识）',
    display_name VARCHAR(100) COMMENT '平台显示名称',
    platform_type VARCHAR(20) NOT NULL COMMENT '平台类型: LINE, WHATSAPP, WECHAT, TELEGRAM, FACEBOOK, CUSTOM',
    callback_url VARCHAR(500) COMMENT '消息回调 URL',
    auth_type VARCHAR(20) DEFAULT 'NONE' COMMENT '认证类型: NONE, API_KEY, BEARER_TOKEN, BASIC_AUTH, CUSTOM_HEADER',
    auth_credential VARCHAR(500) COMMENT '认证凭据',
    extra_headers TEXT COMMENT '额外请求头（JSON格式）',
    webhook_secret VARCHAR(200) COMMENT 'Webhook 密钥',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    remark TEXT COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BINARY(16) COMMENT '创建者ID',
    updated_by BINARY(16) COMMENT '更新者ID',
    
    INDEX idx_name (name),
    INDEX idx_platform_type (platform_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='第三方平台配置表';

-- 外部会话映射表
CREATE TABLE IF NOT EXISTS external_session_mappings (
    id BINARY(16) PRIMARY KEY,
    platform_id BINARY(16) NOT NULL COMMENT '关联平台ID',
    external_thread_id VARCHAR(200) NOT NULL COMMENT '外部线程/会话ID',
    session_id BINARY(16) NOT NULL COMMENT '系统会话ID',
    customer_id BINARY(16) NOT NULL COMMENT '客户ID',
    external_user_id VARCHAR(200) COMMENT '外部用户ID',
    external_user_name VARCHAR(200) COMMENT '外部用户名称',
    metadata TEXT COMMENT '额外元数据（JSON格式）',
    active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否活跃',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BINARY(16),
    updated_by BINARY(16),
    
    CONSTRAINT fk_mapping_platform FOREIGN KEY (platform_id) REFERENCES external_platforms(id),
    CONSTRAINT fk_mapping_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id),
    CONSTRAINT fk_mapping_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT uk_platform_thread UNIQUE (platform_id, external_thread_id),
    
    INDEX idx_external_thread_id (external_thread_id),
    INDEX idx_session_id (session_id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='外部会话映射表';

-- 插入示例平台配置
INSERT INTO external_platforms (id, name, display_name, platform_type, enabled, remark) VALUES
(UUID_TO_BIN(UUID()), 'line', 'Line Official Account', 'LINE', TRUE, 'Line 官方账号消息接入'),
(UUID_TO_BIN(UUID()), 'whatsapp', 'WhatsApp Business', 'WHATSAPP', TRUE, 'WhatsApp Business API 接入'),
(UUID_TO_BIN(UUID()), 'wechat', '微信公众号', 'WECHAT', TRUE, '微信公众号消息接入');

