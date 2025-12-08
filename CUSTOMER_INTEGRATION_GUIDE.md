# 客户端 WebSocket 接入指南

本文档详细说明客户如何通过各种渠道（Web、微信、WhatsApp、Line 等）接入 AI 客服系统。

## 目录
1. [概述](#概述)
2. [快速开始](#快速开始)
3. [获取客户 Token](#获取客户-token)
4. [连接 WebSocket](#连接-websocket)
5. [发送和接收消息](#发送和接收消息)
6. [不同渠道接入示例](#不同渠道接入示例)
7. [客户管理 API](#客户管理-api)
8. [常见问题](#常见问题)

---

## 概述

### 支持的渠道

| 渠道 | Channel 值 | 说明 |
|------|-----------|------|
| 网页 | `WEB` | 网页聊天窗口 |
| 微信 | `WECHAT` | 微信公众号/小程序 |
| WhatsApp | `WHATSAPP` | WhatsApp Business |
| Line | `LINE` | Line 官方账号 |
| Telegram | `TELEGRAM` | Telegram Bot |
| Facebook | `FACEBOOK` | Facebook Messenger |
| 邮件 | `EMAIL` | 邮件客服 |
| 短信 | `SMS` | 短信客服 |
| 电话 | `PHONE` | 电话客服 |
| 移动应用 | `APP` | 原生移动应用 |

### 认证流程

```
客户端                          服务端
  |                              |
  |-- 1. 请求 Token ------------->|
  |   (name, channel, channelId) |
  |                              |-- 查找/创建客户
  |<-- 2. 返回 Token -------------|
  |                              |
  |-- 3. 连接 WebSocket --------->|
  |   (携带 Token)               |
  |                              |-- 验证 Token
  |<-- 4. 连接成功 --------------|
  |                              |
  |<--> 5. 收发消息 <------------>|
```

---

## 快速开始

### 步骤 1: 获取客户 Token

首先调用公开 API 获取客户 Token：

```bash
curl -X POST http://127.0.0.1:8080/api/v1/public/customer-token \
  -H "Content-Type: application/json" \
  -d '{
    "name": "张三",
    "channel": "WEB",
    "channelId": "web_user_123456"
  }'
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "customerId": "550e8400-e29b-41d4-a716-446655440000",
    "token": "cust_a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "name": "张三",
    "channel": "WEB"
  }
}
```

### 步骤 2: 连接 WebSocket

使用返回的 `token` 连接 WebSocket：

```javascript
const token = "cust_a1b2c3d4-e5f6-7890-abcd-ef1234567890";
const ws = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${token}`);

ws.onopen = () => {
  console.log('已连接到客服');
};

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('收到消息:', message);
};
```

### 步骤 3: 发送消息

```javascript
ws.send(JSON.stringify({
  conversationId: "session-123",  // 可选，首次发送可留空
  senderId: "web_user_123456",
  content: "你好，我需要帮助",
  metadata: {}
}));
```

---

## 获取客户 Token

### API 端点
```
POST /api/v1/public/customer-token
```

### 请求参数

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 客户姓名 |
| channel | string | 是 | 渠道类型（见上方渠道列表） |
| channelId | string | 是 | 渠道唯一标识 |

### Channel ID 说明

不同渠道的 `channelId` 含义：

| 渠道 | channelId 示例 | 说明 |
|------|---------------|------|
| WEB | `web_user_12345` | 浏览器 Cookie/Session ID |
| WECHAT | `oAbCd1234567890` | 微信 OpenID |
| WHATSAPP | `+8613800138000` | WhatsApp 手机号 |
| LINE | `Uabcdef123456` | Line User ID |
| TELEGRAM | `123456789` | Telegram User ID |
| FACEBOOK | `1234567890123456` | Facebook PSID |
| EMAIL | `user@example.com` | 邮箱地址 |
| PHONE/SMS | `+8613800138000` | 手机号 |
| APP | `app_user_uuid` | 应用内用户ID |

### 响应格式

```typescript
interface CustomerTokenResponse {
  customerId: string;    // 客户 UUID
  token: string;         // WebSocket 连接 Token
  name: string;          // 客户姓名
  channel: string;       // 渠道名称
}
```

### 特性说明

- **自动创建客户**: 如果客户不存在，系统会自动创建
- **去重机制**: 同一 `channelId` 多次请求会返回同一客户
- **无需注册**: 客户无需密码，直接通过渠道标识获取 Token

---

## 连接 WebSocket

### WebSocket 端点
```
ws://127.0.0.1:8080/ws/chat?token={customer-token}
```

### 连接示例

#### JavaScript (浏览器)

```javascript
class CustomerWebSocket {
  constructor(token) {
    this.token = token;
    this.ws = null;
  }

  connect() {
    this.ws = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${this.token}`);

    this.ws.onopen = () => {
      console.log('✅ 已连接到客服');
      this.onConnected();
    };

    this.ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      this.onMessage(message);
    };

    this.ws.onerror = (error) => {
      console.error('❌ 连接错误:', error);
      this.onError(error);
    };

    this.ws.onclose = () => {
      console.log('🔌 连接已关闭');
      this.onDisconnected();
    };
  }

  sendMessage(content) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({
        senderId: 'current-user-id',
        content: content,
        metadata: {}
      }));
    }
  }

  disconnect() {
    if (this.ws) {
      this.ws.close();
    }
  }

  // 钩子函数（由外部实现）
  onConnected() {}
  onMessage(message) {}
  onError(error) {}
  onDisconnected() {}
}

// 使用示例
const client = new CustomerWebSocket('cust_xxxx');
client.onMessage = (message) => {
  console.log('客服回复:', message.content);
};
client.connect();
```

#### React Hook

```typescript
import { useEffect, useRef, useState } from 'react';

export function useCustomerWebSocket(token: string) {
  const ws = useRef<WebSocket | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [messages, setMessages] = useState<any[]>([]);

  useEffect(() => {
    if (!token) return;

    const socket = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${token}`);

    socket.onopen = () => {
      console.log('已连接到客服');
      setIsConnected(true);
    };

    socket.onmessage = (event) => {
      const message = JSON.parse(event.data);
      setMessages((prev) => [...prev, message]);
    };

    socket.onclose = () => {
      console.log('连接已关闭');
      setIsConnected(false);
    };

    ws.current = socket;

    return () => {
      socket.close();
    };
  }, [token]);

  const sendMessage = (content: string) => {
    if (ws.current && ws.current.readyState === WebSocket.OPEN) {
      ws.current.send(JSON.stringify({
        senderId: 'user-id',
        content,
        metadata: {}
      }));
    }
  };

  return { isConnected, messages, sendMessage };
}
```

---

## 发送和接收消息

### 消息格式

#### 客户端发送消息

```json
{
  "conversationId": "session-uuid",  // 可选，首次可不传
  "senderId": "channel-user-id",
  "content": "消息内容",
  "metadata": {
    "type": "text",
    "attachments": []
  }
}
```

#### 服务端返回消息

```json
{
  "channel": "WEB",
  "conversationId": "session-uuid",
  "senderId": "agent-id",
  "content": "客服回复内容",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 消息示例

```javascript
// 发送文本消息
ws.send(JSON.stringify({
  senderId: 'web_user_123',
  content: '我想咨询产品价格'
}));

// 发送带元数据的消息
ws.send(JSON.stringify({
  senderId: 'web_user_123',
  content: '这是我的订单号',
  metadata: {
    orderId: 'ORDER-12345',
    orderStatus: 'pending'
  }
}));
```

---

## 不同渠道接入示例

### 1. Web 网页聊天

```html
<!DOCTYPE html>
<html>
<head>
  <title>在线客服</title>
</head>
<body>
  <div id="chat-container">
    <div id="messages"></div>
    <input type="text" id="message-input" placeholder="输入消息...">
    <button onclick="sendMessage()">发送</button>
  </div>

  <script>
    let ws;
    let customerId;

    // 初始化
    async function init() {
      // 1. 获取 Token
      const response = await fetch('http://127.0.0.1:8080/api/v1/public/customer-token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: '访客_' + Date.now(),
          channel: 'WEB',
          channelId: 'web_' + generateUUID()
        })
      });

      const { data } = await response.json();
      customerId = data.customerId;

      // 2. 连接 WebSocket
      ws = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${data.token}`);

      ws.onmessage = (event) => {
        const message = JSON.parse(event.data);
        displayMessage(message.content, 'agent');
      };
    }

    function sendMessage() {
      const input = document.getElementById('message-input');
      const content = input.value.trim();
      
      if (content && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({
          senderId: customerId,
          content: content
        }));
        
        displayMessage(content, 'user');
        input.value = '';
      }
    }

    function displayMessage(content, sender) {
      const messagesDiv = document.getElementById('messages');
      const messageEl = document.createElement('div');
      messageEl.className = sender;
      messageEl.textContent = content;
      messagesDiv.appendChild(messageEl);
    }

    function generateUUID() {
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
        const r = Math.random() * 16 | 0;
        return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
      });
    }

    init();
  </script>
</body>
</html>
```

### 2. 微信小程序

```javascript
// pages/chat/chat.js
Page({
  data: {
    messages: [],
    inputValue: '',
    customerId: '',
    token: ''
  },

  onLoad() {
    this.getCustomerToken();
  },

  async getCustomerToken() {
    const res = await wx.request({
      url: 'https://your-api.com/api/v1/public/customer-token',
      method: 'POST',
      data: {
        name: '微信用户',
        channel: 'WECHAT',
        channelId: wx.getStorageSync('openid') // 微信 OpenID
      }
    });

    this.setData({
      customerId: res.data.data.customerId,
      token: res.data.data.token
    });

    this.connectWebSocket();
  },

  connectWebSocket() {
    const socketTask = wx.connectSocket({
      url: `wss://your-api.com/ws/chat?token=${this.data.token}`
    });

    socketTask.onMessage((res) => {
      const message = JSON.parse(res.data);
      this.addMessage(message.content, 'agent');
    });

    this.socketTask = socketTask;
  },

  sendMessage() {
    const content = this.data.inputValue.trim();
    if (!content) return;

    this.socketTask.send({
      data: JSON.stringify({
        senderId: this.data.customerId,
        content: content
      })
    });

    this.addMessage(content, 'user');
    this.setData({ inputValue: '' });
  }
});
```

### 3. WhatsApp Business (Node.js)

```javascript
const axios = require('axios');
const WebSocket = require('ws');

class WhatsAppCustomerService {
  constructor(phone) {
    this.phone = phone;
    this.token = null;
    this.ws = null;
  }

  async init() {
    // 获取客户 Token
    const response = await axios.post('http://127.0.0.1:8080/api/v1/public/customer-token', {
      name: 'WhatsApp User',
      channel: 'WHATSAPP',
      channelId: this.phone
    });

    this.token = response.data.data.token;
    this.connectWebSocket();
  }

  connectWebSocket() {
    this.ws = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${this.token}`);

    this.ws.on('open', () => {
      console.log('Connected to customer service');
    });

    this.ws.on('message', (data) => {
      const message = JSON.parse(data);
      console.log('Agent replied:', message.content);
      // 发送回 WhatsApp 用户
      this.sendWhatsAppMessage(this.phone, message.content);
    });
  }

  sendMessage(content) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({
        senderId: this.phone,
        content: content
      }));
    }
  }

  sendWhatsAppMessage(phone, content) {
    // 调用 WhatsApp Business API 发送消息
    // 实现略
  }
}

// 使用示例
const customer = new WhatsAppCustomerService('+8613800138000');
customer.init();
```

---

## 客户管理 API

以下 API 供坐席管理客户信息使用，需要坐席 Token 认证。

### 查询客户列表

```http
GET /api/v1/customers?name=张三&channel=WEB&active=true
Authorization: Bearer {agent-token}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "name": "张三",
        "primaryChannel": "WEB",
        "email": "zhangsan@example.com",
        "phone": "+8613800138000",
        "tags": ["VIP", "已购买"],
        "active": true,
        "lastInteractionAt": "2024-01-15T10:30:00Z"
      }
    ],
    "totalElements": 100,
    "totalPages": 5
  }
}
```

### 获取客户详情

```http
GET /api/v1/customers/{customerId}
Authorization: Bearer {agent-token}
```

### 创建客户

```http
POST /api/v1/customers
Authorization: Bearer {agent-token}
Content-Type: application/json

{
  "name": "李四",
  "primaryChannel": "WECHAT",
  "wechatOpenId": "oAbCd1234567890",
  "phone": "+8613900139000",
  "tags": ["潜在客户"],
  "notes": "对产品A感兴趣"
}
```

### 更新客户信息

```http
PUT /api/v1/customers/{customerId}
Authorization: Bearer {agent-token}
Content-Type: application/json

{
  "name": "李四（已购买）",
  "tags": ["VIP", "已购买"],
  "notes": "已购买产品A，满意度高"
}
```

### 为客户生成 Token

```http
POST /api/v1/customers/{customerId}/token
Authorization: Bearer {agent-token}
```

---

## 常见问题

### Q1: channelId 应该使用什么值？

**A**: `channelId` 是客户在该渠道的唯一标识：
- Web: 浏览器生成的 UUID 或 Cookie ID
- 微信: OpenID
- WhatsApp: 手机号
- Email: 邮箱地址
- 其他: 各平台提供的用户唯一标识

### Q2: Token 会过期吗？

**A**: 当前实现的 Token 不会过期（存储在内存中）。生产环境建议：
- 使用 Redis 存储，设置过期时间（如 24 小时）
- 客户端检测连接断开后重新获取 Token

### Q3: 同一客户多次获取 Token 会怎样？

**A**: 系统会为同一 `channelId` 返回同一客户，但生成新的 Token。旧 Token 依然有效。

### Q4: 如何区分客户和坐席？

**A**: 通过 Token 前缀区分：
- 客户 Token: `cust_xxxxxxxx`
- 坐席 Token: 普通 UUID 格式

### Q5: 客户可以主动断开连接吗？

**A**: 可以，调用 `ws.close()` 即可。

### Q6: 如何实现断线重连？

**A**: 
```javascript
function connectWithRetry(token, maxRetries = 5) {
  let retries = 0;

  function connect() {
    const ws = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${token}`);

    ws.onclose = () => {
      if (retries < maxRetries) {
        retries++;
        console.log(`重连中... (${retries}/${maxRetries})`);
        setTimeout(connect, 3000);
      }
    };

    return ws;
  }

  return connect();
}
```

### Q7: 客户信息会自动更新吗？

**A**: `lastInteractionAt`（最后交互时间）会在客户发送消息时自动更新。其他信息需要通过管理 API 手动更新。

### Q8: 支持群聊吗？

**A**: 当前系统主要面向一对一客服场景。如需群聊功能，需要额外开发。

---

## 技术支持

如有问题，请联系技术支持：
- 邮箱: support@example.com
- 电话: 400-xxx-xxxx

---

**文档版本**: v1.0  
**最后更新**: 2024-01-15
