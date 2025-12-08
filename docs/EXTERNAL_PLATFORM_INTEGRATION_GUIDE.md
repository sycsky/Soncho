# 第三方平台接入指南

## 概述

本文档面向需要将外部消息平台（如 Line、WhatsApp、微信、Telegram 等）与客服系统集成的开发者。

通过本指南，您可以实现：
- 将第三方平台的客户消息转发到客服系统
- 将客服/AI 的回复消息推送回第三方平台

---

## 接入架构

```
┌──────────────────┐                    ┌──────────────────┐
│                  │   1. 客户消息       │                  │
│   第三方平台      │ ───────────────>   │    客服系统       │
│  (Line/微信等)   │                    │                  │
│                  │   2. 回复消息       │  AI + 人工客服   │
│                  │ <───────────────   │                  │
└──────────────────┘                    └──────────────────┘
        │                                       │
        │                                       │
        ▼                                       ▼
┌──────────────────┐                    ┌──────────────────┐
│   您的中间服务    │ <────────────────> │   Webhook 接口    │
│  (消息转发代理)   │                    │                  │
└──────────────────┘                    └──────────────────┘
```

---

## 快速开始

### 步骤 1：创建平台配置

首先在客服系统中创建您的平台配置：

```bash
POST /api/v1/webhook/platforms
Content-Type: application/json

{
  "name": "my_line_bot",
  "displayName": "我的 Line 机器人",
  "platformType": "LINE",
  "callbackUrl": "https://your-server.com/receive-reply",
  "authType": "BEARER_TOKEN",
  "authCredential": "your-api-token",
  "enabled": true
}
```

**响应**：
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "my_line_bot",
  "displayName": "我的 Line 机器人",
  "platformType": "LINE",
  "callbackUrl": "https://your-server.com/receive-reply",
  "authType": "BEARER_TOKEN",
  "enabled": true,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

### 步骤 2：转发客户消息到客服系统

当您的平台收到客户消息时，调用客服系统的 Webhook 接口：

```bash
POST /api/v1/webhook/{platformName}/message
Content-Type: application/json

{
  "threadId": "user_123456",
  "content": "你好，我想咨询一下",
  "externalUserId": "user_123456",
  "userName": "张三"
}
```

### 步骤 3：接收客服系统的回复

客服系统会将回复消息 POST 到您配置的 `callbackUrl`：

```json
{
  "threadId": "user_123456",
  "content": "您好！请问有什么可以帮您？",
  "senderType": "AI",
  "timestamp": 1702345678000,
  "externalUserId": "user_123456"
}
```

---

## API 详细说明

### 1. 创建平台配置

**接口**：`POST /api/v1/webhook/platforms`

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | ✅ | 平台唯一标识（英文，用于 URL 路径） |
| displayName | string | ❌ | 显示名称 |
| platformType | string | ✅ | 平台类型（见下表） |
| callbackUrl | string | ❌ | 回复消息回调 URL |
| authType | string | ❌ | 认证类型（见下表） |
| authCredential | string | ❌ | 认证凭据 |
| webhookSecret | string | ❌ | Webhook 签名密钥 |
| extraHeaders | string | ❌ | 额外请求头（JSON 格式） |
| enabled | boolean | ❌ | 是否启用（默认 true） |

**平台类型 (platformType)**：

可通过接口获取：`GET /api/v1/webhook/platform-types`

```json
[
  { "value": "LINE", "label": "Line" },
  { "value": "WHATSAPP", "label": "WhatsApp" },
  { "value": "WECHAT", "label": "微信" },
  { "value": "TELEGRAM", "label": "Telegram" },
  { "value": "FACEBOOK", "label": "Facebook Messenger" },
  { "value": "WEB", "label": "网页" },
  { "value": "CUSTOM", "label": "自定义平台" }
]
```

**认证类型 (authType)**：

可通过接口获取：`GET /api/v1/webhook/auth-types`

```json
[
  { "value": "NONE", "description": "无认证" },
  { "value": "API_KEY", "description": "API Key (X-API-Key 请求头)" },
  { "value": "BEARER_TOKEN", "description": "Bearer Token" },
  { "value": "BASIC_AUTH", "description": "Basic 认证" },
  { "value": "CUSTOM_HEADER", "description": "自定义请求头" }
]
```

| authType | 请求头格式 |
|----------|-----------|
| NONE | - |
| API_KEY | `X-API-Key: {credential}` |
| BEARER_TOKEN | `Authorization: Bearer {credential}` |
| BASIC_AUTH | `Authorization: Basic {base64(username:password)}` |
| CUSTOM_HEADER | `{headerName}: {headerValue}` |

---

### 2. 转发客户消息

**接口**：`POST /api/v1/webhook/{platformName}/message`

**路径参数**：
- `platformName`：您创建的平台名称（如 `my_line_bot`）

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| threadId | string | ✅ | **会话标识**，相同 threadId 的消息会归入同一会话 |
| content | string | ✅ | 消息内容 |
| externalUserId | string | ❌ | 外部用户 ID（建议提供，用于客户识别） |
| userName | string | ❌ | 用户名称 |
| email | string | ❌ | 用户邮箱 |
| phone | string | ❌ | 用户手机号 |
| messageType | string | ❌ | 消息类型：text/image/file/audio/video |
| categoryId | string | ❌ | 会话分类 ID（UUID 格式） |
| attachmentUrl | string | ❌ | 附件 URL |
| attachmentName | string | ❌ | 附件名称 |
| timestamp | number | ❌ | 消息时间戳（毫秒） |
| metadata | object | ❌ | 自定义元数据 |

**响应**：

```json
{
  "success": true,
  "messageId": "msg-uuid",
  "sessionId": "session-uuid",
  "customerId": "customer-uuid",
  "newSession": true
}
```

| 字段 | 说明 |
|------|------|
| success | 是否成功 |
| messageId | 消息 ID |
| sessionId | 会话 ID |
| customerId | 客户 ID |
| newSession | 是否为新创建的会话 |

---

### 3. 接收回复消息（您需要实现）

客服系统会向您配置的 `callbackUrl` 发送 POST 请求：

**请求头**：
- 根据您配置的 `authType` 自动添加认证头
- `Content-Type: application/json`

**请求体**：

```json
{
  "threadId": "user_123456",
  "content": "您好！请问有什么可以帮您？",
  "senderType": "AI",
  "timestamp": 1702345678000,
  "externalUserId": "user_123456"
}
```

| 字段 | 说明 |
|------|------|
| threadId | 原始会话标识 |
| content | 回复内容 |
| senderType | 发送者类型：AI / AGENT / SYSTEM |
| timestamp | 时间戳（毫秒） |
| externalUserId | 外部用户 ID |

**您需要实现的接口示例**：

```javascript
// Node.js Express 示例
app.post('/receive-reply', (req, res) => {
  const { threadId, content, senderType } = req.body;
  
  // 将消息推送到您的平台
  sendMessageToUser(threadId, content);
  
  res.status(200).json({ success: true });
});
```

---

## 完整集成示例

### Line 集成示例

```javascript
const express = require('express');
const axios = require('axios');
const app = express();

const KEFU_API = 'https://your-kefu-system.com';
const PLATFORM_NAME = 'my_line_bot';
const LINE_ACCESS_TOKEN = 'your-line-access-token';

// 1. 接收 Line 消息，转发到客服系统
app.post('/line-webhook', async (req, res) => {
  const events = req.body.events;
  
  for (const event of events) {
    if (event.type === 'message' && event.message.type === 'text') {
      try {
        await axios.post(`${KEFU_API}/api/v1/webhook/${PLATFORM_NAME}/message`, {
          threadId: event.source.userId,
          content: event.message.text,
          externalUserId: event.source.userId,
          messageType: 'text',
          timestamp: event.timestamp,
          metadata: {
            replyToken: event.replyToken,
            sourceType: event.source.type
          }
        });
      } catch (error) {
        console.error('转发消息失败:', error.message);
      }
    }
  }
  
  res.status(200).send('OK');
});

// 2. 接收客服系统回复，推送到 Line
app.post('/receive-reply', async (req, res) => {
  const { threadId, content, senderType } = req.body;
  
  try {
    // 使用 Line Push API 发送消息
    await axios.post('https://api.line.me/v2/bot/message/push', {
      to: threadId,
      messages: [{
        type: 'text',
        text: content
      }]
    }, {
      headers: {
        'Authorization': `Bearer ${LINE_ACCESS_TOKEN}`,
        'Content-Type': 'application/json'
      }
    });
    
    res.json({ success: true });
  } catch (error) {
    console.error('推送到 Line 失败:', error.message);
    res.status(500).json({ success: false, error: error.message });
  }
});

app.listen(3000);
```

### 微信公众号集成示例

```python
from flask import Flask, request
import requests
import hashlib
import xml.etree.ElementTree as ET

app = Flask(__name__)

KEFU_API = 'https://your-kefu-system.com'
PLATFORM_NAME = 'my_wechat'
WECHAT_TOKEN = 'your-wechat-token'

# 1. 微信 Webhook 验证
@app.route('/wechat', methods=['GET'])
def verify():
    signature = request.args.get('signature')
    timestamp = request.args.get('timestamp')
    nonce = request.args.get('nonce')
    echostr = request.args.get('echostr')
    
    # 验证签名
    tmp_list = sorted([WECHAT_TOKEN, timestamp, nonce])
    tmp_str = ''.join(tmp_list)
    if hashlib.sha1(tmp_str.encode()).hexdigest() == signature:
        return echostr
    return 'Invalid signature'

# 2. 接收微信消息，转发到客服系统
@app.route('/wechat', methods=['POST'])
def receive_message():
    xml_data = request.data
    root = ET.fromstring(xml_data)
    
    msg_type = root.find('MsgType').text
    if msg_type == 'text':
        from_user = root.find('FromUserName').text
        content = root.find('Content').text
        
        # 转发到客服系统
        requests.post(
            f'{KEFU_API}/api/v1/webhook/{PLATFORM_NAME}/message',
            json={
                'threadId': from_user,
                'content': content,
                'externalUserId': from_user,
                'messageType': 'text'
            }
        )
    
    return 'success'

# 3. 接收客服系统回复，推送到微信
@app.route('/receive-reply', methods=['POST'])
def receive_reply():
    data = request.json
    thread_id = data['threadId']
    content = data['content']
    
    # 使用微信客服消息接口推送
    # 注意：需要先获取 access_token
    send_wechat_message(thread_id, content)
    
    return {'success': True}

if __name__ == '__main__':
    app.run(port=3000)
```

### WhatsApp Business 集成示例

```javascript
const express = require('express');
const axios = require('axios');
const app = express();

const KEFU_API = 'https://your-kefu-system.com';
const PLATFORM_NAME = 'my_whatsapp';
const WHATSAPP_TOKEN = 'your-whatsapp-token';
const PHONE_NUMBER_ID = 'your-phone-number-id';

// 1. WhatsApp Webhook 验证
app.get('/whatsapp-webhook', (req, res) => {
  const mode = req.query['hub.mode'];
  const token = req.query['hub.verify_token'];
  const challenge = req.query['hub.challenge'];
  
  if (mode === 'subscribe' && token === 'your-verify-token') {
    res.status(200).send(challenge);
  } else {
    res.sendStatus(403);
  }
});

// 2. 接收 WhatsApp 消息，转发到客服系统
app.post('/whatsapp-webhook', async (req, res) => {
  const entry = req.body.entry?.[0];
  const changes = entry?.changes?.[0];
  const message = changes?.value?.messages?.[0];
  
  if (message) {
    const from = message.from;
    const text = message.text?.body || '';
    
    try {
      await axios.post(`${KEFU_API}/api/v1/webhook/${PLATFORM_NAME}/message`, {
        threadId: from,
        content: text,
        externalUserId: from,
        phone: from,
        messageType: message.type,
        timestamp: parseInt(message.timestamp) * 1000
      });
    } catch (error) {
      console.error('转发消息失败:', error.message);
    }
  }
  
  res.sendStatus(200);
});

// 3. 接收客服系统回复，推送到 WhatsApp
app.post('/receive-reply', async (req, res) => {
  const { threadId, content } = req.body;
  
  try {
    await axios.post(
      `https://graph.facebook.com/v17.0/${PHONE_NUMBER_ID}/messages`,
      {
        messaging_product: 'whatsapp',
        to: threadId,
        type: 'text',
        text: { body: content }
      },
      {
        headers: {
          'Authorization': `Bearer ${WHATSAPP_TOKEN}`,
          'Content-Type': 'application/json'
        }
      }
    );
    
    res.json({ success: true });
  } catch (error) {
    console.error('推送到 WhatsApp 失败:', error.message);
    res.status(500).json({ success: false });
  }
});

app.listen(3000);
```

---

## threadId 设计建议

`threadId` 是关联会话的关键，建议：

| 平台 | 推荐 threadId |
|------|---------------|
| Line | userId（用户 ID） |
| WhatsApp | 用户手机号 |
| 微信 | OpenID |
| Telegram | chat_id |
| 自定义 | 确保同一用户的 threadId 一致 |

**注意事项**：
- 相同 `threadId` 的消息会归入同一会话
- 不同 `threadId` 会创建新会话
- 建议使用用户唯一标识作为 `threadId`

---

## 错误处理

### 常见错误码

| HTTP 状态码 | 说明 | 处理建议 |
|------------|------|---------|
| 400 | 请求参数错误 | 检查必填字段 |
| 404 | 平台不存在 | 检查 platformName 是否正确 |
| 500 | 服务器错误 | 稍后重试 |

### 错误响应格式

```json
{
  "success": false,
  "messageId": null,
  "sessionId": null,
  "customerId": null,
  "newSession": false,
  "errorMessage": "平台不存在或未启用: xxx"
}
```

---

## 最佳实践

### 1. 消息去重
建议在 `metadata` 中传递原始消息 ID，避免重复处理：

```json
{
  "threadId": "user_123",
  "content": "你好",
  "metadata": {
    "originalMessageId": "msg_abc123"
  }
}
```

### 2. 错误重试
回调接口建议实现幂等性，客服系统可能会重试失败的请求。

### 3. 超时设置
- 转发消息到客服系统：建议 10 秒超时
- 接收回调请求：建议 5 秒内响应

### 4. 日志记录
记录所有消息的发送和接收，便于问题排查：

```javascript
console.log(`[${new Date().toISOString()}] 转发消息: threadId=${threadId}, content=${content.substring(0, 50)}`);
```

---

## 常见问题

### Q: 如何测试 Webhook？

可以使用 curl 命令测试：

```bash
curl -X POST https://your-kefu-system.com/api/v1/webhook/my_platform/message \
  -H "Content-Type: application/json" \
  -d '{
    "threadId": "test_user_001",
    "content": "测试消息",
    "externalUserId": "test_user_001",
    "userName": "测试用户"
  }'
```

### Q: 回调 URL 必须是 HTTPS 吗？

生产环境建议使用 HTTPS，开发测试可以使用 HTTP。

### Q: 如何更新平台配置？

```bash
PUT /api/v1/webhook/platforms/{platformId}
Content-Type: application/json

{
  "callbackUrl": "https://new-url.com/receive-reply",
  "enabled": true
}
```

### Q: 如何禁用某个平台？

```bash
PUT /api/v1/webhook/platforms/{platformId}
Content-Type: application/json

{
  "enabled": false
}
```

---

## 技术支持

如有问题，请联系技术支持或查阅完整 API 文档。

