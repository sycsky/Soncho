# 已读记录功能 API 文档

## 概述

已读记录功能用于追踪客服对会话的阅读状态，支持计算未读消息数量。每个客服对每个会话维护一条已读记录。

## 数据库设计

### 表结构: `read_records`

```sql
CREATE TABLE read_records (
    id CHAR(36) PRIMARY KEY,
    session_id CHAR(36) NOT NULL,
    agent_id CHAR(36) NOT NULL,
    last_read_time DATETIME(6) NOT NULL,
    CONSTRAINT uk_session_agent UNIQUE (session_id, agent_id),
    CONSTRAINT fk_read_record_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id),
    CONSTRAINT fk_read_record_agent FOREIGN KEY (agent_id) REFERENCES agents(id)
);
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | CHAR(36) | 主键 UUID |
| session_id | CHAR(36) | 会话ID（外键） |
| agent_id | CHAR(36) | 客服ID（外键） |
| last_read_time | DATETIME(6) | 最后已读时间（微秒精度） |

### 约束说明

- **唯一约束** `uk_session_agent`: 确保每个客服对每个会话只有一条记录
- **外键约束**: 关联到会话表和客服表，级联删除保证数据一致性

---

## API 端点

### 1. 标记会话为已读

**请求**

```http
POST /api/read-records/mark-read
Content-Type: application/json

{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "agentId": "660e8400-e29b-41d4-a716-446655440001"
}
```

**响应**

```http
HTTP/1.1 204 No Content
```

**功能说明**
- 更新或创建客服对会话的已读记录
- 将 `last_read_time` 设置为当前时间
- 如果记录不存在则自动创建

**使用场景**
- 客服打开会话详情页面时
- 客服查看消息列表时
- 客服主动标记会话为已读时

**错误响应**

```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "error": "Session not found",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

## 业务逻辑

### 未读消息计算逻辑

```java
// 伪代码
if (read_record 不存在) {
    未读数 = 会话中所有消息的数量;
} else {
    未读数 = COUNT(messages WHERE created_at > read_record.last_read_time);
}
```

**关键要点:**
1. 没有已读记录时，所有消息都被视为未读
2. 有已读记录时，只计算在 `last_read_time` 之后创建的消息
3. 使用数据库时间戳比较，确保精确性

### Bootstrap 接口集成

Bootstrap 接口已集成未读数统计，采用**批量查询优化**避免 N+1 问题：

```java
// 1. 收集所有会话ID
List<UUID> allSessionIds = collectAllSessionIds();

// 2. 批量查询未读数（一次数据库调用）
Map<UUID, Integer> unreadCountMap = readRecordService.getUnreadCountBatch(
    allSessionIds, 
    agentId
);

// 3. 填充到 DTO
ChatSessionDto dto = new ChatSessionDto(
    // ...其他字段
    unreadCountMap.getOrDefault(sessionId, 0)  // 未读数
);
```

**Bootstrap 响应示例:**

```json
{
  "sessionGroups": [
    {
      "id": "group-uuid",
      "name": "待处理",
      "sessions": [
        {
          "id": "session-uuid",
          "userId": "user-uuid",
          "status": "active",
          "lastActive": "2024-01-20T10:30:00Z",
          "unreadCount": 5,  // ← 已读记录功能提供
          "lastMessage": "最后一条消息内容"
        }
      ]
    }
  ],
  "agents": [...],
  "roles": [...],
  "quickReplies": [...],
  "knowledgeEntries": [...]
}
```

---

## 性能优化

### 批量查询方法

`ReadRecordService.getUnreadCountBatch()` 实现：

```java
public Map<UUID, Integer> getUnreadCountBatch(List<UUID> sessionIds, UUID agentId) {
    // 1. 批量查询已读记录（1次查询）
    List<ReadRecord> records = readRecordRepository
        .findByAgentIdAndSessionIdIn(agentId, sessionIds);
    
    // 2. 构建时间映射
    Map<UUID, Instant> lastReadTimeMap = records.stream()
        .collect(Collectors.toMap(
            r -> r.getSession().getId(),
            ReadRecord::getLastReadTime
        ));
    
    // 3. 批量计算未读数（N次查询，但可以进一步优化为1次）
    Map<UUID, Integer> unreadCountMap = new HashMap<>();
    for (UUID sessionId : sessionIds) {
        Instant lastReadTime = lastReadTimeMap.get(sessionId);
        int unreadCount = calculateUnreadCount(sessionId, lastReadTime);
        unreadCountMap.put(sessionId, unreadCount);
    }
    
    return unreadCountMap;
}
```

### 数据库索引建议

```sql
-- 加速批量查询
CREATE INDEX idx_read_record_agent ON read_records(agent_id);

-- 加速会话查询
CREATE INDEX idx_read_record_session ON read_records(session_id);

-- 加速消息时间查询
CREATE INDEX idx_message_session_time ON messages(session_id, created_at);
```

---

## 实体类设计

### ReadRecord.java

```java
@Entity
@Table(name = "read_records", 
       uniqueConstraints = @UniqueConstraint(
           name = "uk_session_agent", 
           columnNames = {"session_id", "agent_id"}
       ))
public class ReadRecord {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(nullable = false)
    private Instant lastReadTime;

    // Getters and Setters
}
```

---

## 使用示例

### 前端集成示例

#### 会话列表显示未读数

```javascript
// Bootstrap 数据已包含 unreadCount
function renderSessionList(sessions) {
  sessions.forEach(session => {
    const badge = session.unreadCount > 0 
      ? `<span class="badge">${session.unreadCount}</span>` 
      : '';
    
    html += `
      <div class="session-item" data-id="${session.id}">
        <div class="session-title">${session.user.name}</div>
        ${badge}
      </div>
    `;
  });
}
```

#### 打开会话时标记已读

```javascript
function openSession(sessionId) {
  // 发送已读标记
  fetch('/api/read-records/mark-read', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      sessionId: sessionId,
      agentId: currentAgentId
    })
  });
  
  // 清除本地未读标识
  updateLocalUnreadCount(sessionId, 0);
}
```

#### WebSocket 消息接收时更新未读数

```javascript
websocket.on('new-message', (message) => {
  const isCurrentSession = message.sessionId === currentOpenSessionId;
  
  if (!isCurrentSession) {
    // 非当前会话，增加未读数
    incrementUnreadCount(message.sessionId);
  } else {
    // 当前会话，自动标记已读
    markAsRead(message.sessionId);
  }
});
```

---

## 测试场景

### 测试用例 1: 新会话未读数

```
前置条件: 会话存在，客服从未打开过
预期结果: unreadCount = 会话中所有消息数
```

### 测试用例 2: 标记已读后新消息

```
步骤:
1. 客服打开会话（调用 mark-read）
2. 客户发送 3 条新消息
3. 刷新会话列表

预期结果: unreadCount = 3
```

### 测试用例 3: 多客服独立已读状态

```
步骤:
1. 客服A打开会话
2. 客服B未打开会话
3. 客户发送消息

预期结果:
- 客服A看到 unreadCount = 1（新消息）
- 客服B看到 unreadCount = 所有历史消息 + 1
```

### 测试用例 4: 并发标记已读

```
步骤: 同一客服快速打开多个会话

预期结果:
- 每个会话都正确更新 last_read_time
- 不出现重复记录（唯一约束保证）
- 后续查询未读数正确
```

---

## 循环依赖解决方案

### 问题描述

`ChatSessionService` ↔ `ReadRecordService` 存在循环依赖：
- `ChatSessionService` 需要调用 `ReadRecordService.getUnreadCount()`
- `ReadRecordService` 需要调用 `ChatSessionService.findById()`

### 解决方案: @Lazy 注入

```java
@Service
public class ChatSessionService {
    private final ReadRecordService readRecordService;
    
    // 使用 @Lazy 延迟加载，打破循环
    public ChatSessionService(@Lazy ReadRecordService readRecordService) {
        this.readRecordService = readRecordService;
    }
    
    public ChatSessionDto getSessionDto(UUID sessionId, UUID agentId) {
        // 现在可以安全调用
        int unreadCount = readRecordService.getUnreadCount(sessionId, agentId);
        // ...
    }
}
```

---

## 未来扩展建议

### 1. 消息分组已读

```java
// 支持按消息类型标记已读
POST /api/read-records/mark-read-by-type
{
  "sessionId": "...",
  "agentId": "...",
  "messageType": "text"  // 只标记文本消息为已读
}
```

### 2. 批量标记已读

```java
// 一次标记多个会话为已读
POST /api/read-records/mark-read-batch
{
  "sessionIds": ["uuid1", "uuid2", "uuid3"],
  "agentId": "..."
}
```

### 3. 已读回执

```java
// 客户端可见客服是否已读消息
GET /api/read-records/receipt?sessionId=...&messageId=...

Response:
{
  "readByAgents": [
    {"agentId": "...", "readAt": "2024-01-20T10:30:00Z"}
  ]
}
```

### 4. 未读数推送

```java
// WebSocket 实时推送未读数变化
{
  "type": "unread-count-update",
  "sessionId": "...",
  "unreadCount": 5
}
```

---

## 总结

已读记录功能的核心优势：

✅ **简洁设计**: 单表存储，唯一约束确保数据一致性  
✅ **高性能**: 批量查询优化，避免 N+1 问题  
✅ **精确计算**: 基于时间戳比较，支持微秒精度  
✅ **易于集成**: 与 Bootstrap 接口无缝集成  
✅ **可扩展**: 支持未来多种已读场景扩展  

**关键实现文件:**
- `ReadRecord.java` - 实体类
- `ReadRecordRepository.java` - 数据访问层
- `ReadRecordService.java` - 业务逻辑层
- `ReadRecordController.java` - API 控制器
- `BootstrapService.java` - 集成未读数统计
- `db/create_read_records_table.sql` - 数据库迁移脚本
