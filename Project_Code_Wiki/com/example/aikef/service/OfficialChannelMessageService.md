# OfficialChannelMessageService

## 1. Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `RequiredArgsConstructor`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 官方渠道消息服务。专门处理通过官方 SDK 或 API 直接集成的渠道（如 WeChat Official, WeChat Work/Kf, Line, WhatsApp Business API）。提供了特定于渠道的签名验证、消息解密和格式转换功能。

## 2. Method Deep Dive

### `verifyWechatWebhook` / `verifyWechatKfWebhook`
- **Signature**: `public ResponseEntity<String> verifyWechatWebhook(...)`
- **Description**: 处理微信服务器的 GET 验证请求（EchoStr 机制）。支持微信服务号和微信客服（企业微信）。

### `handleWechatMessage` / `handleWechatKfMessage`
- **Signature**: `public ResponseEntity<String> handleWechatMessage(...)`
- **Description**: 处理微信 POST 消息回调。
- **Logic**:
  1. 验证签名。
  2. (微信客服) 解密消息体。
  3. 解析 XML/JSON 消息内容。
  4. 处理特殊事件（如 `kf_msg_or_event` 同步事件）。
  5. 转换为通用 `WebhookMessageRequest` 并委托给 `ExternalPlatformService` 处理。

### `sendMessageToOfficialChannel`
- **Signature**: `public boolean sendMessageToOfficialChannel(UUID sessionId, String content, SenderType senderType, List<Attachment> attachments)`
- **Description**: 尝试通过官方 SDK 适配器发送消息。
- **Returns**: `true` 如果成功且是官方渠道；`false` 如果不是官方渠道或发送失败。
- **Logic**:
  1. 识别会话所属的渠道类型。
  2. 获取对应的配置 (`OfficialChannelConfig`)。
  3. switch-case 分发给具体的适配器（如 `wechatAdapter`, `lineAdapter`）。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `OfficialChannelConfigRepository`: 渠道配置。
  - `WechatOfficialAdapter`, `LineOfficialAdapter`, `WhatsappOfficialAdapter` 等: 具体渠道的底层实现类。
  - `ExternalPlatformService`: 通用消息处理逻辑。

## 4. Usage Guide
### 场景：微信客服接入
企业配置了微信客服功能。当用户在微信内咨询时：
1. 微信服务器推送加密 XML 到 `/api/v1/official/wechat-kf/message`。
2. `handleWechatKfMessage` 验证签名并解密。
3. 如果是 `kf_msg_or_event` 事件，主动调用 `wechatAdapter.syncMessages` 拉取具体消息。
4. 将拉取到的消息通过 `ExternalPlatformService` 存入系统。
