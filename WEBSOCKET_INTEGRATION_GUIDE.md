# WebSocket 接入指南

本文档详细说明如何接入 AI 客服系统的 WebSocket 服务，实现实时消息推送和双向通信。

## 目录
1. [概述](#概述)
2. [连接方式](#连接方式)
3. [认证机制](#认证机制)
4. [消息格式](#消息格式)
5. [事件类型](#事件类型)
6. [前端示例代码](#前端示例代码)
7. [错误处理](#错误处理)
8. [常见问题](#常见问题)

---

## 概述

### 技术特点
- **协议**: 原生 WebSocket (非 STOMP)
- **消息格式**: JSON
- **认证方式**: Token 参数认证
- **支持库**: 原生 `WebSocket` 或 `SockJS`
- **端口**: 与 HTTP 服务共用 (默认 8080)

### WebSocket 端点
```
ws://127.0.0.1:8080/ws/chat?token={your-token}
```

或使用 SockJS:
```
http://127.0.0.1:8080/ws/chat?token={your-token}
```

---

## 连接方式

### 方式 1: 原生 WebSocket (推荐用于现代浏览器)

```typescript
const token = 'your-access-token'; // 从登录接口获取
const ws = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${token}`);

ws.onopen = () => {
  console.log('WebSocket 连接成功');
};

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('收到消息:', message);
};

ws.onerror = (error) => {
  console.error('WebSocket 错误:', error);
};

ws.onclose = (event) => {
  console.log('WebSocket 连接关闭:', event.code, event.reason);
};
```

### 方式 2: SockJS (兼容旧浏览器)

```typescript
import SockJS from 'sockjs-client';

const token = 'your-access-token';
const socket = new SockJS(`http://127.0.0.1:8080/ws/chat?token=${token}`);

socket.onopen = () => {
  console.log('SockJS 连接成功');
};

socket.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('收到消息:', message);
};

socket.onerror = (error) => {
  console.error('SockJS 错误:', error);
};

socket.onclose = () => {
  console.log('SockJS 连接关闭');
};
```

---

## 认证机制

### Token 获取
首先通过登录接口获取 Token:

```typescript
const response = await fetch('http://127.0.0.1:8080/api/v1/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'agent@example.com',
    password: 'password123'
  })
});

const { data } = await response.json();
const token = data.token; // 使用此 token 连接 WebSocket
```

### Token 传递
Token 必须通过 URL 查询参数传递:
```
ws://127.0.0.1:8080/ws/chat?token=your-token-here
```

**注意**: 
- ❌ 不支持通过 HTTP Header 传递 Token
- ❌ 不支持在连接后再发送 Token
- ✅ 必须在建立连接时通过 URL 参数传递

---

## 消息格式

WebSocket 支持两种类型的消息：**事件消息** 和 **聊天消息**。

### 1. 事件消息 (Event Message)

用于订阅、状态变更等控制类操作。

**客户端发送格式**:
```json
{
  "event": "事件名称",
  "payload": {
    // 事件相关的数据
  }
}
```

**服务端响应格式**:
```json
{
  "type": "事件类型",
  "data": {
    // 事件响应数据
  }
}
```

### 2. 聊天消息 (Chat Message)

用于发送和接收聊天内容。

**客户端发送格式**:
```json
{
  "conversationId": "会话ID (可选)",
  "senderId": "发送者ID",
  "content": "消息内容",
  "metadata": {
    // 额外的元数据 (可选)
  }
}
```

**服务端响应格式**:
```json
{
  "channel": "WEB",
  "conversationId": "会话ID",
  "senderId": "发送者ID",
  "content": "消息内容",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## 事件类型

### 常用事件列表

| 事件名称 | 说明 | Payload 示例 |
|---------|------|-------------|
| `subscribe` | 订阅会话更新 | `{ "sessionId": "uuid" }` |
| `unsubscribe` | 取消订阅 | `{ "sessionId": "uuid" }` |
| `typing` | 发送正在输入状态 | `{ "sessionId": "uuid" }` |
| `status_change` | 坐席状态变更 | `{ "status": "ONLINE" }` |

### 示例: 订阅会话

**发送**:
```typescript
ws.send(JSON.stringify({
  event: 'subscribe',
  payload: { sessionId: 'session-uuid-123' }
}));
```

**接收**:
```json
{
  "type": "subscription_confirmed",
  "data": {
    "sessionId": "session-uuid-123",
    "status": "subscribed"
  }
}
```

---

## 前端示例代码

### 完整的 WebSocket 服务封装 (TypeScript)

```typescript
class WebSocketService {
  private ws: WebSocket | null = null;
  private token: string;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 3000;

  constructor(token: string) {
    this.token = token;
  }

  connect() {
    const url = `ws://127.0.0.1:8080/ws/chat?token=${this.token}`;
    this.ws = new WebSocket(url);

    this.ws.onopen = () => {
      console.log('✅ WebSocket 连接成功');
      this.reconnectAttempts = 0;
      this.onConnected();
    };

    this.ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        this.handleMessage(message);
      } catch (error) {
        console.error('解析消息失败:', error);
      }
    };

    this.ws.onerror = (error) => {
      console.error('❌ WebSocket 错误:', error);
    };

    this.ws.onclose = (event) => {
      console.log('🔌 WebSocket 连接关闭:', event.code);
      this.attemptReconnect();
    };
  }

  private attemptReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`🔄 尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
      setTimeout(() => this.connect(), this.reconnectDelay);
    } else {
      console.error('❌ 重连失败，已达到最大尝试次数');
    }
  }

  private handleMessage(message: any) {
    // 判断消息类型
    if (message.event) {
      this.onEvent(message);
    } else if (message.content) {
      this.onChatMessage(message);
    }
  }

  // 发送事件消息
  sendEvent(event: string, payload: any) {
    this.send({ event, payload });
  }

  // 发送聊天消息
  sendChatMessage(conversationId: string, content: string) {
    this.send({
      conversationId,
      senderId: 'current-user-id',
      content,
      metadata: {}
    });
  }

  private send(data: any) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data));
    } else {
      console.error('WebSocket 未连接');
    }
  }

  disconnect() {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }

  // 钩子函数，由外部实现
  onConnected() {
    // 连接成功后的处理
  }

  onEvent(message: any) {
    console.log('收到事件:', message);
  }

  onChatMessage(message: any) {
    console.log('收到聊天消息:', message);
  }
}

// 使用示例
const token = localStorage.getItem('auth_token');
const wsService = new WebSocketService(token);

wsService.onConnected = () => {
  // 连接成功，订阅会话
  wsService.sendEvent('subscribe', { sessionId: 'session-123' });
};

wsService.onChatMessage = (message) => {
  // 显示聊天消息
  console.log(`${message.senderId}: ${message.content}`);
};

wsService.connect();
```

### React Hook 示例

```typescript
import { useEffect, useRef, useState } from 'react';

interface WebSocketMessage {
  event?: string;
  content?: string;
  [key: string]: any;
}

export function useWebSocket(token: string) {
  const ws = useRef<WebSocket | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [messages, setMessages] = useState<WebSocketMessage[]>([]);

  useEffect(() => {
    if (!token) return;

    const socket = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${token}`);

    socket.onopen = () => {
      console.log('WebSocket 连接成功');
      setIsConnected(true);
    };

    socket.onmessage = (event) => {
      const message = JSON.parse(event.data);
      setMessages((prev) => [...prev, message]);
    };

    socket.onerror = (error) => {
      console.error('WebSocket 错误:', error);
    };

    socket.onclose = () => {
      console.log('WebSocket 连接关闭');
      setIsConnected(false);
    };

    ws.current = socket;

    return () => {
      socket.close();
    };
  }, [token]);

  const sendMessage = (data: any) => {
    if (ws.current && ws.current.readyState === WebSocket.OPEN) {
      ws.current.send(JSON.stringify(data));
    }
  };

  return { isConnected, messages, sendMessage };
}

// 组件中使用
function ChatComponent() {
  const token = localStorage.getItem('auth_token')!;
  const { isConnected, messages, sendMessage } = useWebSocket(token);

  const handleSendMessage = () => {
    sendMessage({
      conversationId: 'session-123',
      senderId: 'user-456',
      content: 'Hello!',
      metadata: {}
    });
  };

  return (
    <div>
      <div>状态: {isConnected ? '已连接' : '未连接'}</div>
      <button onClick={handleSendMessage}>发送消息</button>
      <div>
        {messages.map((msg, idx) => (
          <div key={idx}>{JSON.stringify(msg)}</div>
        ))}
      </div>
    </div>
  );
}
```

---

## 错误处理

### 常见错误及解决方案

| 错误 | 原因 | 解决方案 |
|------|------|---------|
| 连接立即关闭 | Token 无效或未提供 | 检查 Token 是否正确，是否在 URL 参数中传递 |
| 403 Forbidden | Token 已过期 | 重新登录获取新 Token |
| 404 Not Found | WebSocket 路径错误 | 确认路径为 `/ws/chat` |
| 握手失败 | CORS 或安全策略问题 | 检查服务端 CORS 配置 |

### 错误消息格式

服务端发送的错误消息:
```json
{
  "channel": "WEB",
  "conversationId": "session-id",
  "senderId": "system",
  "content": "无法解析消息: Invalid JSON",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## 常见问题

### Q1: WebSocket 连接失败怎么办？
**A**: 
1. 确认服务端已启动
2. 检查 Token 是否有效
3. 确认 URL 格式正确，包含 `token` 参数
4. 查看浏览器控制台和服务端日志

### Q2: 如何判断连接已建立？
**A**: 监听 `onopen` 事件:
```javascript
ws.onopen = () => {
  console.log('连接已建立');
};
```

### Q3: 消息发送后没有响应？
**A**: 
1. 确认消息格式正确 (JSON)
2. 检查 `conversationId` 或 `event` 字段是否存在
3. 查看服务端日志是否有错误

### Q4: 如何实现断线重连？
**A**: 监听 `onclose` 事件，使用 `setTimeout` 延迟重连:
```javascript
ws.onclose = () => {
  setTimeout(() => {
    console.log('尝试重连...');
    connect(); // 重新建立连接
  }, 3000);
};
```

### Q5: 生产环境需要使用 WSS 吗？
**A**: 是的，生产环境建议使用 `wss://` (WebSocket over SSL):
```javascript
const ws = new WebSocket(`wss://your-domain.com/ws/chat?token=${token}`);
```

### Q6: 如何调试 WebSocket？
**A**: 
1. 使用浏览器开发者工具的 Network 标签，筛选 WS
2. 安装 WebSocket 调试工具，如 `WebSocket Test Client` 浏览器扩展
3. 查看服务端日志中的 WebSocket 相关信息

---

## 附录

### 完整消息流程图

```
客户端                           服务端
  |                               |
  |-- 建立连接 (携带 token) ------>|
  |                               |-- 验证 token
  |                               |-- 保存会话信息
  |<-------- 连接成功 ------------|
  |                               |
  |-- 发送事件消息 --------------->|
  |                               |-- 处理事件
  |<-------- 事件响应 ------------|
  |                               |
  |-- 发送聊天消息 --------------->|
  |                               |-- AI 处理
  |<-------- 聊天响应 ------------|
  |                               |
  |<-- 服务端推送 (新消息/状态) ---|
  |                               |
  |-- 断开连接 ------------------->|
  |<-------- 连接关闭 ------------|
```

### 推荐阅读
- [MDN WebSocket API](https://developer.mozilla.org/zh-CN/docs/Web/API/WebSocket)
- [Spring WebSocket 文档](https://docs.spring.io/spring-framework/reference/web/websocket.html)

---

**文档版本**: v1.0  
**最后更新**: 2024-01-15
