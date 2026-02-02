-- Initial Seed Data for CMS

-- FAQs
INSERT INTO cms_articles (id, title, slug, content, type, status, created_at, updated_at) VALUES 
(UUID(), '你们支持哪些电商平台？', 'supported-platforms', '目前我们深度集成 Shopify。未来计划支持 WooCommerce 和 Magento。', 'FAQ', 'PUBLISHED', NOW(), NOW()),
(UUID(), '如何收费？', 'pricing', '我们提供免费的基础版和按量付费的高级版。详情请查看价格页面。', 'FAQ', 'PUBLISHED', NOW(), NOW()),
(UUID(), '可以自定义 AI 的语气吗？', 'custom-tone', '可以。您可以在设置中通过 System Prompt 调整 AI 的语气和人设。', 'FAQ', 'PUBLISHED', NOW(), NOW());

-- Blog Posts
INSERT INTO cms_articles (id, title, slug, content, type, status, seo_title, seo_description, category, created_at, updated_at) VALUES 
(UUID(), 'AI Agent 在跨境电商中的应用前景', 'ai-agent-in-cross-border-ecommerce', '# AI Agent 在跨境电商中的应用前景\n\n随着人工智能技术的飞速发展...', 'BLOG', 'PUBLISHED', 'AI Agent 跨境电商应用前景', '探讨 AI Agent 如何助力跨境电商卖家降本增效。', '行业洞察', NOW(), NOW()),
(UUID(), '如何配置自动退款工作流', 'how-to-setup-refund-workflow', '# 如何配置自动退款工作流\n\n本文将详细介绍...', 'BLOG', 'PUBLISHED', '自动退款工作流配置教程', '5分钟教程：搭建自动退款流程。', '教程', NOW(), NOW());

-- Documentation
INSERT INTO cms_articles (id, title, slug, content, type, status, category, sort_order, created_at, updated_at) VALUES 
(UUID(), '快速开始指南', 'getting-started', '# 快速开始\n\n欢迎使用 Soncho AI...', 'DOCUMENTATION', 'PUBLISHED', 'getting-started', 1, NOW(), NOW()),
(UUID(), '安装 Shopify 应用', 'install-shopify-app', '# 安装应用\n\n在 Shopify App Store 搜索...', 'DOCUMENTATION', 'PUBLISHED', 'getting-started', 2, NOW(), NOW()),
(UUID(), 'API 认证', 'api-authentication', '# API 认证\n\n所有请求需携带 Token...', 'DOCUMENTATION', 'PUBLISHED', 'api', 1, NOW(), NOW());
