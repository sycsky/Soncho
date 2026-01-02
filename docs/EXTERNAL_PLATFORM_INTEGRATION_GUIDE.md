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

---

# Shopify 接入指南（内嵌应用 + OAuth + Webhooks）

本节描述如何将本项目作为 Shopify Embedded App 接入并在测试商店中完成安装、回调、Webhook 验证与数据落库。

## 1. 关键概念

- **App URL**：Shopify Admin 内打开应用时 iframe 加载的地址（对你来说是公网可访问的前端或后端地址）。
- **Allowed redirection URL(s)**：OAuth 授权完成后，Shopify 回调到你后端的地址（必须精确匹配）。
- **Embedded App**：应用 UI 被嵌入到 Shopify Admin 中显示；外观看起来像 `admin.shopify.com/.../apps/...`，但内容实际来自你的 App URL。

## 2. 后端能力概览

- OAuth 安装与回调：
  - `GET /api/v1/shopify/oauth/install?shop={shop}.myshopify.com`（发起授权）
  - `GET /api/v1/shopify/oauth/callback`（处理回调、换 token、写库、注册 webhooks）
  - 代码参考：[ShopifyAuthController](file:///d:/ai_agent_work/ai_kef/src/main/java/com/example/aikef/shopify/controller/ShopifyAuthController.java)、[ShopifyAuthService](file:///d:/ai_agent_work/ai_kef/src/main/java/com/example/aikef/shopify/service/ShopifyAuthService.java)

- Webhooks 接收与校验（HMAC-SHA256）：
  - `POST /api/v1/shopify/webhooks/*`
  - 代码参考：[ShopifyWebhookController](file:///d:/ai_agent_work/ai_kef/src/main/java/com/example/aikef/shopify/controller/ShopifyWebhookController.java)、[ShopifyWebhookVerifier](file:///d:/ai_agent_work/ai_kef/src/main/java/com/example/aikef/shopify/service/ShopifyWebhookVerifier.java)

- GDPR Webhooks（已实现接收与校验）：
  - `POST /api/v1/shopify/gdpr/*`
  - 代码参考：[ShopifyGdprController](file:///d:/ai_agent_work/ai_kef/src/main/java/com/example/aikef/shopify/controller/ShopifyGdprController.java)

## 3. Shopify 后台配置（Dev Dashboard / Partner）

在你的 Shopify App 配置中设置：

- **App URL**（两种方式二选一）
  - 方式 A：直接指向后端安装入口（最适合纯后端阶段）
    - `https://<公网域名>/api/v1/shopify/oauth/install`
  - 方式 B：指向前端首页（前端再跳转到 `/oauth/install`）
    - `https://<公网域名>/`

- **Allowed redirection URL(s)**（必须）
  - `https://<公网域名>/api/v1/shopify/oauth/callback`

- **Preferences URL**（可选）
  - 可填 `https://<公网域名>/` 或留空

## 4. 本地配置（Spring Boot）

配置项位于：
- [application-dev.yml](file:///d:/ai_agent_work/ai_kef/src/main/resources/application-dev.yml)
- [application-local.yml](file:///d:/ai_agent_work/ai_kef/src/main/resources/application-local.yml)（本地覆盖，不提交）

最小配置示例：

```yaml
shopify:
  api-key: ${SHOPIFY_API_KEY:your_shopify_api_key}
  api-secret: ${SHOPIFY_API_SECRET:your_shopify_api_secret}
  app-url: ${SHOPIFY_APP_URL:https://<公网域名>}
  ui-url: ${SHOPIFY_UI_URL:https://<前端域名>}   # 可选
  embedded: true                                 # 默认 true
```

说明：
- `shopify.app-url` 必须是 Shopify 可以访问到的公网 HTTPS 域名（通常来自 cloudflared/ngrok）。
- `shopify.ui-url` 仅用于 callback 跳转到你自定义的前端面板（没有前端时可不配）。

## 5. 安装流程（推荐走 Shopify Admin 内嵌体验）

1) 在 Shopify Admin 中打开 App（或点击 Install）
- Shopify 会打开你的 App URL，并带上 `shop`、`host` 等参数（由 Shopify 注入）。

2) 发起 OAuth 授权
- 如果 App URL 直接指向 `/oauth/install`：后端会立刻 302 跳到 Shopify 授权页
- 如果 App URL 指向前端：前端拿到 `shop` 后跳转到：
  - `/api/v1/shopify/oauth/install?shop={shop}.myshopify.com`
  - 代码参考：[ShopifyAuthController.install](file:///d:/ai_agent_work/ai_kef/src/main/java/com/example/aikef/shopify/controller/ShopifyAuthController.java#L40-L48)

3) OAuth 回调与跳转
- 本项目 callback 默认会 302 跳转（不再返回 JSON）
- 如果参数里存在 `host` 且 `shopify.embedded=true`，会优先跳回 Shopify Admin 的 apps 页面
  - 代码参考：[ShopifyAuthController.callback](file:///d:/ai_agent_work/ai_kef/src/main/java/com/example/aikef/shopify/controller/ShopifyAuthController.java#L50-L105)

调试：在 callback URL 上加 `&format=json` 可以强制返回 JSON。

## 6. 数据库准备

当前 Shopify 安装信息会写入 `shopify_stores` 表：
- SQL 文件：[create_shopify_stores.sql](file:///d:/ai_agent_work/ai_kef/db/create_shopify_stores.sql)

本项目采用 `tenant_id VARCHAR(50)`，Shopify 的租户 ID 使用短格式（`shp_{md5}`）以避免长度问题：
- 生成逻辑参考：[ShopifyAuthService.generateTenantId](file:///d:/ai_agent_work/ai_kef/src/main/java/com/example/aikef/shopify/service/ShopifyAuthService.java#L116-L131)

## 7. Webhook 注册与限制（Protected Customer Data）

安装完成后，后端会自动尝试注册 Webhooks：
- 代码参考：[ShopifyWebhookRegistrationService](file:///d:/ai_agent_work/ai_kef/src/main/java/com/example/aikef/shopify/service/ShopifyWebhookRegistrationService.java)

其中卸载事件使用 `app/uninstalled`：

- Topic：`app/uninstalled`
- 接收地址：`POST /api/v1/shopify/webhooks/app/uninstalled`
- 行为：
  - 将 `shopify_stores.active` 置为 `false`，写入 `uninstalled_at`
  - 清理该店铺的 Redis access token 缓存，并将数据库中的 `access_token`/`scopes` 置空
  - 代码参考：[ShopifySyncService.upsertFromWebhook](file:///d:/ai_agent_work/ai_kef/src/main/java/com/example/aikef/shopify/service/ShopifySyncService.java#L26-L46)

注意：Shopify 对包含敏感客户数据的主题（如 `orders/create`、`customers/*`）需要额外权限申请，否则会返回 403。
因此当前实现会跳过以下主题的自动注册：
- `orders/create`
- `customers/create`
- `customers/update`
- `customers/data_request`
- `customers/redact`
- `shop/redact`

## 8. iframe 嵌入安全头（必需）

为保证 Shopify Admin 能 iframe 加载你的页面，本项目已放开 `frame-ancestors`：
- 配置参考：[SecurityConfig](file:///d:/ai_agent_work/ai_kef/src/main/java/com/example/aikef/config/SecurityConfig.java#L56-L78)

## 9. 测试清单（Test Store）

- 启动后端（确保 8080 未被占用）并保持公网隧道 URL 可用
- 安装/授权成功后，检查数据库：

```sql
SELECT id, shop_domain, active, tenant_id, created_at
FROM shopify_stores
ORDER BY created_at DESC
LIMIT 5;
```

- 触发一个可用 Webhook（例如修改商品标题并保存），检查是否成功接收（日志/落库取决于你的实现）

## 10. 常见问题排查

- callback 报 redirect_uri 不在白名单：检查 Shopify 后台 Allowed redirection URL(s) 与实际回调地址是否完全一致（含路径、https）。
- 403 无权限创建某些 Webhook：需要申请 Protected Customer Data 权限；开发阶段可先跳过敏感主题。
- 页面无法被 Shopify Admin 内嵌：检查响应头是否包含允许 iframe 的 `Content-Security-Policy frame-ancestors ...`（见 SecurityConfig）。
- 卸载后仍可调用 Shopify Admin API：检查 `app/uninstalled` webhook 是否创建成功，以及卸载事件是否到达本服务（可查 `shopify_webhook_events` 与 `shopify_stores.active/uninstalled_at`）。

## 11. 前端接入（App URL 指向前端）

适用场景：你在 Shopify Partner Dashboard 中把 **App URL** 配置为前端域名（例如 `https://<前端域名>/`），而 OAuth 安装与回调仍由后端完成。

说明：本仓库当前未包含可直接运行的前端工程脚手架；以下内容描述的是前端需要实现的接入行为与与后端的接口契约。

### 11.1 Shopify 后台配置要点

- **App URL**：
  - `https://<前端域名>/`
- **Allowed redirection URL(s)**：
  - `https://<后端域名>/api/v1/shopify/oauth/callback`
- 后端配置：
  - `shopify.app-url` 配为后端公网域名（用于生成 OAuth redirect_uri）
  - `shopify.ui-url` 配为前端公网域名（用于非 embedded/缺 host 的回跳兜底）

### 11.2 前端启动页：拿参数并触发安装

Shopify Admin 打开 App 时，会把关键参数拼在 App URL 上，常见为：

- `shop`：形如 `xxx.myshopify.com`
- `host`：Shopify 的 host 参数（用于 embedded 场景下的 App Bridge 初始化）

前端需要在首次渲染时做两件事：

1) 解析 `shop`（必要）与 `host`（建议保留）
2) 如果尚未完成安装/授权，则跳转到后端安装入口：
   - `GET /api/v1/shopify/oauth/install?shop={shop}`

示例（框架无关，浏览器原生）：

```js
const params = new URLSearchParams(window.location.search);
const shop = params.get('shop');
const host = params.get('host');

if (shop) {
  sessionStorage.setItem('shop', shop);
}
if (host) {
  sessionStorage.setItem('host', host);
}

if (shop && !sessionStorage.getItem('shopifyInstalled')) {
  window.location.href = `/api/v1/shopify/oauth/install?shop=${encodeURIComponent(shop)}`;
}
```

安装成功后标记一次（避免循环）：

```js
sessionStorage.setItem('shopifyInstalled', '1');
```

### 11.3 OAuth 回调后：前端如何回到应用页面

后端回调处理完成后会 302 跳转：

- embedded 场景（`shopify.embedded=true` 且存在 `host`）：优先跳回 Shopify Admin 的 apps 页面，由 Shopify 重新加载你的 App URL
  - 代码参考：[ShopifyAuthController.callback](file:///d:/ai_agent_work/ai_kef/src/main/java/com/example/aikef/shopify/controller/ShopifyAuthController.java#L50-L105)
- 非 embedded 或缺少 `host`：会跳回 `shopify.ui-url`（若配置）或 `shopify.app-url`，并附带 `shop`、`tenantId`、`host`（若有）

前端建议统一在启动时从 querystring 与 sessionStorage 兜底读取：

```js
const params = new URLSearchParams(window.location.search);
const shop = params.get('shop') || sessionStorage.getItem('shop');
const host = params.get('host') || sessionStorage.getItem('host');
const tenantId = params.get('tenantId') || sessionStorage.getItem('tenantId');

if (shop) sessionStorage.setItem('shop', shop);
if (host) sessionStorage.setItem('host', host);
if (tenantId) sessionStorage.setItem('tenantId', tenantId);
```

### 11.4 登录与登录态判断（本项目现状）

本项目对前端受保护接口使用的是后端签发的坐席 Token（以及客户 Token）。在 Shopify Embedded App 场景下，推荐用 Shopify Session Token 换取后端坐席 Token（避免让商家再输入邮箱/密码）。

- 换取后端 Token（推荐）：
  - `POST /api/v1/shopify/auth/exchange`
  - Header：`Authorization: Bearer <shopify_session_token>`
  - 或使用 query：`?id_token=<jwt>&shop=<shop>.myshopify.com`（兼容从 URL 带 token 的场景）
  - 返回：`{ success, shop, tenantId, token }`（其中 `token` 为后端坐席 Token）
  - 代码参考：[ShopifyEmbeddedAuthController.exchange](file:///d:/ai_agent_work/ai_kef/src/main/java/com/example/aikef/shopify/controller/ShopifyEmbeddedAuthController.java#L52-L110)
  - 说明：
    - `shop` / `host` 等 URL 参数用于定位店铺与初始化 App Bridge，但不应直接作为“已登录”凭证
    - Session Token（JWT）由 Shopify 签名；后端会校验签名与 `aud`（等于 `shopify.api-key`），并检查 `exp`

- 登录：
  - `POST /api/v1/auth/login`
  - 返回：`{ token, agent }`
  - 代码参考：[AuthController.login](file:///d:/ai_agent_work/ai_kef/src/main/java/com/example/aikef/controller/AuthController.java#L35-L42)、[LoginResponse](file:///d:/ai_agent_work/ai_kef/src/main/java/com/example/aikef/dto/response/LoginResponse.java)
- 携带 Token 调用受保护接口：
  - `Authorization: Bearer <token>`
  - 代码参考：[UnifiedAuthenticationFilter](file:///d:/ai_agent_work/ai_kef/src/main/java/com/example/aikef/security/UnifiedAuthenticationFilter.java#L32-L73)
- 判断登录态：
  - `GET /api/v1/auth/me` 成功返回当前用户信息即为已登录
  - 代码参考：[AuthController.me](file:///d:/ai_agent_work/ai_kef/src/main/java/com/example/aikef/controller/AuthController.java#L44-L47)

前端通用请求封装示例：

```js
async function apiFetch(path, options = {}) {
  const token = localStorage.getItem('token');
  const tenantId = sessionStorage.getItem('tenantId');
  const headers = new Headers(options.headers || {});
  headers.set('Content-Type', 'application/json');
  if (token) headers.set('Authorization', `Bearer ${token}`);
  if (tenantId) headers.set('X-Tenant-ID', tenantId);
  const resp = await fetch(path, { ...options, headers });
  if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
  return resp.json();
}
```

### 11.5 租户透传（SaaS 模式）

当 `app.saas.enabled=true` 时，后端需要明确的租户上下文。当前实现支持三种来源（按优先级）：

1) Token 中携带的 tenantId
2) 请求头 `X-Tenant-ID`
3) 查询参数 `tenantId`

代码参考：[TenantInterceptor](file:///d:/ai_agent_work/ai_kef/src/main/java/com/example/aikef/saas/interceptor/TenantInterceptor.java#L20-L88)

前端建议：

- 如回跳 querystring 带有 `tenantId`，则持久化并在后续 API 请求中通过 `X-Tenant-ID` 传递
- 如未带 `tenantId`，可在非 SaaS 模式下先跑通页面；SaaS 模式需要补齐 tenantId 的获取与持久化策略
