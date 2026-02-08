# FacebookAdapter

## 1. Class Profile
- **Package**: `com.example.aikef.service.channel.facebook`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `Slf4j`, `RequiredArgsConstructor`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: Facebook Messenger 和 Instagram Direct 消息适配器。基于 Meta Graph API 实现。

## 2. Method Deep Dive

### `verifyWebhook`
- **Signature**: `public boolean verifyWebhook(OfficialChannelConfig config, String mode, String token, String challenge)`
- **Description**: 处理 Meta 的 Webhook 验证请求（Hub Challenge）。
- **Logic**: 验证 `verify_token` 是否与配置中的 `webhookSecret` 匹配。

### `parseMessage`
- **Signature**: `public Map<String, Object> parseMessage(String body)`
- **Description**: 解析 Meta 发送的 JSON 消息体。

### `toWebhookRequest`
- **Signature**: `public WebhookMessageRequest toWebhookRequest(Map<String, Object> message)`
- **Description**: 提取 `messaging` 事件中的 `sender.id` 和 `message.text`，转换为标准格式。

### `sendMessage`
- **Signature**: `public void sendMessage(OfficialChannelConfig config, String recipientId, String content, List<Attachment> attachments)`
- **Description**: 发送回复消息。
- **Logic**: 使用 Page Access Token 调用 Graph API (`/me/messages`)。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `RestTemplate`: HTTP 请求。
  - `ObjectMapper`: JSON 处理。

## 4. Usage Guide
### 场景：FB 主页消息
用户在企业 Facebook 主页发送消息。适配器接收 Webhook，验证签名，提取消息内容。客服回复时，系统通过 Page Access Token 调用 API 将消息推送到用户的 Messenger。
