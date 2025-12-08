# 聊天系统接入文档

本文档详细说明如何接入 AI 客服系统的聊天功能，包括客服端和客户端的完整接入流程。

## 📋 目录

- [系统架构](#系统架构)
- [客服端接入](#客服端接入)
  - [1. 客服登录认证](#1-客服登录认证)
  - [2. 连接 WebSocket](#2-连接-websocket-客服端)
  - [3. 收发消息](#3-收发消息-客服端)
  - [4. 拉取历史消息](#4-拉取历史消息-客服端)
  - [5. 查看隐藏标识](#5-查看隐藏标识)
- [客户端接入](#客户端接入)
  - [1. 创建客户 Token](#1-创建客户-token)
  - [2. 连接 WebSocket](#2-连接-websocket-客户端)
  - [3. 收发消息](#3-收发消息-客户端)
  - [4. 拉取历史消息](#4-拉取历史消息-客户端)
- [消息格式规范](#消息格式规范)
- [完整代码示例](#完整代码示例)
- [常见问题](#常见问题)

---

## 系统架构

### 核心特性

✅ **群组聊天模式** - 每个客户自动创建独立群组（1客户 + 1主责客服 + N支持客服）  
✅ **智能客服分配** - 支持多种分配策略（随机、技能匹配、负载均衡等）  
✅ **双重认证** - 同时支持客服 Token 和客户 Token  
✅ **权限隔离** - 仅群组成员可访问会话消息  
✅ **隐藏标识** - agentMetadata 字段仅客服可见  
✅ **消息归属** - 自动区分本人和他人发送的消息  

### 工作流程

```
┌─────────────┐                    ┌─────────────┐
│  客户端      │                    │  客服端      │
└──────┬──────┘                    └──────┬──────┘
       │                                  │
       │ 1. 请求 Token                     │ 1. 登录获取 Token
       ├──────────────────────────────────┤
       │ 2. 创建群组+分配客服                │
       │ 3. 返回 token+sessionId           │
       │                                  │
       │ 4. 连接 WebSocket                 │ 4. 连接 WebSocket
       ├──────────────────────────────────┤
       │                                  │
       │ 5. 发送消息 ──────────────────────>│ 接收消息
       │                                  │
       │ 接收消息 <──────────────────────── │ 6. 发送消息
       │                                  │
       │ 7. 获取历史消息                    │ 7. 获取历史消息（含隐藏信息）
       │    (agentMetadata=null)          │    (agentMetadata 可见)
       │                                  │
```

---

## 客服端接入

### 1. 客服登录认证

客服需要先通过登录接口获取访问令牌。

#### API 端点

```
POST /api/v1/auth/login
```

#### 请求示例

```json
{
  "email": "agent@example.com",
  "password": "your-password"
}
```

#### 响应示例

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "agent": {
    "id": "agent-uuid",
    "email": "agent@example.com",
    "name": "客服小王",
    "role": "AGENT",
    "status": "ONLINE",
    "createdAt": "2024-01-15T10:00:00Z"
  }
}
```

#### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `token` | string | 访问令牌，后续所有请求需携带此 Token |
| `agent.id` | string | 客服唯一标识 |
| `agent.name` | string | 客服姓名 |
| `agent.status` | string | 客服状态：ONLINE, OFFLINE, BUSY 等 |

#### 代码示例

```javascript
async function agentLogin(email, password) {
  const response = await fetch('http://127.0.0.1:8080/api/v1/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ email, password })
  });
  
  const { token, agent } = await response.json();
  
  // 保存 Token
  localStorage.setItem('agent_token', token);
  localStorage.setItem('agent_id', agent.id);
  
  return { token, agent };
}

// 使用示例
const { token, agent } = await agentLogin('agent@example.com', 'password');
console.log('登录成功:', agent.name);
```

---

### 2. 连接 WebSocket (客服端)

登录后使用 Token 连接 WebSocket 服务。

#### WebSocket 端点

```
ws://127.0.0.1:8080/ws/chat?token={agent-token}
```

#### 认证方式

⚠️ **重要**: Token 必须通过 URL 查询参数传递，不支持 Header 方式。

#### 连接示例

```javascript
const token = localStorage.getItem('agent_token');
const ws = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${token}`);

ws.onopen = () => {
  console.log('✅ WebSocket 连接成功');
};

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('📩 收到消息:', message);
  handleIncomingMessage(message);
};

ws.onerror = (error) => {
  console.error('❌ WebSocket 错误:', error);
};

ws.onclose = (event) => {
  console.log('🔌 WebSocket 连接关闭:', event.code);
  // 可以实现自动重连逻辑
};
```

---

### 3. 收发消息 (客服端)

#### 发送消息格式

```json
{
  "sessionId": "会话ID",
  "senderId": "客服ID",
  "content": "消息内容",
  "metadata": {
    "type": "text"
  }
}
```

#### 发送代码示例

```javascript
function sendMessage(sessionId, content, metadata = {}) {
  const agentId = localStorage.getItem('agent_id');
  
  const message = {
    sessionId: sessionId,
    senderId: agentId,
    content: content,
    metadata: metadata
  };
  
  if (ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(message));
    console.log('📤 消息已发送:', content);
  } else {
    console.error('WebSocket 未连接');
  }
}

// 使用示例
sendMessage('session-uuid-123', '您好，请问有什么可以帮您？');
```

#### 接收消息格式

```json
{
  "channel": "WEB",
  "conversationId": "会话ID",
  "senderId": "发送者ID",
  "content": "消息内容",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### 接收代码示例

```javascript
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  
  // 判断消息类型
  if (message.event) {
    // 事件消息（如订阅确认、状态变更等）
    handleEventMessage(message);
  } else if (message.content) {
    // 聊天消息
    displayMessage({
      sender: message.senderId,
      content: message.content,
      time: new Date(message.timestamp),
      isMe: message.senderId === localStorage.getItem('agent_id')
    });
  }
};

function displayMessage(msg) {
  console.log(`[${msg.time.toLocaleTimeString()}] ${msg.sender}: ${msg.content}`);
  // 在 UI 中显示消息
}
```

---

### 4. 拉取历史消息 (客服端)

#### API 端点

```
GET /api/v1/chat/sessions/{sessionId}/messages
```

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `sessionId` | string | 是 | 会话 ID（路径参数） |
| `page` | number | 否 | 页码，从 0 开始，默认 0 |
| `size` | number | 否 | 每页数量，默认 50 |
| `sort` | string | 否 | 排序规则，默认 `createdAt,asc` |

#### 请求示例

```javascript
async function getSessionMessages(sessionId, page = 0, size = 50) {
  const token = localStorage.getItem('agent_token');
  
  const url = new URL(`http://127.0.0.1:8080/api/v1/chat/sessions/${sessionId}/messages`);
  url.searchParams.append('page', page);
  url.searchParams.append('size', size);
  url.searchParams.append('sort', 'createdAt,asc');
  
  const response = await fetch(url, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  const result = await response.json();
  return result.data; // { content: [], totalElements, totalPages, ... }
}

// 使用示例
const messages = await getSessionMessages('session-uuid-123');
console.log('历史消息:', messages.content);
console.log('消息总数:', messages.totalElements);
```

#### 响应示例

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "msg-uuid-1",
        "sessionId": "session-uuid",
        "senderType": "AGENT",
        "agentId": "agent-uuid",
        "agentName": "客服小王",
        "text": "您好，请问有什么可以帮您？",
        "internal": false,
        "isMine": true,
        "translationData": {},
        "mentionAgentIds": [],
        "attachments": [],
        "agentMetadata": {
          "priority": "high",
          "tags": ["VIP客户"],
          "notes": "需要特别关注"
        },
        "createdAt": "2024-01-15T10:30:00Z"
      },
      {
        "id": "msg-uuid-2",
        "sessionId": "session-uuid",
        "senderType": "USER",
        "agentId": null,
        "agentName": null,
        "text": "我想咨询产品价格",
        "internal": false,
        "isMine": false,
        "translationData": {},
        "mentionAgentIds": [],
        "attachments": [],
        "agentMetadata": null,
        "createdAt": "2024-01-15T10:31:00Z"
      }
    ],
    "totalElements": 25,
    "totalPages": 1,
    "size": 50,
    "number": 0,
    "first": true,
    "last": true
  }
}
```

#### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `isMine` | boolean | 是否是本人发送的消息 |
| `senderType` | string | 发送者类型：USER（客户）或 AGENT（客服） |
| `agentMetadata` | object | **仅客服可见的隐藏信息**，客户看不到 |
| `internal` | boolean | 是否是内部消息（客户看不到） |

---

### 5. 查看隐藏标识

客服可以看到消息中的 `agentMetadata` 字段，这是仅客服之间可见的隐藏信息。

#### agentMetadata 用途

- 客服间协作备注
- 客户标签和优先级
- 内部处理状态
- 敏感信息标记
- 工单关联信息

#### agentMetadata 示例

```json
{
  "agentMetadata": {
    "priority": "high",              // 优先级
    "customerType": "VIP",            // 客户类型
    "tags": ["投诉", "退款"],        // 内部标签
    "assignedTo": "张三",             // 指派给
    "notes": "客户情绪激动，需耐心处理",
    "relatedTicket": "TICKET-123",    // 关联工单
    "sentiment": "negative"           // 情绪分析
  }
}
```

#### 代码示例

```javascript
function displayAgentMessage(message) {
  console.log('消息内容:', message.text);
  console.log('是否本人发送:', message.isMine);
  
  // 客服可以看到隐藏信息
  if (message.agentMetadata) {
    console.log('🔒 内部信息:');
    console.log('  优先级:', message.agentMetadata.priority);
    console.log('  客户标签:', message.agentMetadata.tags);
    console.log('  备注:', message.agentMetadata.notes);
    
    // 在 UI 中显示隐藏信息标识
    if (message.agentMetadata.priority === 'high') {
      addHighPriorityFlag(message.id);
    }
  }
}
```

---

## 客户端接入

### 1. 创建客户 Token

客户端首次连接时需要获取访问令牌，系统会自动创建客户信息、会话和群组，并分配客服。

#### API 端点

```
POST /api/v1/public/customer-token
```

⚠️ **注意**: 此接口无需认证，属于公开接口。

#### 请求示例

```json
{
  "name": "张三",
  "channel": "WEB",
  "email": "zhangsan@example.com",
  "phone": "+8613800138000",
  "channelUserId": "wx_openid_123"
}
```

#### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | 是 | 客户姓名 |
| `channel` | string | 是 | 渠道类型：WEB, WECHAT, APP, PHONE 等 |
| `email` | string | 否 | 客户邮箱（用于去重） |
| `phone` | string | 否 | 客户手机号（用于去重） |
| `channelUserId` | string | 否 | 渠道用户 ID，如微信 OpenID（优先用于去重） |

#### 客户去重逻辑

系统会根据以下优先级查找已有客户：
1. **channelUserId** + channel（优先级最高）
2. **email** + channel
3. **phone** + channel

如果找到已存在的客户，会更新客户信息并返回；否则创建新客户。

#### 响应示例

```json
{
  "success": true,
  "data": {
    "customerId": "customer-uuid",
    "token": "cust_eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "name": "张三",
    "channel": "WEB",
    "sessionId": "session-uuid",
    "groupId": "group-uuid"
  }
}
```

#### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `customerId` | string | 客户唯一标识 |
| `token` | string | 客户访问令牌（以 `cust_` 开头） |
| `sessionId` | string | **会话 ID**，用于发送消息和获取历史 |
| `groupId` | string | 群组 ID |

#### 自动执行的操作

调用此接口后，系统会自动：
1. ✅ 查找或创建客户
2. ✅ 创建聊天群组
3. ✅ 分配主责客服（基于配置的分配策略）
4. ✅ 创建聊天会话
5. ✅ 生成客户 Token

#### 代码示例

```javascript
async function createCustomerToken(name, channel, options = {}) {
  const response = await fetch('http://127.0.0.1:8080/api/v1/public/customer-token', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      name,
      channel,
      ...options // email, phone, channelUserId
    })
  });
  
  const result = await response.json();
  const { customerId, token, sessionId, groupId } = result.data;
  
  // 保存 Token 和会话信息
  localStorage.setItem('customer_token', token);
  localStorage.setItem('customer_id', customerId);
  localStorage.setItem('session_id', sessionId);
  
  return result.data;
}

// 使用示例 1: 基本用法（仅姓名和渠道）
const customerData = await createCustomerToken('张三', 'WEB');

// 使用示例 2: 提供邮箱（推荐）
const customerData2 = await createCustomerToken('李四', 'WEB', {
  email: 'lisi@example.com'
});

// 使用示例 3: 微信小程序（提供 OpenID）
const customerData3 = await createCustomerToken('王五', 'WECHAT', {
  channelUserId: 'oX1234567890abcdef'
});

console.log('客户 Token:', customerData.token);
console.log('会话 ID:', customerData.sessionId);
```

---

### 2. 连接 WebSocket (客户端)

获取 Token 后连接 WebSocket 服务。

#### WebSocket 端点

```
ws://127.0.0.1:8080/ws/chat?token={customer-token}
```

#### 连接示例

```javascript
const token = localStorage.getItem('customer_token');
const ws = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${token}`);

ws.onopen = () => {
  console.log('✅ 已连接到客服');
  // 连接成功后可以获取历史消息
  loadHistoryMessages();
};

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('📩 收到客服消息:', message);
  displayCustomerMessage(message);
};

ws.onerror = (error) => {
  console.error('❌ 连接错误:', error);
};

ws.onclose = (event) => {
  console.log('🔌 连接已断开');
  // 实现自动重连
  setTimeout(() => {
    console.log('尝试重连...');
    connectWebSocket();
  }, 3000);
};
```

---

### 3. 收发消息 (客户端)

#### 发送消息格式

```json
{
  "sessionId": "会话ID",
  "senderId": "客户ID",
  "content": "消息内容",
  "metadata": {}
}
```

#### 发送代码示例

```javascript
function sendCustomerMessage(content) {
  const sessionId = localStorage.getItem('session_id');
  const customerId = localStorage.getItem('customer_id');
  
  const message = {
    sessionId: sessionId,
    senderId: customerId,
    content: content,
    metadata: {}
  };
  
  if (ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(message));
    
    // 在 UI 中显示自己发送的消息
    displayMessage({
      sender: '我',
      content: content,
      time: new Date(),
      isMe: true
    });
  } else {
    console.error('未连接到服务器');
  }
}

// 使用示例
sendCustomerMessage('你好，我想咨询产品价格');
```

#### 接收消息示例

```javascript
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  
  if (message.content) {
    displayMessage({
      sender: '客服',
      content: message.content,
      time: new Date(message.timestamp),
      isMe: false
    });
  }
};

function displayMessage(msg) {
  const messageDiv = document.createElement('div');
  messageDiv.className = msg.isMe ? 'message-me' : 'message-other';
  messageDiv.innerHTML = `
    <div class="sender">${msg.sender}</div>
    <div class="content">${msg.content}</div>
    <div class="time">${msg.time.toLocaleTimeString()}</div>
  `;
  document.getElementById('chat-messages').appendChild(messageDiv);
  
  // 滚动到底部
  messageDiv.scrollIntoView({ behavior: 'smooth' });
}
```

---

### 4. 拉取历史消息 (客户端)

#### API 端点

```
GET /api/v1/chat/sessions/{sessionId}/messages
```

#### 请求示例

```javascript
async function loadHistoryMessages(page = 0, size = 50) {
  const sessionId = localStorage.getItem('session_id');
  const token = localStorage.getItem('customer_token');
  
  const url = new URL(`http://127.0.0.1:8080/api/v1/chat/sessions/${sessionId}/messages`);
  url.searchParams.append('page', page);
  url.searchParams.append('size', size);
  url.searchParams.append('sort', 'createdAt,asc');
  
  const response = await fetch(url, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  const result = await response.json();
  const messages = result.data.content;
  
  // 显示历史消息
  messages.forEach(msg => {
    displayMessage({
      sender: msg.isMine ? '我' : '客服',
      content: msg.text,
      time: new Date(msg.createdAt),
      isMe: msg.isMine
    });
  });
  
  return result.data;
}

// 使用示例
await loadHistoryMessages();
```

#### 响应示例（客户视角）

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "msg-uuid-1",
        "sessionId": "session-uuid",
        "senderType": "AGENT",
        "agentId": "agent-uuid",
        "agentName": "客服小王",
        "text": "您好，请问有什么可以帮您？",
        "internal": false,
        "isMine": false,
        "agentMetadata": null,
        "createdAt": "2024-01-15T10:30:00Z"
      },
      {
        "id": "msg-uuid-2",
        "sessionId": "session-uuid",
        "senderType": "USER",
        "agentId": null,
        "agentName": null,
        "text": "我想咨询产品价格",
        "internal": false,
        "isMine": true,
        "agentMetadata": null,
        "createdAt": "2024-01-15T10:31:00Z"
      }
    ],
    "totalElements": 2
  }
}
```

⚠️ **注意**: 客户调用时，`agentMetadata` 字段始终为 `null`，无法看到客服的隐藏标识。

---

## 消息格式规范

### WebSocket 消息类型

系统支持两种类型的 WebSocket 消息：

#### 1. 聊天消息（Chat Message）

用于发送和接收聊天内容。

**客户端发送**:
```json
{
  "sessionId": "session-uuid",
  "senderId": "sender-uuid",
  "content": "消息内容",
  "metadata": {
    "type": "text"
  }
}
```

**服务端响应**:
```json
{
  "channel": "WEB",
  "conversationId": "session-uuid",
  "senderId": "sender-uuid",
  "content": "消息内容",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### 2. 事件消息（Event Message）

用于订阅、状态变更等控制操作。

**客户端发送**:
```json
{
  "event": "事件名称",
  "payload": {
    // 事件数据
  }
}
```

**服务端响应**:
```json
{
  "type": "事件类型",
  "data": {
    // 响应数据
  }
}
```

### 常用事件列表

| 事件名称 | 说明 | Payload 示例 |
|---------|------|-------------|
| `subscribe` | 订阅会话更新 | `{ "sessionId": "uuid" }` |
| `unsubscribe` | 取消订阅 | `{ "sessionId": "uuid" }` |
| `typing` | 正在输入状态 | `{ "sessionId": "uuid" }` |
| `status_change` | 客服状态变更 | `{ "status": "ONLINE" }` |

### 发送事件示例

```javascript
// 订阅会话
ws.send(JSON.stringify({
  event: 'subscribe',
  payload: { sessionId: 'session-uuid' }
}));

// 发送正在输入状态
ws.send(JSON.stringify({
  event: 'typing',
  payload: { sessionId: 'session-uuid' }
}));
```

---

## 完整代码示例

### 客服端完整示例（JavaScript）

```javascript
class AgentChatClient {
  constructor() {
    this.ws = null;
    this.token = null;
    this.agentId = null;
  }
  
  // 登录
  async login(email, password) {
    const response = await fetch('http://127.0.0.1:8080/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    
    const { token, agent } = await response.json();
    this.token = token;
    this.agentId = agent.id;
    
    localStorage.setItem('agent_token', token);
    localStorage.setItem('agent_id', agent.id);
    
    console.log('✅ 客服登录成功:', agent.name);
    return agent;
  }
  
  // 连接 WebSocket
  connect() {
    this.ws = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${this.token}`);
    
    this.ws.onopen = () => {
      console.log('✅ WebSocket 连接成功');
      this.onConnected();
    };
    
    this.ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      this.handleMessage(message);
    };
    
    this.ws.onerror = (error) => {
      console.error('❌ WebSocket 错误:', error);
    };
    
    this.ws.onclose = () => {
      console.log('🔌 WebSocket 连接关闭');
      this.reconnect();
    };
  }
  
  // 发送消息
  sendMessage(sessionId, content) {
    const message = {
      sessionId,
      senderId: this.agentId,
      content,
      metadata: {}
    };
    
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
      console.log('📤 消息已发送');
    }
  }
  
  // 获取历史消息
  async getMessages(sessionId, page = 0) {
    const url = new URL(`http://127.0.0.1:8080/api/v1/chat/sessions/${sessionId}/messages`);
    url.searchParams.append('page', page);
    url.searchParams.append('size', 50);
    
    const response = await fetch(url, {
      headers: { 'Authorization': `Bearer ${this.token}` }
    });
    
    const result = await response.json();
    return result.data.content;
  }
  
  // 处理消息
  handleMessage(message) {
    if (message.event) {
      console.log('📩 事件:', message);
    } else if (message.content) {
      console.log('💬 消息:', message.content);
      
      // 显示消息，包括隐藏信息
      this.displayMessage(message);
    }
  }
  
  displayMessage(message) {
    console.log(`[${new Date(message.timestamp).toLocaleTimeString()}] ${message.content}`);
    
    // 客服可以看到 agentMetadata
    if (message.agentMetadata) {
      console.log('🔒 内部信息:', message.agentMetadata);
    }
  }
  
  // 重连
  reconnect() {
    setTimeout(() => {
      console.log('🔄 尝试重连...');
      this.connect();
    }, 3000);
  }
  
  // 钩子函数
  onConnected() {
    // 连接成功后的处理
  }
}

// 使用示例
const agentClient = new AgentChatClient();

// 登录并连接
await agentClient.login('agent@example.com', 'password');
agentClient.connect();

// 发送消息
agentClient.sendMessage('session-uuid', '您好，请问有什么可以帮您？');

// 获取历史消息
const messages = await agentClient.getMessages('session-uuid');
console.log('历史消息:', messages);
```

---

### 客户端完整示例（JavaScript）

```javascript
class CustomerChatClient {
  constructor() {
    this.ws = null;
    this.token = null;
    this.customerId = null;
    this.sessionId = null;
  }
  
  // 初始化（获取 Token）
  async initialize(name, channel, options = {}) {
    const response = await fetch('http://127.0.0.1:8080/api/v1/public/customer-token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, channel, ...options })
    });
    
    const result = await response.json();
    const { customerId, token, sessionId } = result.data;
    
    this.token = token;
    this.customerId = customerId;
    this.sessionId = sessionId;
    
    localStorage.setItem('customer_token', token);
    localStorage.setItem('customer_id', customerId);
    localStorage.setItem('session_id', sessionId);
    
    console.log('✅ 客户 Token 创建成功');
    console.log('会话 ID:', sessionId);
    
    return result.data;
  }
  
  // 连接 WebSocket
  connect() {
    this.ws = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${this.token}`);
    
    this.ws.onopen = () => {
      console.log('✅ 已连接到客服');
      this.onConnected();
    };
    
    this.ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      this.handleMessage(message);
    };
    
    this.ws.onerror = (error) => {
      console.error('❌ 连接错误:', error);
    };
    
    this.ws.onclose = () => {
      console.log('🔌 连接已断开');
      this.reconnect();
    };
  }
  
  // 发送消息
  sendMessage(content) {
    const message = {
      sessionId: this.sessionId,
      senderId: this.customerId,
      content,
      metadata: {}
    };
    
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
      console.log('📤 消息已发送');
      
      // 显示自己的消息
      this.displayMessage({
        sender: '我',
        content,
        time: new Date(),
        isMe: true
      });
    }
  }
  
  // 获取历史消息
  async getMessages(page = 0) {
    const url = new URL(`http://127.0.0.1:8080/api/v1/chat/sessions/${this.sessionId}/messages`);
    url.searchParams.append('page', page);
    url.searchParams.append('size', 50);
    
    const response = await fetch(url, {
      headers: { 'Authorization': `Bearer ${this.token}` }
    });
    
    const result = await response.json();
    return result.data.content;
  }
  
  // 处理消息
  handleMessage(message) {
    if (message.content) {
      this.displayMessage({
        sender: '客服',
        content: message.content,
        time: new Date(message.timestamp),
        isMe: false
      });
    }
  }
  
  displayMessage(msg) {
    console.log(`[${msg.time.toLocaleTimeString()}] ${msg.sender}: ${msg.content}`);
    // 在 UI 中显示消息
  }
  
  // 重连
  reconnect() {
    setTimeout(() => {
      console.log('🔄 尝试重连...');
      this.connect();
    }, 3000);
  }
  
  // 钩子函数
  async onConnected() {
    // 连接成功后加载历史消息
    const messages = await this.getMessages();
    messages.forEach(msg => {
      this.displayMessage({
        sender: msg.isMine ? '我' : '客服',
        content: msg.text,
        time: new Date(msg.createdAt),
        isMe: msg.isMine
      });
    });
  }
}

// 使用示例
const customerClient = new CustomerChatClient();

// 初始化并连接
await customerClient.initialize('张三', 'WEB', {
  email: 'zhangsan@example.com'
});
customerClient.connect();

// 发送消息
customerClient.sendMessage('你好，我想咨询产品价格');

// 获取历史消息
const messages = await customerClient.getMessages();
console.log('历史消息:', messages);
```

---

### React Hook 示例

```typescript
import { useEffect, useRef, useState } from 'react';

interface Message {
  id: string;
  content: string;
  sender: string;
  time: Date;
  isMe: boolean;
}

export function useCustomerChat(name: string, channel: string) {
  const ws = useRef<WebSocket | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);

  // 初始化
  useEffect(() => {
    async function init() {
      const response = await fetch('http://127.0.0.1:8080/api/v1/public/customer-token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, channel })
      });
      
      const result = await response.json();
      setToken(result.data.token);
      setSessionId(result.data.sessionId);
    }
    
    init();
  }, [name, channel]);

  // 连接 WebSocket
  useEffect(() => {
    if (!token) return;

    const socket = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${token}`);

    socket.onopen = () => {
      console.log('✅ WebSocket 连接成功');
      setIsConnected(true);
      loadHistory();
    };

    socket.onmessage = (event) => {
      const message = JSON.parse(event.data);
      if (message.content) {
        setMessages(prev => [...prev, {
          id: Math.random().toString(),
          content: message.content,
          sender: '客服',
          time: new Date(message.timestamp),
          isMe: false
        }]);
      }
    };

    socket.onclose = () => {
      setIsConnected(false);
    };

    ws.current = socket;

    return () => {
      socket.close();
    };
  }, [token]);

  // 加载历史消息
  const loadHistory = async () => {
    if (!sessionId || !token) return;

    const response = await fetch(
      `http://127.0.0.1:8080/api/v1/chat/sessions/${sessionId}/messages`,
      { headers: { 'Authorization': `Bearer ${token}` } }
    );

    const result = await response.json();
    const history = result.data.content.map((msg: any) => ({
      id: msg.id,
      content: msg.text,
      sender: msg.isMine ? '我' : '客服',
      time: new Date(msg.createdAt),
      isMe: msg.isMine
    }));

    setMessages(history);
  };

  // 发送消息
  const sendMessage = (content: string) => {
    if (ws.current && ws.current.readyState === WebSocket.OPEN && sessionId) {
      const customerId = localStorage.getItem('customer_id');
      
      ws.current.send(JSON.stringify({
        sessionId,
        senderId: customerId,
        content,
        metadata: {}
      }));

      setMessages(prev => [...prev, {
        id: Math.random().toString(),
        content,
        sender: '我',
        time: new Date(),
        isMe: true
      }]);
    }
  };

  return { isConnected, messages, sendMessage };
}

// 组件中使用
function ChatComponent() {
  const { isConnected, messages, sendMessage } = useCustomerChat('张三', 'WEB');
  const [input, setInput] = useState('');

  const handleSend = () => {
    if (input.trim()) {
      sendMessage(input);
      setInput('');
    }
  };

  return (
    <div className="chat-container">
      <div className="status">
        {isConnected ? '✅ 已连接' : '⏳ 连接中...'}
      </div>
      
      <div className="messages">
        {messages.map(msg => (
          <div key={msg.id} className={msg.isMe ? 'message-me' : 'message-other'}>
            <div className="sender">{msg.sender}</div>
            <div className="content">{msg.content}</div>
            <div className="time">{msg.time.toLocaleTimeString()}</div>
          </div>
        ))}
      </div>
      
      <div className="input-area">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyPress={(e) => e.key === 'Enter' && handleSend()}
          placeholder="输入消息..."
        />
        <button onClick={handleSend}>发送</button>
      </div>
    </div>
  );
}
```

---

## 常见问题

### Q1: Token 有效期是多久？

**A**: 
- 客服 Token: 默认 7 天
- 客户 Token: 默认 30 天

Token 过期后需要重新获取。

### Q2: WebSocket 连接失败怎么办？

**A**: 
1. 确认 Token 是否有效（未过期）
2. 检查 Token 是否通过 URL 参数传递
3. 确认服务端已启动
4. 查看浏览器控制台错误信息
5. 检查网络连接和防火墙设置

### Q3: 如何判断消息是否发送成功？

**A**: 
- WebSocket 发送是异步的，没有直接的发送确认
- 可以监听服务端的响应消息或实现消息回执机制
- 建议在 UI 中先显示"发送中"状态，收到服务端推送后更新为"已发送"

### Q4: 客户可以看到 agentMetadata 吗？

**A**: 
**不可以**。`agentMetadata` 字段仅客服可见，客户调用历史消息接口时，该字段始终返回 `null`。

### Q5: 如何实现断线重连？

**A**: 
监听 `onclose` 事件，使用 `setTimeout` 延迟重连：

```javascript
ws.onclose = () => {
  console.log('连接断开，3秒后重连');
  setTimeout(() => {
    connect();
  }, 3000);
};
```

建议设置最大重连次数，避免无限重连。

### Q6: 客户 Token 和客服 Token 有什么区别？

**A**: 

| 特性 | 客户 Token | 客服 Token |
|------|-----------|-----------|
| 前缀 | `cust_` | 无特殊前缀 |
| 获取方式 | `/public/customer-token` | `/auth/login` |
| 权限 | 仅能访问自己的会话 | 可访问分配的所有会话 |
| agentMetadata | 看不到 | 可见 |
| 有效期 | 30 天 | 7 天 |

### Q7: 如何支持多客服协作？

**A**: 
系统自动支持多客服模式（1主责 + N支持），通过分配策略实现：

```java
public List<Agent> assignSupportAgents(...) {
    // 返回支持客服列表
    return List.of(agent1, agent2);
}
```

所有群组成员都能收发消息和查看历史。

### Q8: 如何切换客服分配策略？

**A**: 
在 Spring 配置中注入不同的策略实现：

```java
@Configuration
public class AssignmentConfig {
    @Bean
    @Primary
    public AgentAssignmentStrategy agentAssignmentStrategy() {
        return new SkillBasedAssignmentStrategy(); // 技能匹配
        // return new LoadBalanceAssignmentStrategy(); // 负载均衡
        // return new RandomAgentAssignmentStrategy(); // 随机分配
    }
}
```

### Q9: 消息的 isMine 字段如何判断？

**A**: 
系统自动判断：
- **客服**: 比较 `message.agentId` 和当前客服 ID
- **客户**: 比较 `message.session.customerId` 和当前客户 ID

前端直接使用 `isMine` 字段即可。

### Q10: 如何处理文件上传？

**A**: 
当前版本支持文本消息，文件上传功能正在开发中。临时方案：
1. 先上传文件到云存储
2. 获取文件 URL
3. 在消息的 `metadata` 中包含文件信息

```javascript
sendMessage('文件已上传', {
  type: 'file',
  fileUrl: 'https://example.com/file.pdf',
  fileName: 'document.pdf',
  fileSize: 102400
});
```

---

## 附录

### 支持的渠道类型

| 渠道 | 说明 |
|------|------|
| `WEB` | 网页端 |
| `WECHAT` | 微信 |
| `WECOM` | 企业微信 |
| `APP` | 移动应用 |
| `PHONE` | 电话 |
| `EMAIL` | 邮件 |
| `SMS` | 短信 |
| `WHATSAPP` | WhatsApp |
| `FACEBOOK` | Facebook |
| `TWITTER` | Twitter（X） |

### API 基础地址

- **开发环境**: `http://127.0.0.1:8080`
- **生产环境**: `https://your-domain.com`

### WebSocket 端点

- **开发环境**: `ws://127.0.0.1:8080/ws/chat`
- **生产环境**: `wss://your-domain.com/ws/chat`（建议使用 WSS）

---

**文档版本**: v1.0  
**最后更新**: 2024-01-15  
**维护团队**: AI 客服开发组

如有疑问，请联系技术支持团队。
