# DouyinAdapter

## 1. Class Profile
- **Package**: `com.example.aikef.service.channel.douyin`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `Slf4j`, `RequiredArgsConstructor`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 抖音私信渠道适配器。负责处理抖音开放平台的消息交互，包括 Webhook 消息解析、验签以及通过 API 发送私信。

## 2. Method Deep Dive

### `parseMessage`
- **Signature**: `public Map<String, Object> parseMessage(String body)`
- **Description**: 将接收到的 JSON 格式消息体解析为 Map 结构。

### `toWebhookRequest`
- **Signature**: `public WebhookMessageRequest toWebhookRequest(Map<String, Object> message)`
- **Description**: 将抖音的原始消息格式转换为系统统一的 `WebhookMessageRequest`。
- **Status**: 待实现（占位）。

### `sendMessage`
- **Signature**: `public void sendMessage(OfficialChannelConfig config, String openId, String content, List<Attachment> attachments)`
- **Description**: 发送私信给抖音用户。
- **Logic**: 调用抖音开放平台 `/im/send/msg/` 接口。

### `verifySignature`
- **Signature**: `public boolean verifySignature(OfficialChannelConfig config, String signature, String timestamp, String nonce, String body)`
- **Description**: 验证抖音 Webhook 回调的签名合法性。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `RestTemplate`: HTTP 请求。
  - `ObjectMapper`: JSON 处理。

## 4. Usage Guide
### 场景：抖音私信接入
当用户在企业抖音号发起咨询时，消息通过 Webhook 推送到此适配器。系统解析后创建会话。客服的回复通过 `sendMessage` 接口调用抖音 API，以私信形式发送给用户。
