# AgentMentionService

## 1. Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `Transactional`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 处理客服在会话中被提及（@Mention）的业务逻辑服务。负责创建 @ 记录、查询未读的 @ 消息以及统计未读数量。

## 2. Method Deep Dive

### `createMention`
- **Signature**: `public AgentMention createMention(UUID agentId, UUID sessionId, Message message)`
- **Description**: 创建一条新的 @ 记录。
- **Parameters**:
  - `agentId`: 被 @ 的客服 ID。
  - `sessionId`: 会话 ID。
  - `message`: 关联的消息实体。
- **Returns**: `AgentMention` - 创建的记录。

### `createMentions`
- **Signature**: `public void createMentions(List<UUID> agentIds, UUID sessionId, Message message)`
- **Description**: 批量创建 @ 记录，用于一次消息 @ 多位客服的场景。
- **Parameters**:
  - `agentIds`: 被 @ 的客服 ID 列表。
  - `sessionId`: 会话 ID。
  - `message`: 关联的消息。

### `getUnreadMentions`
- **Signature**: `public List<AgentMention> getUnreadMentions(UUID agentId)`
- **Description**: 获取指定客服所有未读的 @ 记录。
- **Returns**: `List<AgentMention>`

### `getUnreadMentionCount`
- **Signature**: `public long getUnreadMentionCount(UUID agentId)`
- **Description**: 统计指定客服的未读 @ 总数。

### `markAllAsRead`
- **Signature**: `public int markAllAsRead(UUID agentId)`
- **Description**: 将指定客服的所有 @ 记录标记为已读。

### `markAsReadBySession`
- **Signature**: `public int markAsReadBySession(UUID agentId, UUID sessionId)`
- **Description**: 将指定客服在特定会话中的所有 @ 记录标记为已读。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `AgentMentionRepository`: 数据访问层。
  - `AgentRepository`: 客服数据访问。
  - `ChatSessionRepository`: 会话数据访问。

## 4. Usage Guide
### 场景：支持客服收到通知
当主责客服在群聊中发送消息 "@技术支持 请协助查看此问题" 时，WebSocket 服务解析消息中的 mentions 字段，调用 `createMentions`。随后，被 @ 的客服前端会收到通知，并看到未读 @ 计数增加。
