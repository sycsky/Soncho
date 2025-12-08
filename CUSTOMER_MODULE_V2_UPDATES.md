# 客户模块 V2 更新说明

## 🎉 主要更新

### 1. 优化 customer-token 接口

#### 变更内容

**移除参数**:
- ❌ `channelId` - 不再需要单独的渠道标识参数

**新增参数**:
- ✅ `email` (可选) - 客户邮箱
- ✅ `phone` (可选) - 客户手机号
- ✅ `channelUserId` (可选) - 渠道用户 ID（如微信 OpenID）

**变更前**:
```json
{
  "name": "张三",
  "channel": "WEB",
  "channelId": "web_user_123"
}
```

**变更后**:
```json
{
  "name": "张三",
  "channel": "WEB",
  "email": "zhangsan@example.com",
  "phone": "+8613800138000",
  "channelUserId": "wx_openid_123"
}
```

#### 设计优势

1. **更灵活的客户识别**: 支持邮箱、手机号、渠道 ID 多种方式
2. **更好的去重机制**: 优先级查找（channelUserId > email > phone）
3. **更符合业务逻辑**: 直接通过 `channel` 字段识别渠道类型

---

### 2. 自动创建群组和分配客服

#### 新增功能

调用 `/customer-token` 接口时，系统**自动执行**以下操作：

1. ✅ 查找或创建客户
2. ✅ 创建聊天群组
3. ✅ 分配主责客服（使用分配策略）
4. ✅ 创建聊天会话
5. ✅ 生成客户 Token

#### 响应变化

**新增字段**:
```json
{
  "customerId": "uuid",
  "token": "cust_xxxx",
  "name": "张三",
  "channel": "WEB",
  "sessionId": "session-uuid",   // 新增：会话 ID
  "groupId": "group-uuid"         // 新增：群组 ID
}
```

#### 使用示例

```javascript
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

// 直接使用 sessionId 连接 WebSocket 或获取历史消息
```

---

### 3. 客服分配策略系统

#### 抽象类设计

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
    );
    
    public abstract String getStrategyName();
}
```

#### 当前实现：随机分配策略

```java
@Component
public class RandomAgentAssignmentStrategy extends AgentAssignmentStrategy {
    @Override
    public Agent assignPrimaryAgent(Customer customer, Channel channel, ChatGroup group) {
        // 从在线客服中随机选择
        List<Agent> onlineAgents = agentRepository.findByStatus(AgentStatus.ONLINE);
        return onlineAgents.get(random.nextInt(onlineAgents.size()));
    }
}
```

#### 自定义扩展

**示例 1: 按技能分配**
```java
@Component
public class SkillBasedAssignmentStrategy extends AgentAssignmentStrategy {
    @Override
    public Agent assignPrimaryAgent(Customer customer, Channel channel, ChatGroup group) {
        // 根据客户标签匹配客服技能
        return findAgentBySkill(customer.getTags());
    }
}
```

**示例 2: 负载均衡**
```java
@Component
public class LoadBalanceAssignmentStrategy extends AgentAssignmentStrategy {
    @Override
    public Agent assignPrimaryAgent(Customer customer, Channel channel, ChatGroup group) {
        // 选择当前接待最少的客服
        return findLeastBusyAgent();
    }
}
```

---

### 4. 群组聊天功能

#### 群组模式

- **1 个客户** + **1 个主责客服** + **N 个支持客服**
- 所有消息通过群组 Session 传递
- 仅群组成员可收发消息

#### 数据模型变更

**ChatSession 新增字段**:
```java
@ManyToOne
@JoinColumn(name = "customer_id")
private Customer customer;
```

**Message 新增字段**:
```java
@Column(name = "agent_metadata", columnDefinition = "json")
private Map<String, Object> agentMetadata;  // 客服可见的隐藏元数据
```

#### 权限控制

```java
public boolean isSessionMember(UUID sessionId, UUID agentId, UUID customerId) {
    // 验证是否是群组成员
    // - 客户本人
    // - 主责客服
    // - 支持客服
}
```

---

### 5. 消息隐藏标识功能

#### agentMetadata 字段

**功能**: 仅群组内客服可见的元数据，客户完全看不到。

**用途**:
- 客服间协作备注
- 客户标签和优先级
- 内部处理状态
- 敏感信息标记

**示例**:
```json
{
  "agentMetadata": {
    "priority": "high",
    "customerType": "VIP",
    "tags": ["投诉", "退款"],
    "notes": "客户情绪激动，需耐心处理"
  }
}
```

**可见性规则**:
```javascript
// 客服调用 - 可见 agentMetadata
GET /api/v1/chat/sessions/{sessionId}/messages
Authorization: Bearer {agent-token}
// 响应包含 agentMetadata

// 客户调用 - agentMetadata 为 null
GET /api/v1/chat/sessions/{sessionId}/messages
Authorization: Bearer {customer-token}
// 响应 agentMetadata 为 null
```

---

### 6. 群组历史消息 API

#### 新增接口

**端点**: `GET /api/v1/chat/sessions/{sessionId}/messages`

**特性**:
- ✅ 支持客户 Token 和坐席 Token
- ✅ 自动验证群组成员权限
- ✅ 区分本人发送和他人发送（`isMine` 字段）
- ✅ 客服可见 `agentMetadata`，客户不可见
- ✅ 支持分页

**响应示例**:
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
        "isMine": false,
        "agentMetadata": {
          "priority": "high"
        },
        "createdAt": "2024-01-15T10:30:00Z"
      }
    ],
    "totalElements": 25
  }
}
```

---

### 7. 统一认证过滤器

#### 新增组件

**UnifiedAuthenticationFilter**: 同时支持客户 Token 和坐席 Token

```java
@Component
public class UnifiedAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) {
        if (token.startsWith("cust_")) {
            // 客户认证
            customerTokenService.resolve(token).ifPresent(...);
        } else {
            // 坐席认证
            tokenService.resolve(token).ifPresent(...);
        }
    }
}
```

**优势**:
- 单个过滤器处理双重认证
- 自动识别 Token 类型
- 简化配置

---

## 🗄️ 数据库变更

### 执行迁移 SQL

```bash
mysql -u root -p ai_agent < db/alter_tables_for_group_chat.sql
```

### 变更内容

```sql
-- chat_sessions 表新增字段
ALTER TABLE chat_sessions 
ADD COLUMN customer_id CHAR(36) AFTER user_id;

-- messages 表新增字段
ALTER TABLE messages 
ADD COLUMN agent_metadata JSON AFTER translation_data;
```

---

## 📁 新增文件清单

### Java 代码

```
src/main/java/com/example/aikef/
├── service/
│   ├── ChatSessionService.java         # 会话管理服务
│   ├── MessageService.java              # 消息服务
│   └── strategy/
│       ├── AgentAssignmentStrategy.java           # 分配策略抽象类
│       └── RandomAgentAssignmentStrategy.java     # 随机分配实现
├── security/
│   └── UnifiedAuthenticationFilter.java  # 统一认证过滤器
├── dto/
│   └── ChatMessageDto.java              # 聊天消息 DTO
└── controller/
    └── ChatController.java              # 聊天 API
```

### 数据库脚本

```
db/
└── alter_tables_for_group_chat.sql     # 表结构变更脚本
```

### 文档

```
GROUP_CHAT_GUIDE.md                     # 群组聊天功能指南
CUSTOMER_MODULE_V2_UPDATES.md           # 本文档
```

---

## 🔄 迁移指南

### 从 V1 迁移到 V2

#### 1. 更新 API 调用

**变更前**:
```javascript
fetch('/api/v1/public/customer-token', {
  method: 'POST',
  body: JSON.stringify({
    name: '张三',
    channel: 'WEB',
    channelId: 'web_user_123'
  })
});
```

**变更后**:
```javascript
fetch('/api/v1/public/customer-token', {
  method: 'POST',
  body: JSON.stringify({
    name: '张三',
    channel: 'WEB',
    email: 'zhangsan@example.com'  // 可选
  })
});
```

#### 2. 使用返回的 sessionId

```javascript
const { data } = await response.json();
const { token, sessionId } = data;

// 获取历史消息
fetch(`/api/v1/chat/sessions/${sessionId}/messages`, {
  headers: { 'Authorization': `Bearer ${token}` }
});
```

#### 3. 执行数据库迁移

```bash
mysql -u root -p ai_agent < db/alter_tables_for_group_chat.sql
```

---

## ⚡ 性能优化

### 索引优化

```sql
-- 新增索引
CREATE INDEX idx_customer_id ON chat_sessions(customer_id);
```

### 查询优化

- 使用分页避免一次性加载大量消息
- 懒加载会话关联的客户和客服信息

---

## 🐛 已知问题

### 无

当前版本未发现重大问题。

---

## 📝 待办事项

- [ ] 实现更多客服分配策略（技能、负载均衡）
- [ ] 添加群组转接功能
- [ ] 实现消息已读状态
- [ ] 添加客服在线状态实时更新
- [ ] 实现群组成员管理接口

---

## 🤝 反馈与支持

如有问题或建议，请联系开发团队。

---

**版本**: V2.0  
**发布日期**: 2024-01-15  
**兼容性**: 向后兼容 V1（需执行数据库迁移）
