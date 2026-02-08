# QuickReplyService

## 1. Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `Transactional(readOnly = true)`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 快捷回复服务。管理客服常用的预设回复语（Canned Responses）。支持系统级（所有客服可见）和个人级（仅创建者可见）两种类型的回复。

## 2. Method Deep Dive

### `getAllReplies`
- **Signature**: `public List<QuickReplyDto> getAllReplies(UUID agentId)`
- **Description**: 获取指定客服可用的所有快捷回复。
- **Logic**:
  1. 查询所有 `system=true` 的回复。
  2. 查询所有 `createdBy=agentId` 的回复。
  3. 合并并返回。

### `createReply`
- **Signature**: `public QuickReplyDto createReply(String label, String text, String category, UUID agentId, Boolean system)`
- **Description**: 创建新的快捷回复。

### `updateReply`
- **Signature**: `public QuickReplyDto updateReply(UUID id, String label, String text, String category, UUID agentId)`
- **Description**: 更新快捷回复。
- **Constraints**: 只能修改非系统预设且由自己创建的回复。

### `deleteReply`
- **Signature**: `public void deleteReply(UUID id, UUID agentId)`
- **Description**: 删除快捷回复。同样受权限限制。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `QuickReplyRepository`: 数据存储。
  - `AgentService`: 客服验证。
  - `EntityMapper`: DTO 转换。

## 4. Usage Guide
### 场景：提高回复效率
客服在聊天输入框输入 "/"，前端通过 `getAllReplies` 获取的列表展示候选项（如 "/hi" -> "您好，请问有什么可以帮您？"）。客服选择后，文本自动填充到输入框，极大提升常见问题的回复速度。
