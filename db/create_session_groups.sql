-- 创建 Session 分组表
CREATE TABLE IF NOT EXISTS session_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    is_system BOOLEAN NOT NULL DEFAULT false,
    agent_id UUID NOT NULL REFERENCES agents(id) ON DELETE CASCADE,
    icon VARCHAR(50),
    color VARCHAR(20),
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_session_group_agent_name UNIQUE (agent_id, name)
);

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_session_groups_agent_id ON session_groups(agent_id);
CREATE INDEX IF NOT EXISTS idx_session_groups_system ON session_groups(is_system);
CREATE INDEX IF NOT EXISTS idx_session_groups_sort_order ON session_groups(sort_order);

-- 在 chat_sessions 表中添加 session_group_id 字段
ALTER TABLE chat_sessions 
ADD COLUMN IF NOT EXISTS session_group_id UUID REFERENCES session_groups(id) ON DELETE SET NULL;

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_chat_sessions_session_group ON chat_sessions(session_group_id);

-- 注释
COMMENT ON TABLE session_groups IS 'Session 分组表 - 客服可以将会话分配到不同的组进行管理';
COMMENT ON COLUMN session_groups.name IS '分组名称';
COMMENT ON COLUMN session_groups.is_system IS '是否为系统分组（系统分组不能删除）';
COMMENT ON COLUMN session_groups.agent_id IS '分组所属客服';
COMMENT ON COLUMN session_groups.icon IS '分组图标（emoji 或 图标名称）';
COMMENT ON COLUMN session_groups.color IS '分组颜色';
COMMENT ON COLUMN session_groups.sort_order IS '排序顺序';
COMMENT ON COLUMN chat_sessions.session_group_id IS 'Session 分组 ID';
