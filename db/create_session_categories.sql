-- 会话分类模块数据库迁移脚本 (MySQL)
-- 创建日期: 2025-11-27

-- 1. 创建会话分类表
CREATE TABLE IF NOT EXISTS session_categories (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE COMMENT '分类名称（全局唯一）',
    description TEXT COMMENT '分类描述',
    icon VARCHAR(50) COMMENT '分类图标',
    color VARCHAR(20) COMMENT '分类颜色',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_by_agent_id CHAR(36) NOT NULL COMMENT '创建人ID（通常是管理员）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_session_categories_agent
        FOREIGN KEY (created_by_agent_id) REFERENCES agents(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话分类表，用于对会话进行分类管理';

-- 创建索引
CREATE INDEX idx_session_categories_name ON session_categories(name);
CREATE INDEX idx_session_categories_enabled ON session_categories(enabled);
CREATE INDEX idx_session_categories_sort_order ON session_categories(sort_order);
CREATE INDEX idx_session_categories_created_by ON session_categories(created_by_agent_id);

-- 2. 为chat_sessions表添加category_id字段
ALTER TABLE chat_sessions
ADD COLUMN category_id CHAR(36) NULL COMMENT '会话分类ID（可选）';

-- 添加外键约束
ALTER TABLE chat_sessions
ADD CONSTRAINT fk_chat_sessions_category
    FOREIGN KEY (category_id) REFERENCES session_categories(id) ON DELETE SET NULL;

-- 创建索引
CREATE INDEX idx_chat_sessions_category ON chat_sessions(category_id);

-- 3. 创建分组与分类绑定表
CREATE TABLE IF NOT EXISTS session_group_category_bindings (
    id CHAR(36) PRIMARY KEY,
    session_group_id CHAR(36) NOT NULL COMMENT '分组ID',
    category_id CHAR(36) NOT NULL COMMENT '分类ID',
    agent_id CHAR(36) NOT NULL COMMENT '客服ID（冗余字段，用于唯一约束）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_bindings_session_group
        FOREIGN KEY (session_group_id) REFERENCES session_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_bindings_category
        FOREIGN KEY (category_id) REFERENCES session_categories(id) ON DELETE CASCADE,
    CONSTRAINT fk_bindings_agent
        FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    -- 同一Agent下，一个分类只能绑定到一个分组
    CONSTRAINT uk_agent_category UNIQUE (agent_id, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分组与分类绑定表，记录客服的分组绑定了哪些分类';

-- 创建索引
CREATE INDEX idx_bindings_session_group ON session_group_category_bindings(session_group_id);
CREATE INDEX idx_bindings_category ON session_group_category_bindings(category_id);
CREATE INDEX idx_bindings_agent ON session_group_category_bindings(agent_id);
