-- 为 customers 表的 primary_channel 列添加 CUSTOM 枚举值
-- 如果 primary_channel 是 ENUM 类型，执行以下语句：

-- 方法1：如果是 ENUM 类型，修改枚举值列表
ALTER TABLE customers 
MODIFY COLUMN primary_channel ENUM('WEB', 'WECHAT', 'WHATSAPP', 'LINE', 'TELEGRAM', 'FACEBOOK', 'EMAIL', 'SMS', 'PHONE', 'APP', 'CUSTOM') NOT NULL;

-- 方法2：如果要改为 VARCHAR 类型（更灵活）
-- ALTER TABLE customers MODIFY COLUMN primary_channel VARCHAR(50) NOT NULL;







