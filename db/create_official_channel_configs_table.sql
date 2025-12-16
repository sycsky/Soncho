-- 创建官方渠道配置表
CREATE TABLE IF NOT EXISTS official_channel_configs (
    id CHAR(36) PRIMARY KEY,
    channel_type VARCHAR(20) NOT NULL UNIQUE COMMENT '渠道类型（WECHAT_OFFICIAL, LINE_OFFICIAL, WHATSAPP_OFFICIAL）',
    display_name VARCHAR(100) COMMENT '渠道显示名称',
    enabled BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否启用',
    config_json TEXT COMMENT '配置信息（JSON格式，存储各平台特定的配置）',
    webhook_secret VARCHAR(200) COMMENT 'Webhook验证密钥',
    webhook_url VARCHAR(500) COMMENT 'Webhook URL（系统提供的固定URL）',
    remark TEXT COMMENT '备注',
    category_id CHAR(36) COMMENT '会话分类ID（可选，当收到官方渠道消息创建会话时，如果配置了此字段，会自动设置会话分类）',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_channel_type (channel_type),
    INDEX idx_enabled (enabled),
    INDEX idx_category_id (category_id),
    FOREIGN KEY (category_id) REFERENCES session_categories(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='官方渠道配置表';

