# 快速参考卡片

## 📌 核心 API

### 1. 获取客户 Token
```http
POST /api/v1/public/customer-token
Content-Type: application/json

{
  "name": "张三",
  "channel": "WEB",
  "email": "zhangsan@example.com"
}
```

**响应**:
```json
{
  "customerId": "uuid",
  "token": "cust_xxxx",
  "sessionId": "session-uuid",
  "groupId": "group-uuid"
}
```

---

### 2. 获取历史消息
```http
GET /api/v1/chat/sessions/{sessionId}/messages?page=0&size=50
Authorization: Bearer {token}
```

**响应字段**:
- `isMine` - 是否本人发送
- `agentMetadata` - 仅客服可见（客户为 null）

---

## 🎯 关键概念

| 概念 | 说明 |
|------|------|
| **群组模式** | 1 客户 + 1 主责客服 + N 支持客服 |
| **sessionId** | 会话/群组标识，用于发送消息和获取历史 |
| **agentMetadata** | 客服间可见的隐藏标识，客户看不到 |
| **isMine** | 区分本人发送和他人发送的消息 |

---

## 🔐 Token 类型

| Token 类型 | 前缀 | 用途 |
|-----------|------|------|
| 客户 Token | `cust_` | 客户连接 WebSocket、获取消息 |
| 坐席 Token | 无前缀 | 坐席管理、查看隐藏信息 |

---

## 📊 数据模型

```
Customer (客户)
  ├─ id
  ├─ name
  ├─ primaryChannel
  ├─ email
  ├─ phone
  └─ channelUserId

ChatSession (会话)
  ├─ id (sessionId)
  ├─ customer
  ├─ group
  ├─ primaryAgent
  └─ supportAgentIds[]

Message (消息)
  ├─ id
  ├─ session
  ├─ text
  ├─ senderType
  ├─ agent
  └─ agentMetadata  <-- 隐藏字段
```

---

## 🚀 客户端接入流程

```javascript
// 1. 获取 Token
const { data } = await fetch('/api/v1/public/customer-token', {
  method: 'POST',
  body: JSON.stringify({ name: '张三', channel: 'WEB' })
}).then(r => r.json());

// 2. 连接 WebSocket
const ws = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${data.token}`);

// 3. 获取历史消息
const history = await fetch(
  `/api/v1/chat/sessions/${data.sessionId}/messages`,
  { headers: { 'Authorization': `Bearer ${data.token}` }}
).then(r => r.json());

// 4. 发送消息
ws.send(JSON.stringify({
  sessionId: data.sessionId,
  content: '你好'
}));
```

---

## 🔧 客服分配策略

### 当前策略
- **随机分配**: 从在线客服中随机选择

### 自定义策略
```java
@Component
public class CustomStrategy extends AgentAssignmentStrategy {
    @Override
    public Agent assignPrimaryAgent(Customer customer, Channel channel, ChatGroup group) {
        // 自定义逻辑
        return selectAgent();
    }
}
```

---

## 📝 agentMetadata 示例

```json
{
  "agentMetadata": {
    "priority": "high",
    "tags": ["VIP", "投诉"],
    "notes": "需要特别关注",
    "assignedTo": "张三",
    "relatedTicket": "TICKET-123"
  }
}
```

**可见性**:
- ✅ 客服调用：可见
- ❌ 客户调用：null

---

## 🗄️ 数据库迁移

```bash
mysql -u root -p ai_agent < db/create_customers_table.sql
mysql -u root -p ai_agent < db/alter_tables_for_group_chat.sql
```

---

## 📚 文档索引

| 文档 | 说明 |
|------|------|
| `GROUP_CHAT_GUIDE.md` | 群组聊天完整指南 |
| `CUSTOMER_MODULE_V2_UPDATES.md` | V2 更新说明 |
| `CUSTOMER_INTEGRATION_GUIDE.md` | 客户端接入指南 |
| `CUSTOMER_API_SUMMARY.md` | API 接口总结 |

---

## 🎓 常见场景

### 场景 1: Web 聊天窗口
```javascript
// 1. 获取 Token
const { data } = await getCustomerToken({ name: '访客', channel: 'WEB' });

// 2. 连接 WebSocket
const ws = new WebSocket(`ws://localhost:8080/ws/chat?token=${data.token}`);

// 3. 发送消息
ws.send(JSON.stringify({ sessionId: data.sessionId, content: '你好' }));
```

### 场景 2: 客服查看消息（含隐藏信息）
```javascript
// 使用坐席 Token 获取消息
const messages = await fetch(`/api/v1/chat/sessions/${sessionId}/messages`, {
  headers: { 'Authorization': `Bearer ${agentToken}` }
}).then(r => r.json());

// 可以看到 agentMetadata
messages.data.content.forEach(msg => {
  console.log(msg.agentMetadata); // 客服可见
});
```

---

**版本**: V2.0  
**最后更新**: 2024-01-15
