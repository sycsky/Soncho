# RedBookAdapter

## 1. Class Profile
- **Package**: `com.example.aikef.service.channel.redbook`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `Slf4j`, `RequiredArgsConstructor`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 小红书（RedBook）专业号消息适配器。处理私信消息的接收与回复。

## 2. Method Deep Dive

### `parseMessage`
- **Signature**: `public Map<String, Object> parseMessage(String body)`
- **Description**: 解析小红书推送的消息 JSON。

### `toWebhookRequest`
- **Signature**: `public WebhookMessageRequest toWebhookRequest(Map<String, Object> message)`
- **Description**: 转换消息格式。
- **Status**: 待实现（占位）。

### `sendMessage`
- **Signature**: `public void sendMessage(OfficialChannelConfig config, String userId, String content, List<Attachment> attachments)`
- **Description**: 发送私信。
- **Logic**: 调用小红书开放平台私信接口。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `RestTemplate`: HTTP 请求。
  - `ObjectMapper`: JSON 处理。

## 4. Usage Guide
### 场景：小红书种草咨询
用户在品牌小红书笔记下点击"咨询"，发送私信。适配器接收消息，客服在系统内回复，实现闭环服务。
