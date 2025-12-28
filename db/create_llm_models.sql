-- LLM 模型配置表
-- 执行前请确保数据库已存在

-- 1. LLM 模型配置表
CREATE TABLE IF NOT EXISTS llm_models (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '模型名称（显示用）',
    code VARCHAR(50) NOT NULL COMMENT '模型编码（唯一标识）',
    provider VARCHAR(50) NOT NULL COMMENT '提供商: OPENAI, AZURE_OPENAI, OLLAMA, ZHIPU, DASHSCOPE, MOONSHOT, DEEPSEEK, GEMINI, CUSTOM',
    model_name VARCHAR(100) NOT NULL COMMENT 'API 中使用的模型名，如 gpt-4, qwen-plus',
    base_url VARCHAR(500) COMMENT 'API Base URL',
    api_key TEXT COMMENT 'API Key（建议加密存储）',
    azure_deployment_name VARCHAR(100) COMMENT 'Azure 部署名称',
    default_temperature DOUBLE DEFAULT 0.7 COMMENT '默认温度参数',
    default_max_tokens INT DEFAULT 2000 COMMENT '默认最大 Token 数',
    context_window INT DEFAULT 4096 COMMENT '上下文窗口大小',
    input_price_per_1k DOUBLE COMMENT '每千 Token 输入价格（美元）',
    output_price_per_1k DOUBLE COMMENT '每千 Token 输出价格（美元）',
    supports_functions BOOLEAN DEFAULT FALSE COMMENT '是否支持函数调用',
    supports_vision BOOLEAN DEFAULT FALSE COMMENT '是否支持视觉',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    is_default BOOLEAN DEFAULT FALSE COMMENT '是否为默认模型',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    description TEXT COMMENT '描述',
    extra_config TEXT COMMENT '额外配置（JSON）',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_model_code (code),
    KEY idx_model_provider (provider),
    KEY idx_model_enabled (enabled),
    KEY idx_model_default (is_default),
    KEY idx_model_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LLM 模型配置';

-- 2. 插入示例模型配置
INSERT INTO llm_models (id, name, code, provider, model_name, base_url, default_temperature, default_max_tokens, context_window, input_price_per_1k, output_price_per_1k, supports_functions, enabled, is_default, sort_order, description) VALUES
-- OpenAI 模型
(UUID(), 'GPT-4o', 'gpt-4o', 'OPENAI', 'gpt-4o', 'https://api.openai.com/v1', 0.7, 4096, 128000, 0.005, 0.015, TRUE, TRUE, TRUE, 1, 'OpenAI 最新的多模态模型，支持文本和图片'),
(UUID(), 'GPT-4o Mini', 'gpt-4o-mini', 'OPENAI', 'gpt-4o-mini', 'https://api.openai.com/v1', 0.7, 4096, 128000, 0.00015, 0.0006, TRUE, TRUE, FALSE, 2, 'GPT-4o 的轻量版本，性价比高'),
(UUID(), 'GPT-4 Turbo', 'gpt-4-turbo', 'OPENAI', 'gpt-4-turbo', 'https://api.openai.com/v1', 0.7, 4096, 128000, 0.01, 0.03, TRUE, TRUE, FALSE, 3, 'GPT-4 Turbo 模型'),
(UUID(), 'GPT-3.5 Turbo', 'gpt-35-turbo', 'OPENAI', 'gpt-3.5-turbo', 'https://api.openai.com/v1', 0.7, 4096, 16385, 0.0005, 0.0015, TRUE, TRUE, FALSE, 4, '经济实惠的 GPT-3.5 模型'),

-- DeepSeek 模型
(UUID(), 'DeepSeek Chat', 'deepseek-chat', 'DEEPSEEK', 'deepseek-chat', 'https://api.deepseek.com/v1', 0.7, 4096, 64000, 0.00014, 0.00028, TRUE, FALSE, FALSE, 10, 'DeepSeek 对话模型'),
(UUID(), 'DeepSeek Coder', 'deepseek-coder', 'DEEPSEEK', 'deepseek-coder', 'https://api.deepseek.com/v1', 0.2, 4096, 64000, 0.00014, 0.00028, TRUE, FALSE, FALSE, 11, 'DeepSeek 编程模型'),

-- 月之暗面 Moonshot
(UUID(), 'Moonshot v1 8K', 'moonshot-v1-8k', 'MOONSHOT', 'moonshot-v1-8k', 'https://api.moonshot.cn/v1', 0.7, 4096, 8192, 0.012, 0.012, FALSE, FALSE, FALSE, 20, 'Kimi 8K 上下文模型'),
(UUID(), 'Moonshot v1 32K', 'moonshot-v1-32k', 'MOONSHOT', 'moonshot-v1-32k', 'https://api.moonshot.cn/v1', 0.7, 4096, 32768, 0.024, 0.024, FALSE, FALSE, FALSE, 21, 'Kimi 32K 上下文模型'),
(UUID(), 'Moonshot v1 128K', 'moonshot-v1-128k', 'MOONSHOT', 'moonshot-v1-128k', 'https://api.moonshot.cn/v1', 0.7, 4096, 131072, 0.06, 0.06, FALSE, FALSE, FALSE, 22, 'Kimi 128K 上下文模型'),

-- 智谱 AI
(UUID(), 'GLM-4', 'glm-4', 'ZHIPU', 'glm-4', NULL, 0.7, 4096, 128000, 0.1, 0.1, TRUE, FALSE, FALSE, 30, '智谱 GLM-4 模型'),
(UUID(), 'GLM-4 Flash', 'glm-4-flash', 'ZHIPU', 'glm-4-flash', NULL, 0.7, 4096, 128000, 0.001, 0.001, TRUE, FALSE, FALSE, 31, '智谱 GLM-4 Flash 免费模型'),

-- 通义千问
(UUID(), '通义千问 Max', 'qwen-max', 'DASHSCOPE', 'qwen-max', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 0.7, 4096, 32768, 0.04, 0.12, TRUE, FALSE, FALSE, 40, '阿里云通义千问 Max'),
(UUID(), '通义千问 Plus', 'qwen-plus', 'DASHSCOPE', 'qwen-plus', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 0.7, 4096, 131072, 0.004, 0.012, TRUE, FALSE, FALSE, 41, '阿里云通义千问 Plus'),
(UUID(), '通义千问 Turbo', 'qwen-turbo', 'DASHSCOPE', 'qwen-turbo', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 0.7, 4096, 131072, 0.002, 0.006, TRUE, FALSE, FALSE, 42, '阿里云通义千问 Turbo'),

-- Ollama 本地模型（示例）
(UUID(), 'Ollama Llama3', 'ollama-llama3', 'OLLAMA', 'llama3', 'http://localhost:11434', 0.7, 4096, 8192, 0, 0, FALSE, FALSE, FALSE, 100, '本地 Llama3 模型（需先安装 Ollama）'),
(UUID(), 'Ollama Qwen2', 'ollama-qwen2', 'OLLAMA', 'qwen2', 'http://localhost:11434', 0.7, 4096, 32768, 0, 0, FALSE, FALSE, FALSE, 101, '本地 Qwen2 模型（需先安装 Ollama）');

-- 注意：
-- 1. api_key 字段需要在创建后手动设置
-- 2. Ollama 模型默认禁用，需要本地安装 Ollama 后再启用
-- 3. 价格仅供参考，请以官方最新价格为准

