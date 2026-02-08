# SessionMessageGateway

## 1. Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `RequiredArgsConstructor`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 会话消息统一网关。作为系统发送消息的中心入口，统一处理消息的构建、持久化、WebSocket 广播以及向外部平台的转发。支持发送文本、图片、结构化消息等多种类型。

## 2. Method Deep Dive

### `sendMessage`
- **Signature**: `public Message sendMessage(UUID sessionId, String text, SenderType senderType, UUID agentId, Map<String, Object> metadata, boolean isInternal)`
- **Description**: 通用发送方法。
- **Logic**:
  1. **结构化处理**: 检查是否为 `struct#` 开头的结构化消息，如果是则调用 `sendStructuredMessage` 进行解析和转换。
  2. **消息创建**: 构建 `Message` 实体，设置发送者、内容、附件等。
  3. **翻译**: 如果启用了翻译服务，自动翻译消息内容并保存 `translationData`。
  4. **持久化**: 保存到数据库。
  5. **广播**: 调用 `broadcastMessage` 推送给 WebSocket 客户端。
  6. **转发**: 如果是客服/AI/系统消息，调用 `officialChannelMessageService` 或 `externalPlatformService` 转发给外部客户。

### `sendStructuredMessage`
- **Signature**: `private Message sendStructuredMessage(...)`
- **Description**: 处理复杂的结构化消息（如图文混排）。
- **Logic**: 解析 JSON，提取图片作为附件，合并文本内容，最终作为单条消息发送。

### `broadcastMessage`
- **Signature**: `private void broadcastMessage(ChatSession session, Message message)`
- **Description**: 将消息包装为 WebSocket 事件 (`newMessage`) 并推送到会话的所有参与者。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `MessageRepository`, `ChatSessionRepository`: 数据存储。
  - `WebSocketSessionManager`: 实时推送。
  - `TranslationService`: 翻译。
  - `OfficialChannelMessageService`, `ExternalPlatformService`: 外部发送渠道。

## 4. Usage Guide
### 场景：AI 回复
当 AI 工作流生成回复时，调用 `sendAiMessage`。Gateway 会：
1. 保存消息记录（Sender=AI）。
2. 在前端界面显示 AI 的回复气泡。
3. 通过 WhatsApp 接口将回复发送给用户的手机。
