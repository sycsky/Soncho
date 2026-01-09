-- 事件模块数据库迁移脚本 (MySQL)
-- 创建日期: 2025-12-20

-- 创建事件表
CREATE TABLE IF NOT EXISTS events (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE COMMENT '事件名称（唯一标识，用于hook接收）',
    display_name VARCHAR(255) COMMENT '事件显示名称',
    description TEXT COMMENT '事件描述',
    workflow_id CHAR(36) NOT NULL COMMENT '绑定的工作流ID',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_events_workflow
        FOREIGN KEY (workflow_id) REFERENCES ai_workflows(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件配置表，用于配置外部事件hook并绑定工作流';

-- 创建索引
CREATE INDEX idx_events_name ON events(name);
CREATE INDEX idx_events_enabled ON events(enabled);
CREATE INDEX idx_events_sort_order ON events(sort_order);
CREATE INDEX idx_events_workflow_id ON events(workflow_id);





