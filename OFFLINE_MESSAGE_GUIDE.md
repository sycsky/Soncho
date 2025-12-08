# 离线消息功能指南

## 📬 功能概述

离线消息功能确保用户和客服在离线期间收到的消息能在上线后自动推送，不会遗漏任何对话内容。

### 核心特性

✅ **自动推送** - WebSocket 连接建立时自动推送离线消息  
✅ **已读标记** - 区分客户已读和客服已读状态  
✅ **未读统计** - 实时查询未读消息数量  
✅ **双向支持** - 同时支持客户端和客服端  
✅ **性能优化** - 使用索引加速查询  

---

## 工作原理

### 消息已读状态

每条消息包含两个已读标记：

| 字段 | 类型 | 说明 |
|------|------|------|
| `readByCustomer` | boolean | 客户是否已读此消息 |
| `readByAgent` | boolean | 客服是否已读此消息 |

### 离线消息判断逻辑

**客户离线消息**：
- 发送者：客服（`senderType = AGENT`）
- 状态：`readByCustomer = false`

**客服离线消息**：
- 发送者：客户（`senderType = USER`）
- 状态：`readByAgent = false`

### 推送流程

```
用户上线
  ↓
WebSocket 连接建立
  ↓
识别用户身份（客户/客服）
  ↓
查询未读消息
  ↓
推送离线消息
  ↓
发送推送完成通知
```

---

## 数据库变更

### 执行迁移

```bash
mysql -u root -p ai_agent < db/add_message_read_status.sql
```

### 表结构变更

```sql
-- messages 表新增字段
ALTER TABLE messages 
ADD COLUMN read_by_customer BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN read_by_agent BOOLEAN NOT NULL DEFAULT FALSE;

-- 性能优化索引
CREATE INDEX idx_messages_read_by_customer ON messages(session_id, read_by_customer, sender_type);
CREATE INDEX idx_messages_read_by_agent ON messages(session_id, read_by_agent, sender_type);
```

---

## WebSocket 离线消息推送

### 连接建立时自动推送

当用户连接 WebSocket 时，系统会自动推送离线消息。

#### 推送消息格式

**单条离线消息**：
```json
{
  "type": "offline_message",
  "message": {
    "id": "msg-uuid",
    "sessionId": "session-uuid",
    "senderType": "AGENT",
    "agentName": "客服小王",
    "text": "您好，我是客服小王",
    "isMine": false,
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

**推送完成通知**：
```json
{
  "type": "offline_messages_complete",
  "count": 5
}
```

### 客户端接收示例

```javascript
const ws = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${token}`);

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  
  if (data.type === 'offline_message') {
    // 处理离线消息
    console.log('📬 收到离线消息:', data.message);
    displayMessage(data.message);
  } else if (data.type === 'offline_messages_complete') {
    // 离线消息推送完成
    console.log(`✅ 离线消息推送完成，共 ${data.count} 条`);
    showNotification(`您有 ${data.count} 条未读消息`);
  } else {
    // 普通实时消息
    displayMessage(data);
  }
};
```

---

## REST API

### 1. 获取未读消息数量

**端点**: `GET /api/v1/offline-messages/unread-count`

**认证**: 需要 Token（客户或客服）

**响应**:
```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": {
    "unreadCount": 12
  }
}
```

**使用示例**:
```javascript
async function getUnreadCount() {
  const token = localStorage.getItem('customer_token');
  
  const response = await fetch('http://127.0.0.1:8080/api/v1/offline-messages/unread-count', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  const result = await response.json();
  const count = result.data.unreadCount;
  
  if (count > 0) {
    updateBadge(count); // 更新未读徽章
  }
  
  return count;
}
```

---

### 2. 标记会话消息为已读

**端点**: `POST /api/v1/offline-messages/sessions/{sessionId}/mark-read`

**认证**: 需要 Token（客户或客服）

**参数**:
- `sessionId` (路径参数): 会话 ID

**响应**:
```json
{
  "success": true,
  "code": 200,
  "message": "Success",
  "data": {
    "success": true,
    "message": "已标记为已读"
  }
}
```

**使用示例**:
```javascript
async function markSessionAsRead(sessionId) {
  const token = localStorage.getItem('customer_token');
  
  await fetch(`http://127.0.0.1:8080/api/v1/offline-messages/sessions/${sessionId}/mark-read`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  console.log('✅ 会话已标记为已读');
}

// 当用户打开聊天窗口时调用
markSessionAsRead('session-uuid-123');
```

---

## 完整使用示例

### 客户端完整示例

```javascript
class ChatClientWithOfflineMessages {
  constructor() {
    this.ws = null;
    this.token = null;
    this.sessionId = null;
    this.unreadCount = 0;
  }

  // 初始化
  async init(token, sessionId) {
    this.token = token;
    this.sessionId = sessionId;
    
    // 获取未读数
    await this.fetchUnreadCount();
    
    // 连接 WebSocket
    this.connect();
  }

  // 获取未读消息数
  async fetchUnreadCount() {
    const response = await fetch('http://127.0.0.1:8080/api/v1/offline-messages/unread-count', {
      headers: { 'Authorization': `Bearer ${this.token}` }
    });
    
    const result = await response.json();
    this.unreadCount = result.data.unreadCount;
    
    if (this.unreadCount > 0) {
      this.showUnreadBadge(this.unreadCount);
    }
  }

  // 连接 WebSocket
  connect() {
    this.ws = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${this.token}`);

    this.ws.onopen = () => {
      console.log('✅ 已连接到客服');
    };

    this.ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      
      if (data.type === 'offline_message') {
        // 处理离线消息
        console.log('📬 离线消息:', data.message.text);
        this.displayMessage(data.message, true); // true 表示离线消息
      } else if (data.type === 'offline_messages_complete') {
        // 离线消息推送完成
        console.log(`✅ ${data.count} 条离线消息推送完成`);
        this.hideLoadingIndicator();
        
        // 标记为已读
        this.markAsRead();
      } else if (data.content) {
        // 实时消息
        this.displayMessage(data, false);
      }
    };

    this.ws.onerror = (error) => {
      console.error('❌ 连接错误:', error);
    };

    this.ws.onclose = () => {
      console.log('🔌 连接已断开');
      setTimeout(() => this.connect(), 3000); // 重连
    };
  }

  // 显示消息
  displayMessage(message, isOffline = false) {
    const messageDiv = document.createElement('div');
    messageDiv.className = message.isMine ? 'message-me' : 'message-other';
    
    if (isOffline) {
      messageDiv.classList.add('offline-message');
    }
    
    messageDiv.innerHTML = `
      ${isOffline ? '<span class="offline-badge">离线消息</span>' : ''}
      <div class="sender">${message.agentName || '我'}</div>
      <div class="content">${message.text || message.content}</div>
      <div class="time">${new Date(message.createdAt || message.timestamp).toLocaleTimeString()}</div>
    `;
    
    document.getElementById('chat-messages').appendChild(messageDiv);
  }

  // 标记会话为已读
  async markAsRead() {
    await fetch(`http://127.0.0.1:8080/api/v1/offline-messages/sessions/${this.sessionId}/mark-read`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${this.token}` }
    });
    
    this.unreadCount = 0;
    this.hideUnreadBadge();
  }

  // 显示未读徽章
  showUnreadBadge(count) {
    const badge = document.getElementById('unread-badge');
    badge.textContent = count;
    badge.style.display = 'block';
  }

  // 隐藏未读徽章
  hideUnreadBadge() {
    const badge = document.getElementById('unread-badge');
    badge.style.display = 'none';
  }

  // 发送消息
  sendMessage(content) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({
        sessionId: this.sessionId,
        senderId: localStorage.getItem('customer_id'),
        content: content,
        metadata: {}
      }));
    }
  }
}

// 使用示例
const chatClient = new ChatClientWithOfflineMessages();
chatClient.init(customerToken, sessionId);
```

---

### 客服端完整示例

```javascript
class AgentChatWithOfflineMessages {
  constructor() {
    this.ws = null;
    this.token = null;
    this.agentId = null;
    this.sessions = new Map(); // sessionId -> unreadCount
  }

  async init(token, agentId) {
    this.token = token;
    this.agentId = agentId;
    
    // 获取总未读数
    await this.fetchUnreadCount();
    
    // 连接 WebSocket
    this.connect();
  }

  async fetchUnreadCount() {
    const response = await fetch('http://127.0.0.1:8080/api/v1/offline-messages/unread-count', {
      headers: { 'Authorization': `Bearer ${this.token}` }
    });
    
    const result = await response.json();
    const totalUnread = result.data.unreadCount;
    
    console.log(`📬 客服有 ${totalUnread} 条未读消息`);
    this.updateTotalUnreadBadge(totalUnread);
  }

  connect() {
    this.ws = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${this.token}`);

    this.ws.onopen = () => {
      console.log('✅ 客服端已连接');
    };

    this.ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      
      if (data.type === 'offline_message') {
        // 离线消息
        console.log('📬 离线消息:', data.message);
        this.handleOfflineMessage(data.message);
      } else if (data.type === 'offline_messages_complete') {
        console.log(`✅ ${data.count} 条离线消息推送完成`);
      } else if (data.content) {
        // 实时消息
        this.handleRealtimeMessage(data);
      }
    };
  }

  handleOfflineMessage(message) {
    // 显示在对应的会话列表中
    this.addMessageToSession(message.sessionId, message, true);
    
    // 更新未读计数
    const currentCount = this.sessions.get(message.sessionId) || 0;
    this.sessions.set(message.sessionId, currentCount + 1);
    this.updateSessionBadge(message.sessionId, currentCount + 1);
  }

  // 打开会话时标记为已读
  async openSession(sessionId) {
    // 标记为已读
    await fetch(`http://127.0.0.1:8080/api/v1/offline-messages/sessions/${sessionId}/mark-read`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${this.token}` }
    });
    
    // 清除未读标记
    this.sessions.set(sessionId, 0);
    this.updateSessionBadge(sessionId, 0);
  }

  updateTotalUnreadBadge(count) {
    document.getElementById('total-unread').textContent = count > 0 ? count : '';
  }

  updateSessionBadge(sessionId, count) {
    const badge = document.querySelector(`[data-session="${sessionId}"] .unread-badge`);
    if (badge) {
      badge.textContent = count;
      badge.style.display = count > 0 ? 'block' : 'none';
    }
  }
}

// 使用示例
const agentChat = new AgentChatWithOfflineMessages();
agentChat.init(agentToken, agentId);
```

---

## 消息自动标记逻辑

系统会自动处理消息的已读状态：

### 1. 消息创建时

```java
// 客服发送的消息
message.setReadByAgent(true);   // 客服自己已读
message.setReadByCustomer(false); // 客户未读

// 客户发送的消息
message.setReadByCustomer(true);  // 客户自己已读
message.setReadByAgent(false);    // 客服未读
```

### 2. 用户上线时

- 自动推送离线消息
- 不自动标记为已读

### 3. 显式标记已读

调用 `/mark-read` API 时：
- 将会话中所有未读消息标记为已读
- 适合在用户打开会话窗口时调用

---

## 性能优化

### 数据库索引

```sql
-- 客户未读消息查询优化
CREATE INDEX idx_messages_read_by_customer 
ON messages(session_id, read_by_customer, sender_type);

-- 客服未读消息查询优化
CREATE INDEX idx_messages_read_by_agent 
ON messages(session_id, read_by_agent, sender_type);
```

### 查询优化

- 使用复合索引加速未读消息查询
- 分页加载历史消息
- 缓存未读计数（可选）

---

## 常见问题

### Q1: 离线消息会推送多少条？

**A**: 推送所有未读消息。如果数量很大，建议：
- 限制推送数量（如最近 100 条）
- 使用分批推送
- 让用户手动加载更多

### Q2: 如何避免重复推送？

**A**: 系统通过 `readByCustomer` 和 `readByAgent` 字段确保消息只推送一次。标记为已读后不会再次推送。

### Q3: 用户关闭聊天窗口后，消息会标记为已读吗？

**A**: 不会。需要显式调用 `mark-read` API。建议：
- 用户打开会话时调用
- 或在用户查看消息后调用

### Q4: 如何处理大量离线消息？

**A**: 建议策略：
```javascript
// 限制推送数量
const MAX_OFFLINE_MESSAGES = 100;

// 分批推送
for (let i = 0; i < unreadMessages.length; i += 10) {
  const batch = unreadMessages.slice(i, i + 10);
  await pushBatch(batch);
  await sleep(100); // 避免消息洪水
}
```

### Q5: 客服如何知道哪个会话有未读消息？

**A**: 
1. 连接时自动推送所有离线消息
2. 调用 `/unread-count` API 获取总数
3. 在会话列表中显示未读徽章

---

## 最佳实践

### 1. 客户端

```javascript
// ✅ 连接建立后等待离线消息推送完成
ws.onmessage = (event) => {
  if (event.data.type === 'offline_messages_complete') {
    // 离线消息加载完成，可以显示聊天界面
    showChatInterface();
  }
};

// ✅ 用户查看会话时标记为已读
function openChatWindow(sessionId) {
  markAsRead(sessionId);
  loadMessages(sessionId);
}

// ❌ 不要在连接建立时立即标记为已读
// 应该等用户真正查看消息后再标记
```

### 2. 客服端

```javascript
// ✅ 在会话列表显示未读徽章
sessions.forEach(session => {
  if (session.unreadCount > 0) {
    showBadge(session.id, session.unreadCount);
  }
});

// ✅ 切换会话时标记为已读
function switchToSession(sessionId) {
  markAsRead(sessionId);
  hideSessionBadge(sessionId);
}
```

### 3. UI/UX 建议

- 离线消息用特殊样式标识（如淡色背景）
- 显示"离线消息"标签
- 推送完成后显示提示："您有 5 条新消息"
- 在未读消息和已读消息之间添加分隔线

---

**文档版本**: v1.0  
**最后更新**: 2024-01-15  
**相关功能**: 聊天系统、WebSocket、消息推送
