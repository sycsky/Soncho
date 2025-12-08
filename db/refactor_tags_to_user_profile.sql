-- 重构标签系统：将标签从customer表迁移到独立的user_tags和user_ai_tags表
-- 注意：表名为user_tags和user_ai_tags，但实际存储的是客户(Customer)的标签，user_id列对应customerId

-- user_tags表和user_ai_tags表会由JPA自动创建，通过Customer实体的@ElementCollection注解

-- 1. 迁移现有customer表的tags数据（如果存在）
-- 如果customers表有tags字段且包含数据，需要先迁移数据
-- 注意：这里假设原tags字段是JSON数组或逗号分隔的字符串，需要根据实际情况调整

-- 2. 移除customer表的tags字段（如果存在）
ALTER TABLE customers DROP COLUMN IF EXISTS tags;

-- 注意事项：
-- 1. user_tags和user_ai_tags表会由JPA自动创建
--    - user_tags表结构: (user_id UUID, tag VARCHAR)
--    - user_ai_tags表结构: (user_id UUID, tag VARCHAR)
--    - 这里的user_id实际上是customerId（对应customers表的id）
-- 2. 表名虽然是user_tags，但是存储的是Customer的标签
-- 3. 如果需要手动创建表，可以使用以下SQL：
/*
CREATE TABLE IF NOT EXISTS user_tags (
    user_id UUID NOT NULL,
    tag VARCHAR(255) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_ai_tags (
    user_id UUID NOT NULL,
    tag VARCHAR(255) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_tags_user_id ON user_tags(user_id);
CREATE INDEX idx_user_ai_tags_user_id ON user_ai_tags(user_id);
*/

