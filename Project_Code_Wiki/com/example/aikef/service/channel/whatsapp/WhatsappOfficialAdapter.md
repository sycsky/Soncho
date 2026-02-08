# WhatsappOfficialAdapter

## 1. Class Profile
- **Package**: `com.example.aikef.service.channel.whatsapp`
- **Type**: `Class`
- **Modifiers**: `public`, `Component`, `Slf4j`, `RequiredArgsConstructor`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: WhatsApp Business API (Cloud API) 适配器。基于 Meta 提供的 Cloud API 接口实现消息收发。

## 2. Method Deep Dive

### `verifySignature`
- **Signature**: `public boolean verifySignature(OfficialChannelConfig config, String body, String signature)`
- **Description**: 验证 Webhook 签名（HMAC-SHA256）。
- **Logic**: 使用 `appSecret` 计算 body 的 HMAC-SHA256，对比 `x-hub-signature-256`。

### `parseMessage`
- **Signature**: `public Map<String, Object> parseMessage(String body)`
- **Description**: 解析 JSON 消息体。

### `toWebhookRequest`
- **Signature**: `public WebhookMessageRequest toWebhookRequest(Map<String, Object> whatsappMessage)`
- **Description**: 提取深层嵌套的 JSON 结构中的消息内容。
- **Path**: `entry[0].changes[0].value.messages[0]`。
- **Mapping**: `from` -> `threadId`/`externalUserId`。

### `sendMessage`
- **Signature**: `public void sendMessage(OfficialChannelConfig config, String phoneNumber, String content, List<Attachment> attachments)`
- **Description**: 发送消息。
- **Logic**: 使用 `phoneNumberId` 和 `accessToken` 调用 Graph API (`/messages`)。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `OfficialChannelService`: 配置管理。
  - `ObjectMapper`: JSON 处理。

## 4. Usage Guide
### 场景：WhatsApp 营销
系统向客户发送 WhatsApp 营销消息。客户回复后，消息推送到 Webhook。适配器验证签名，解析消息内容（文本、图片等），并将其转交给客服系统进行处理。
