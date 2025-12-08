-- 为 agents 表添加 language 字段
-- 用于存储客服使用的语言代码（如 zh-CN, zh-TW, en, ja）

ALTER TABLE agents 
ADD COLUMN language VARCHAR(10) DEFAULT NULL;

-- 添加注释
ALTER TABLE agents 
MODIFY COLUMN language VARCHAR(10) COMMENT '客服使用的语言代码，如 zh-CN, en, ja';

