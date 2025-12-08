# AWS 翻译服务集成指南

## 概述

本系统集成了 AWS Translate 服务，支持自动翻译消息内容到多种目标语言。翻译功能默认开启，可通过配置文件进行管理。

## 功能特点

1. **自动语言检测**：当客户首次发送消息时，系统会自动检测其使用的语言
2. **多语言翻译**：支持将消息翻译成多种配置的目标语言
3. **翻译数据存储**：所有消息的翻译结果存储在 `Message.translationData` 字段中
4. **双向翻译**：
   - 客户消息：翻译为系统默认语言及所有目标语言
   - 客服/AI消息：翻译为客户使用的语言

## 配置说明

### application.yml 配置

```yaml
# AWS Translate 翻译服务配置
translation:
  enabled: true  # 是否启用翻译服务
  aws:
    access-key: ${AWS_ACCESS_KEY:}
    secret-key: ${AWS_SECRET_KEY:}
    region: ${AWS_REGION:us-east-1}
  # 支持的目标语言列表
  target-languages:
    - code: zh-TW
      name: 中文繁体
    - code: en
      name: English
    - code: ja
      name: 日本語
    # 可按需添加更多语言
    # - code: ko
    #   name: 한국어
  # 默认系统语言（客服和AI使用的语言）
  default-system-language: zh-CN
```

### 语言代码参考

常用语言代码：
- `zh-CN` - 简体中文
- `zh-TW` - 繁体中文
- `en` - 英语
- `ja` - 日语
- `ko` - 韩语
- `fr` - 法语
- `de` - 德语
- `es` - 西班牙语
- `pt` - 葡萄牙语
- `th` - 泰语
- `vi` - 越南语

完整语言代码列表请参考：[AWS Translate 支持的语言](https://docs.aws.amazon.com/translate/latest/dg/what-is-languages.html)

## 数据库变更

### ChatSession 表新增字段

```sql
-- 为 chat_sessions 表添加 customer_language 字段
ALTER TABLE chat_sessions 
ADD COLUMN customer_language VARCHAR(10) DEFAULT NULL;

-- 添加注释
ALTER TABLE chat_sessions 
MODIFY COLUMN customer_language VARCHAR(10) COMMENT '客户使用的语言代码，如 zh-TW, en, ja';

-- 创建索引（可选）
CREATE INDEX idx_chat_sessions_customer_language ON chat_sessions(customer_language);
```

### Agent 表新增字段

```sql
-- 为 agents 表添加 language 字段
ALTER TABLE agents 
ADD COLUMN language VARCHAR(10) DEFAULT NULL;

-- 添加注释
ALTER TABLE agents 
MODIFY COLUMN language VARCHAR(10) COMMENT '客服使用的语言代码，如 zh-CN, en, ja';
```

## API 接口

### 1. 获取翻译服务配置

```http
GET /api/v1/translation/config
```

**响应示例：**
```json
{
  "enabled": true,
  "defaultSystemLanguage": "zh-CN",
  "supportedLanguages": [
    {"code": "zh-TW", "name": "中文繁体"},
    {"code": "en", "name": "English"},
    {"code": "ja", "name": "日本語"}
  ]
}
```

### 2. 获取支持的语言列表

```http
GET /api/v1/translation/languages
```

**响应示例：**
```json
[
  {"code": "zh-TW", "name": "中文繁体"},
  {"code": "en", "name": "English"},
  {"code": "ja", "name": "日本語"}
]
```

### 3. 检测文本语言

```http
POST /api/v1/translation/detect
Content-Type: application/json

{
  "text": "Hello, how can I help you?"
}
```

**响应示例：**
```json
{
  "detectedLanguage": "en"
}
```

### 4. 翻译文本

```http
POST /api/v1/translation/translate
Content-Type: application/json

{
  "text": "Hello, how can I help you?",
  "sourceLanguage": "en",  // 可选，默认自动检测
  "targetLanguage": "zh-TW"  // 必填
}
```

**响应示例：**
```json
{
  "originalText": "Hello, how can I help you?",
  "translatedText": "您好，我可以為您提供什麼幫助？",
  "sourceLanguage": "en",
  "targetLanguage": "zh-TW"
}
```

### 5. 翻译为所有目标语言

```http
POST /api/v1/translation/translate-all
Content-Type: application/json

{
  "text": "Hello, how can I help you?",
  "sourceLanguage": "en"  // 可选，默认自动检测
}
```

**响应示例：**
```json
{
  "sourceLanguage": "en",
  "originalText": "Hello, how can I help you?",
  "zh-TW": "您好，我可以為您提供什麼幫助？",
  "en": "Hello, how can I help you?",
  "ja": "こんにちは、どのようにお手伝いできますか？",
  "zh-CN": "您好，我可以为您提供什么帮助？"
}
```

## 客服语言设置

客服可以设置自己使用的语言，系统会根据客服的语言设置来显示翻译后的消息。

### 创建客服时设置语言

```http
POST /api/v1/admin/agents
Content-Type: application/json

{
  "name": "客服小王",
  "email": "wang@example.com",
  "password": "password123",
  "roleId": "xxx-xxx-xxx",
  "language": "zh-TW"  // 可选，客服使用的语言
}
```

### 更新客服语言设置

```http
PUT /api/v1/admin/agents/{agentId}
Content-Type: application/json

{
  "language": "en"  // 更新语言设置
}
```

### AgentDto 响应示例

```json
{
  "id": "xxx-xxx-xxx",
  "name": "客服小王",
  "email": "wang@example.com",
  "avatarUrl": null,
  "status": "ONLINE",
  "roleId": "xxx-xxx-xxx",
  "roleName": "客服",
  "language": "zh-TW"
}
```

## 客户语言设置

### 1. WEB 端（前端传输）

在创建会话时通过 metadata 传递 language 参数：

```http
POST /api/v1/public/quick-customer
Content-Type: application/json

{
  "name": "访客用户",
  "channel": "WEB",
  "metadata": {
    "language": "zh-TW",  // 客户使用的语言
    "categoryId": "xxx",
    "source": "website"
  }
}
```

### 2. Webhook（其他渠道）

在 Webhook 请求中携带 language 参数：

```http
POST /api/v1/external/{platformName}/webhook
Content-Type: application/json

{
  "threadId": "external-thread-123",
  "content": "Hello",
  "language": "en",  // 客户使用的语言
  "externalUserId": "user-123",
  "userName": "John"
}
```

### 3. 自动检测

如果未提供语言参数，系统会在收到客户第一条消息时自动检测语言并设置到会话中。

## 消息翻译数据结构

所有消息的 `translationData` 字段包含翻译结果：

```json
{
  "sourceLanguage": "en",
  "originalText": "Hello",
  "zh-TW": "您好",
  "zh-CN": "您好",
  "ja": "こんにちは"
}
```

## 翻译流程

### 客户消息处理流程

1. 收到客户消息
2. 检查会话是否已有 customerLanguage
3. 如果没有，尝试从请求中获取或自动检测
4. 将消息翻译为所有目标语言 + 系统默认语言
5. 保存翻译结果到 `translationData`
6. 广播消息到 WebSocket

### 客服/AI 消息处理流程

1. 客服或 AI 发送消息
2. 使用系统默认语言作为源语言
3. 将消息翻译为所有目标语言
4. 保存翻译结果到 `translationData`
5. 如果是外部平台会话，将翻译为客户语言的版本转发到外部平台

## 注意事项

1. **AWS 凭证配置**：确保正确配置 AWS Access Key 和 Secret Key
2. **区域选择**：选择离用户较近的 AWS 区域可以降低延迟
3. **费用考虑**：AWS Translate 按字符计费，大量翻译可能产生费用
4. **语言检测准确性**：短文本的语言检测可能不够准确
5. **翻译质量**：机器翻译可能存在误差，建议关键消息人工复核

## 相关文件

- 配置类：`com.example.aikef.config.TranslationConfig`
- 服务类：`com.example.aikef.service.TranslationService`
- 控制器：`com.example.aikef.controller.TranslationController`
- 数据库迁移：`db/alter_chat_sessions_add_customer_language.sql`

