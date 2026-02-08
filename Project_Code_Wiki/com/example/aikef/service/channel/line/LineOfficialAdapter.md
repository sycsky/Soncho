# LineOfficialAdapter

## 1. Class Profile
- **Package**: `com.example.aikef.service.channel.line`
- **Type**: `Class`
- **Modifiers**: `public`, `Component`, `Slf4j`, `RequiredArgsConstructor`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: LINE 官方账号适配器。处理 LINE Messaging API 的 Webhook 事件和消息发送。

## 2. Method Deep Dive

### `verifySignature`
- **Signature**: `public boolean verifySignature(OfficialChannelConfig config, String body, String signature)`
- **Description**: 验证 Webhook 签名（HMAC-SHA256）。
- **Logic**: 使用 `channelSecret` 对请求体进行 HMAC-SHA256 计算，对比 `x-line-signature` 头。

### `parseMessage`
- **Signature**: `public Map<String, Object> parseMessage(String body)`
- **Description**: 解析 JSON 消息体。

### `toWebhookRequest`
- **Signature**: `public WebhookMessageRequest toWebhookRequest(Map<String, Object> lineMessage)`
- **Description**: 提取 `events` 数组中的第一个消息事件。
- **Mapping**: `source.userId` -> `threadId`/`externalUserId`。

### `sendMessage`
- **Signature**: `public void sendMessage(OfficialChannelConfig config, String userId, String content, List<Attachment> attachments)`
- **Description**: 发送 Push Message。
- **Logic**: 使用 `channelAccessToken` 调用 `/v2/bot/message/push` 接口。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `OfficialChannelService`: 获取配置。
  - `ObjectMapper`: JSON 处理。

## 4. Usage Guide
### 场景：LINE 客服
配置 LINE Developers Console 的 Webhook URL。当用户添加官方账号为好友并发送消息时，适配器接收并验证请求，将消息路由到客服系统。
