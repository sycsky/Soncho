-- 工作流与会话分类绑定表
-- 关系：一个工作流可以绑定多个分类，一个分类只能绑定一个工作流
CREATE TABLE IF NOT EXISTS workflow_category_bindings (
    id CHAR(36) PRIMARY KEY,
    workflow_id CHAR(36) NOT NULL COMMENT '工作流ID',
    category_id CHAR(36) NOT NULL COMMENT '会话分类ID（唯一，一个分类只能绑定一个工作流）',
    priority INT DEFAULT 0 COMMENT '优先级（数字越小优先级越高）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 唯一约束：一个分类只能绑定一个工作流
    UNIQUE KEY uk_wcb_category (category_id),
    
    -- 索引
    INDEX idx_wcb_workflow (workflow_id),
    INDEX idx_wcb_priority (priority),
    
    -- 外键约束
    CONSTRAINT fk_wcb_workflow FOREIGN KEY (workflow_id) 
        REFERENCES ai_workflows(id) ON DELETE CASCADE,
    CONSTRAINT fk_wcb_category FOREIGN KEY (category_id) 
        REFERENCES session_categories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
  COMMENT='工作流与会话分类绑定表';

-- 示例：绑定工作流到分类
-- INSERT INTO workflow_category_bindings (id, workflow_id, category_id, priority)
-- VALUES (UUID(), 'workflow-uuid', 'category-uuid', 0);

-- 查询分类绑定的工作流
-- SELECT w.* FROM ai_workflows w
-- JOIN workflow_category_bindings b ON w.id = b.workflow_id
-- WHERE b.category_id = 'category-uuid'
-- AND w.enabled = true;

-- 查询工作流可绑定的分类（未被其他工作流绑定的分类）
-- SELECT c.* FROM session_categories c
-- WHERE c.enabled = true
-- AND c.id NOT IN (
--     SELECT b.category_id FROM workflow_category_bindings b 
--     WHERE b.workflow_id != 'current-workflow-uuid'
-- )
-- ORDER BY c.sort_order ASC;

