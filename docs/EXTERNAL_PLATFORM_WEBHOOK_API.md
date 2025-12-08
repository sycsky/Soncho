# 第三方平台 Webhook 接入指南

## 概述

本系统支持接收来自 Line、WhatsApp、微信等第三方平台的消息，并将客服/AI 回复消息转发回第三方平台。

## 架构流程

```
┌─────────────────┐     Webhook      ┌─────────────────┐
│  第三方平台      │ ──────────────> │   客服系统       │
│  (Line/微信等)   │                 │                 │
│                 │ <────────────── │  AI + 人工客服   │
└─────────────────┘   消息转发回调   └─────────────────┘
```

### 消息流向

1. **客户消息接收**：第三方平台 → Webhook 接口 → 创建/关联会话 → 保存消息 → 触发 AI 工作流
2. **回复消息转发**：AI/客服回复 → 保存消息 → 异步转发到第三方平台回调 URL

---

## API 接口

### 1. 接收第三方平台消息

**接口地址**：`POST /api/v1/webhook/{platformName}/message`

**请求示例**：
```bash
POST /api/v1/webhook/line/message
Content-Type: application/json

{
  "threadId": "U1234567890abcdef",
  "content": "你好，我想咨询一下订单问题",
  "externalUserId": "U1234567890abcdef",
  "userName": "张三",
  "email": "zhangsan@example.com",
  "phone": "13800138000",
  "messageType": "text",
  "categoryId": "uuid-of-category",
  "timestamp": 1702345678000,
  "metadata": {
    "replyToken": "xxx",
    "source": "line_official"
  }
}
```

**请求参数说明**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| threadId | string | ✅ | 第三方平台的会话/线程 ID，用于标识和关联会话 |
| content | string | ✅ | 消息内容 |
| externalUserId | string | ❌ | 外部用户 ID（如微信 openId、Line userId） |
| userName | string | ❌ | 用户名称 |
| email | string | ❌ | 用户邮箱（用于创建/匹配客户） |
| phone | string | ❌ | 用户手机号（用于创建/匹配客户） |
| messageType | string | ❌ | 消息类型：text, image, file, audio, video（默认 text） |
| categoryId | string | ❌ | 会话分类 ID（UUID 格式） |
| attachmentUrl | string | ❌ | 附件 URL（用于图片/文件消息） |
| attachmentName | string | ❌ | 附件名称 |
| timestamp | number | ❌ | 消息时间戳（毫秒） |
| metadata | object | ❌ | 额外元数据 |

**响应示例**：
```json
{
  "success": true,
  "messageId": "550e8400-e29b-41d4-a716-446655440000",
  "sessionId": "660e8400-e29b-41d4-a716-446655440001",
  "customerId": "770e8400-e29b-41d4-a716-446655440002",
  "newSession": true
}
```

### 2. Webhook 验证接口

某些平台（如微信）需要先验证 Webhook URL。

**接口地址**：`GET /api/v1/webhook/{platformName}/verify`

**请求参数**：
- `echostr`：微信验证字符串
- `signature`：签名
- `timestamp`：时间戳
- `nonce`：随机数

---

## 平台管理接口

### 1. 获取所有平台配置

```bash
GET /api/v1/webhook/platforms
```

### 2. 创建平台配置

```bash
POST /api/v1/webhook/platforms
Content-Type: application/json

{
  "name": "line",
  "displayName": "Line Official Account",
  "platformType": "LINE",
  "callbackUrl": "https://api.line.me/v2/bot/message/push",
  "authType": "BEARER_TOKEN",
  "authCredential": "your-channel-access-token",
  "webhookSecret": "your-webhook-secret",
  "enabled": true,
  "remark": "Line 官方账号"
}
```

**平台类型 (platformType)**：
- `LINE` - Line
- `WHATSAPP` - WhatsApp
- `WECHAT` - 微信
- `TELEGRAM` - Telegram
- `FACEBOOK` - Facebook Messenger
- `CUSTOM` - 自定义平台

**认证类型 (authType)**：
- `NONE` - 无认证
- `API_KEY` - API Key（Header: X-API-Key）
- `BEARER_TOKEN` - Bearer Token
- `BASIC_AUTH` - Basic Auth（格式：username:password）
- `CUSTOM_HEADER` - 自定义 Header（格式：Header-Name:value）

### 3. 更新平台配置

```bash
PUT /api/v1/webhook/platforms/{platformId}
Content-Type: application/json

{
  "callbackUrl": "https://new-callback-url.com",
  "authCredential": "new-token",
  "enabled": true
}
```

### 4. 获取单个平台配置

```bash
GET /api/v1/webhook/platforms/{platformName}
```

---

## 消息转发机制

当系统中的 AI 或客服发送消息时，会自动转发到第三方平台的 `callbackUrl`。

### 转发请求格式

系统会向配置的 `callbackUrl` 发送 POST 请求：

```json
{
  "threadId": "U1234567890abcdef",
  "content": "您好，您的订单已发货...",
  "senderType": "AI",
  "timestamp": 1702345678000,
  "externalUserId": "U1234567890abcdef"
}
```

**senderType 值**：
- `AI` - AI 回复
- `AGENT` - 客服回复
- `SYSTEM` - 系统消息

### 转发认证

根据平台配置的 `authType`，系统会自动添加认证请求头：

- **API_KEY**：`X-API-Key: {authCredential}`
- **BEARER_TOKEN**：`Authorization: Bearer {authCredential}`
- **BASIC_AUTH**：`Authorization: Basic {base64(username:password)}`
- **CUSTOM_HEADER**：`{headerName}: {headerValue}`

---

## 集成示例

### Line 集成

1. **配置平台**：
```bash
POST /api/v1/webhook/platforms
{
  "name": "line_official",
  "displayName": "Line 官方账号",
  "platformType": "LINE",
  "callbackUrl": "https://api.line.me/v2/bot/message/push",
  "authType": "BEARER_TOKEN",
  "authCredential": "YOUR_CHANNEL_ACCESS_TOKEN",
  "enabled": true
}
```

2. **在 Line 开发者后台配置 Webhook URL**：
```
https://your-domain.com/api/v1/webhook/line_official/message
```

3. **Line 服务器转发消息示例**：
```javascript
// Line webhook handler
app.post('/line-webhook', async (req, res) => {
  const events = req.body.events;
  
  for (const event of events) {
    if (event.type === 'message') {
      await fetch('https://your-domain.com/api/v1/webhook/line_official/message', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          threadId: event.source.userId,
          content: event.message.text,
          externalUserId: event.source.userId,
          userName: event.source.userId,
          messageType: event.message.type,
          timestamp: event.timestamp,
          metadata: {
            replyToken: event.replyToken
          }
        })
      });
    }
  }
  
  res.status(200).send('OK');
});
```

### 微信公众号集成

1. **配置平台**：
```bash
POST /api/v1/webhook/platforms
{
  "name": "wechat_official",
  "displayName": "微信公众号",
  "platformType": "WECHAT",
  "callbackUrl": "https://your-wechat-proxy.com/send",
  "authType": "API_KEY",
  "authCredential": "YOUR_API_KEY",
  "webhookSecret": "YOUR_WECHAT_TOKEN",
  "enabled": true
}
```

2. **微信消息转发示例**：
```python
# 微信消息处理
@app.route('/wechat', methods=['POST'])
def wechat_webhook():
    xml_data = request.data
    msg = parse_wechat_message(xml_data)
    
    if msg['MsgType'] == 'text':
        response = requests.post(
            'https://your-domain.com/api/v1/webhook/wechat_official/message',
            json={
                'threadId': msg['FromUserName'],
                'content': msg['Content'],
                'externalUserId': msg['FromUserName'],
                'messageType': 'text',
                'timestamp': int(msg['CreateTime']) * 1000
            }
        )
    
    return 'success'
```

---

## 数据库表结构

### external_platforms（第三方平台配置）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 主键 |
| name | VARCHAR(50) | 平台名称（唯一） |
| display_name | VARCHAR(100) | 显示名称 |
| platform_type | ENUM | 平台类型 |
| callback_url | VARCHAR(500) | 消息转发回调 URL |
| auth_type | ENUM | 认证类型 |
| auth_credential | VARCHAR(500) | 认证凭据 |
| extra_headers | TEXT | 额外请求头（JSON） |
| webhook_secret | VARCHAR(200) | Webhook 验证密钥 |
| enabled | BOOLEAN | 是否启用 |

### external_session_mappings（会话映射）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 主键 |
| platform_id | UUID | 关联平台 ID |
| external_thread_id | VARCHAR(200) | 外部线程 ID |
| session_id | UUID | 系统会话 ID |
| customer_id | UUID | 客户 ID |
| external_user_id | VARCHAR(200) | 外部用户 ID |
| external_user_name | VARCHAR(200) | 外部用户名称 |
| active | BOOLEAN | 是否活跃 |

---

## 注意事项

1. **threadId 唯一性**：每个平台的 `threadId` 必须唯一，用于关联会话
2. **消息幂等**：建议在 `metadata` 中传递消息 ID，避免重复处理
3. **错误重试**：消息转发失败时，系统会记录日志但不会重试，建议在第三方集成层实现重试机制
4. **安全验证**：生产环境建议配置 `webhookSecret` 并验证请求签名

