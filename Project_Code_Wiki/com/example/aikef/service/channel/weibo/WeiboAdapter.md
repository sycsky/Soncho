# WeiboAdapter

## 1. Class Profile
- **Package**: `com.example.aikef.service.channel.weibo`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `Slf4j`, `RequiredArgsConstructor`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 微博粉丝服务平台适配器。处理微博私信。

## 2. Method Deep Dive

### `parseMessage`
- **Signature**: `public Map<String, Object> parseMessage(String body)`
- **Description**: 解析微博消息 JSON。

### `toWebhookRequest`
- **Signature**: `public WebhookMessageRequest toWebhookRequest(Map<String, Object> message)`
- **Description**: 转换消息格式。
- **Status**: 待实现（占位）。

### `sendMessage`
- **Signature**: `public void sendMessage(OfficialChannelConfig config, String receiverId, String content, List<Attachment> attachments)`
- **Description**: 发送私信。
- **Logic**: 调用微博粉丝服务平台 `/messages/send.json` 接口。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `RestTemplate`: HTTP 请求。
  - `ObjectMapper`: JSON 处理。

## 4. Usage Guide
### 场景：微博私信
用户给企业蓝V账号发送私信。适配器接收 Webhook，解析内容并创建会话。
