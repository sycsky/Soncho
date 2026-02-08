# ReadRecordService

## 1. Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `Transactional(readOnly = true)`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 已读记录服务。核心功能是追踪每个客服在每个会话中的阅读进度（Last Read Time），并据此计算未读消息数和未读 @ 提醒数。

## 2. Method Deep Dive

### `updateReadTime`
- **Signature**: `public void updateReadTime(UUID sessionId, UUID agentId)`
- **Description**: 更新客服在指定会话的最后阅读时间为当前时间。通常在客服点开会话窗口时触发。

### `getUnreadCount`
- **Signature**: `public int getUnreadCount(UUID sessionId, UUID agentId)`
- **Description**: 计算单个会话的未读消息数。
- **Logic**: `count(messages) WHERE sessionId = ? AND createdAt > lastReadTime`。

### `getUnreadCountBatch`
- **Signature**: `public Map<UUID, Integer> getUnreadCountBatch(List<UUID> sessionIds, UUID agentId)`
- **Description**: 批量计算多个会话的未读数，优化数据库查询性能。

### `getMentionUnreadCount` / `getMentionUnreadCountBatch`
- **Signature**: `public int getMentionUnreadCount(UUID sessionId, UUID agentId)`
- **Description**: 计算未读的 @ 提醒数量。
- **Logic**: 统计在最后阅读时间之后创建的 `AgentMention` 记录。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `ReadRecordRepository`: 阅读记录存储。
  - `MessageRepository`: 消息计数查询。
  - `AgentMentionRepository`: @ 记录计数查询。

## 4. Usage Guide
### 场景：未读红点展示
1. 客服登录后，侧边栏会话列表显示每个会话的未读数（调用 `getUnreadCountBatch`）。
2. 当有新消息进入时，如果是当前未打开的会话，未读数 +1。
3. 当客服点击该会话，调用 `updateReadTime`，未读数清零。
