# 群组聊天功能指南

## 概述

系统采用**群组聊天室**模式，每个客户连接时自动创建独立的群组会话，并自动分配客服。

### 核心特性

✅ **自动创建群组** - 客户获取 Token 时自动创建聊天群组  
✅ **智能客服分配** - 支持可扩展的分配策略（当前：随机分配）  
✅ **多客服协作** - 1个主责客服 + N个支持客服  
✅ **权限控制** - 仅群组成员可收发消息  
✅ **隐藏标识** - 客服间可见的元数据，客户看不到  
✅ **消息归属** - 区分本人发送和他人发送  

---

## 工作流程

```
客户端                         服务端
  |                              |
  |-- 1. 请求 Token ------------->|
  |                              |-- 查找/创建客户
  |                              |-- 创建群组
  |                              |-- 分配客服（策略）
  |                              |-- 创建会话
  |<-- 2. 返回 Token+SessionID --|
  |                              |
  |-- 3. 连接 WebSocket --------->|
  |   (token + sessionId)        |
  |<-- 4. 连接成功 --------------|
  |                              |
  |-- 5. 发送消息 --------------->|
  |   (携带 sessionId)           |-- 验证群组成员
  |                              |-- 保存消息
  |<-- 6. 广播给群组成员 ---------|
  |                              |
  |-- 7. 获取历史消息 ----------->|
  |                              |-- 验证权限
  |                              |-- 返回消息（区分可见性）
  |<-- 8. 返回消息列表 ----------|
```

---

## API 接口

### 1. 获取客户 Token（已优化）

**端点**: `POST /api/v1/public/customer-token`

**请求体**:
```json
{
  "name": "张三",
  "channel": "WEB",
  "email": "zhangsan@example.com",    // 可选
  "phone": "+8613800138000",           // 可选
  "channelUserId": "wx_openid_123"     // 可选，渠道用户ID
}
```

**变更说明**:
- ❌ 移除了 `channelId` 参数
- ✅ 新增 `email`、`phone`、`channelUserId` 可选参数
- ✅ 系统根据 `channel` 字段识别渠道类型

**响应**:
```json
{
  "success": true,
  "data": {
    "customerId": "uuid",
    "token": "cust_xxxx",
    "name": "张三",
    "channel": "WEB",
    "sessionId": "session-uuid",    // 新增：会话 ID
    "groupId": "group-uuid"          // 新增：群组 ID
  }
}
```

**自动执行的操作**:
1. 查找或创建客户
2. 创建聊天群组
3. 分配主责客服（随机策略）
4. 创建聊天会话
5. 生成客户 Token

### 2. 获取群组历史消息

**端点**: `GET /api/v1/chat/sessions/{sessionId}/messages`

**认证**: 支持客户 Token 和坐席 Token

**查询参数**:
- `page` - 页码（默认 0）
- `size` - 每页数量（默认 50）
- `sort` - 排序（默认 `createdAt,asc`）

**示例**:
```http
GET /api/v1/chat/sessions/550e8400-e29b-41d4-a716-446655440000/messages?page=0&size=50
Authorization: Bearer {token}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "msg-uuid",
        "sessionId": "session-uuid",
        "senderType": "AGENT",
        "agentId": "agent-uuid",
        "agentName": "客服小王",
        "text": "您好，请问有什么可以帮您？",
        "internal": false,
        "isMine": false,                    // 是否是本人发送
        "translationData": {},
        "mentionAgentIds": [],
        "attachments": [],
        "agentMetadata": {                  // 仅客服可见
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
        "isMine": true,                     // 客户看到自己的消息
        "translationData": {},
        "mentionAgentIds": [],
        "attachments": [],
        "agentMetadata": null,              // 客户看不到此字段
        "createdAt": "2024-01-15T10:31:00Z"
      }
    ],
    "totalElements": 25,
    "totalPages": 1,
    "size": 50,
    "number": 0
  }
}
```

**字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `isMine` | boolean | 是否是本人发送的消息 |
| `agentMetadata` | object | **仅客服可见**的元数据，客户调用时返回 `null` |
| `internal` | boolean | 是否是内部消息（客户看不到） |

---

## 隐藏标识功能

### agentMetadata 字段

`agentMetadata` 是一个 JSON 对象，仅群组内的客服可见，客户完全看不到。

**用途**:
- 客服之间的协作备注
- 客户标签和优先级
- 内部处理状态
- 敏感信息标记

**示例**:
```json
{
  "agentMetadata": {
    "priority": "high",           // 优先级
    "customerType": "VIP",        // 客户类型
    "tags": ["投诉", "退款"],    // 内部标签
    "assignedTo": "张三",         // 指派给
    "notes": "客户情绪激动，需耐心处理",
    "relatedTicket": "TICKET-123"
  }
}
```

**可见性规则**:
```javascript
// 客服调用接口
GET /api/v1/chat/sessions/{sessionId}/messages
Authorization: Bearer {agent-token}

// 响应包含 agentMetadata
{
  "agentMetadata": {
    "priority": "high",
    "notes": "..."
  }
}

// 客户调用接口
GET /api/v1/chat/sessions/{sessionId}/messages
Authorization: Bearer {customer-token}

// 响应 agentMetadata 为 null
{
  "agentMetadata": null
}
```

---

## 客服分配策略

### 抽象类设计

```java
public abstract class AgentAssignmentStrategy {
    // 分配主责客服
    public abstract Agent assignPrimaryAgent(
        Customer customer, 
        Channel channel, 
        ChatGroup group
    );
    
    // 分配支持客服（可选）
    public List<Agent> assignSupportAgents(
        Customer customer, 
        Channel channel, 
        Agent primaryAgent, 
        ChatGroup group
    ) {
        return List.of(); // 默认不分配
    }
    
    public abstract String getStrategyName();
}
```

### 当前实现：随机分配

```java
@Component
public class RandomAgentAssignmentStrategy extends AgentAssignmentStrategy {
    @Override
    public Agent assignPrimaryAgent(Customer customer, Channel channel, ChatGroup group) {
        // 从在线客服中随机选择
        List<Agent> onlineAgents = agentRepository.findByStatus(AgentStatus.ONLINE);
        return onlineAgents.get(random.nextInt(onlineAgents.size()));
    }
    
    @Override
    public String getStrategyName() {
        return "RANDOM";
    }
}
```

### 自定义策略示例

**按技能分配**:
```java
@Component
public class SkillBasedAssignmentStrategy extends AgentAssignmentStrategy {
    @Override
    public Agent assignPrimaryAgent(Customer customer, Channel channel, ChatGroup group) {
        // 根据客户标签查找具有对应技能的客服
        List<String> skills = customer.getTags();
        List<Agent> agents = agentRepository.findBySkills(skills);
        return selectBestAgent(agents);
    }
}
```

**负载均衡**:
```java
@Component
public class LoadBalanceAssignmentStrategy extends AgentAssignmentStrategy {
    @Override
    public Agent assignPrimaryAgent(Customer customer, Channel channel, ChatGroup group) {
        // 选择当前接待客户最少的客服
        List<Agent> agents = agentRepository.findByStatus(AgentStatus.ONLINE);
        return agents.stream()
            .min(Comparator.comparingInt(this::getActiveSessionCount))
            .orElseThrow();
    }
}
```

**切换策略**:
```java
@Configuration
public class AssignmentConfig {
    @Bean
    @Primary
    public AgentAssignmentStrategy agentAssignmentStrategy() {
        return new SkillBasedAssignmentStrategy(); // 使用技能分配
    }
}
```

---

## 群组成员权限

### 权限验证

系统自动验证消息访问权限：

**群组成员包括**:
- 客户本人
- 主责客服
- 支持客服列表中的所有客服

**权限检查代码**:
```java
public boolean isSessionMember(UUID sessionId, UUID agentId, UUID customerId) {
    ChatSession session = getSession(sessionId);
    
    // 检查是否是客户
    if (customerId != null && session.getCustomer().getId().equals(customerId)) {
        return true;
    }
    
    // 检查是否是主责客服
    if (agentId != null && session.getPrimaryAgent().getId().equals(agentId)) {
        return true;
    }
    
    // 检查是否是支持客服
    if (agentId != null && session.getSupportAgentIds().contains(agentId)) {
        return true;
    }
    
    return false;
}
```

**访问限制**:
```java
// 非成员访问会抛出异常
if (!chatSessionService.isSessionMember(sessionId, agentId, customerId)) {
    throw new SecurityException("无权访问此会话的消息");
}
```

---

## WebSocket 消息格式

### 客户端发送消息

```json
{
  "sessionId": "session-uuid",
  "senderId": "customer-uuid",
  "content": "消息内容",
  "metadata": {}
}
```

### 服务端推送消息

```json
{
  "sessionId": "session-uuid",
  "senderType": "AGENT",
  "agentId": "agent-uuid",
  "agentName": "客服小王",
  "content": "回复内容",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## 数据库表结构

### chat_sessions 表（新增字段）

```sql
ALTER TABLE chat_sessions 
ADD COLUMN customer_id CHAR(36) AFTER user_id COMMENT '客户ID';
```

### messages 表（新增字段）

```sql
ALTER TABLE messages 
ADD COLUMN agent_metadata JSON COMMENT '客服可见的元数据（对客户隐藏）';
```

---

## 完整示例

### Web 客户端接入

```javascript
// 1. 获取 Token
const response = await fetch('/api/v1/public/customer-token', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    name: '张三',
    channel: 'WEB',
    email: 'zhangsan@example.com'
  })
});

const { data } = await response.json();
const { token, sessionId, groupId } = data;

// 2. 连接 WebSocket
const ws = new WebSocket(`ws://127.0.0.1:8080/ws/chat?token=${token}`);

ws.onopen = async () => {
  console.log('已连接到客服');
  
  // 3. 获取历史消息
  const historyResponse = await fetch(
    `/api/v1/chat/sessions/${sessionId}/messages`,
    {
      headers: { 'Authorization': `Bearer ${token}` }
    }
  );
  
  const history = await historyResponse.json();
  console.log('历史消息:', history.data.content);
};

// 4. 发送消息
ws.send(JSON.stringify({
  sessionId: sessionId,
  senderId: data.customerId,
  content: '你好，我需要帮助'
}));

// 5. 接收消息
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('收到消息:', message);
  // 注意：客户端看不到 agentMetadata
};
```

### 客服端查看消息

```javascript
// 1. 客服登录获取 Token
const loginResponse = await fetch('/api/v1/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'agent@example.com',
    password: 'password'
  })
});

const { data } = await loginResponse.json();
const agentToken = data.token;

// 2. 获取会话消息（包含 agentMetadata）
const messagesResponse = await fetch(
  `/api/v1/chat/sessions/${sessionId}/messages`,
  {
    headers: { 'Authorization': `Bearer ${agentToken}` }
  }
);

const messages = await messagesResponse.json();

messages.data.content.forEach(msg => {
  console.log('消息:', msg.text);
  console.log('是否本人:', msg.isMine);
  
  if (msg.agentMetadata) {
    // 仅客服可见
    console.log('内部标记:', msg.agentMetadata);
  }
});
```

---

## 常见问题

### Q1: 客户可以加入多个群组吗？

**A**: 可以。每次调用 `/customer-token` 接口都会创建新的群组和会话。如果需要复用会话，前端应保存 `sessionId`。

### Q2: 如何添加支持客服到群组？

**A**: 实现自定义 `AgentAssignmentStrategy`，在 `assignSupportAgents` 方法中返回支持客服列表。

### Q3: agentMetadata 可以存储哪些数据？

**A**: 任何 JSON 格式的数据，例如：
- 客户标签
- 优先级
- 内部备注
- 关联工单号
- 处理状态

### Q4: 消息的 isMine 字段如何判断？

**A**: 
- 客服：比较 `message.agentId` 和当前客服 ID
- 客户：比较 `message.session.customerId` 和当前客户 ID

### Q5: 如何切换客服分配策略？

**A**: 在 Spring 配置中注入不同的 `AgentAssignmentStrategy` Bean，或使用 `@Primary` 注解指定默认策略。

---

## 技术要点

1. **群组隔离**: 每个客户独立群组，消息不会串
2. **权限控制**: 接口层验证成员身份
3. **数据隔离**: `agentMetadata` 在 DTO 转换时过滤
4. **扩展性**: 策略模式支持自定义分配逻辑
5. **性能优化**: 使用索引加速成员查询

---

**文档版本**: v2.0  
**最后更新**: 2024-01-15
