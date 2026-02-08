# TelegramAdapter

## 1. Class Profile
- **Package**: `com.example.aikef.service.channel.telegram`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `Slf4j`, `RequiredArgsConstructor`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: Telegram Bot API 适配器。支持 Webhook 模式的消息接收和消息发送。

## 2. Method Deep Dive

### `parseMessage`
- **Signature**: `public Map<String, Object> parseMessage(String body)`
- **Description**: 解析 `Update` 对象 JSON。

### `toWebhookRequest`
- **Signature**: `public WebhookMessageRequest toWebhookRequest(Map<String, Object> update)`
- **Description**: 从 `Update` 对象中提取 `message`。
- **Mapping**: `message.chat.id` -> `threadId`, `message.from.username` -> `userName`。

### `sendMessage`
- **Signature**: `public void sendMessage(OfficialChannelConfig config, String chatId, String content, List<Attachment> attachments)`
- **Description**: 发送消息。
- **Logic**: 使用 `botToken` 调用 `https://api.telegram.org/bot{token}/sendMessage` 接口。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `RestTemplate`: HTTP 请求。
  - `ObjectMapper`: JSON 处理。

## 4. Usage Guide
### 场景：Telegram Bot 客服
创建 Telegram Bot 并设置 Webhook。用户与 Bot 对话时，消息实时推送到适配器。客服回复后，Bot 将消息发回给用户。
