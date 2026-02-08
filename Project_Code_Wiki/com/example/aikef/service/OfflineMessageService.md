# OfflineMessageService

## 1. Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 离线消息服务。专门用于管理发送给离线客服的消息队列。当客服离线时，消息会被记录；当客服重新上线时，通过此服务拉取未送达的消息。

## 2. Method Deep Dive

### `getUnsentMessagesForAgent`
- **Signature**: `public List<ChatMessageDto> getUnsentMessagesForAgent(UUID agentId)`
- **Description**: 查询指定客服所有未标记为 "已发送" 的离线消息。
- **Returns**: `List<ChatMessageDto>` - 包含完整消息内容、附件和元数据的 DTO 列表。

### `markAsSentForAgent`
- **Signature**: `public void markAsSentForAgent(UUID agentId)`
- **Description**: 批量将指定客服的所有离线消息标记为 "已发送"（已投递）。
- **Logic**:
  1. 查找所有 `sent=false` 的 `MessageDelivery` 记录。
  2. 设置 `sent=true` 和 `sentAt=now`。
  3. 批量保存。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `MessageDeliveryRepository`: 消息投递记录存储。

## 4. Usage Guide
### 场景：客服断线重连
客服在使用过程中网络中断。期间系统产生了 5 条新消息。
当客服网络恢复并重新建立 WebSocket 连接时，前端会自动调用接口触发 `getUnsentMessagesForAgent`，获取这 5 条消息并渲染到界面上，确保消息不丢失。随后调用 `markAsSentForAgent` 确认接收。
