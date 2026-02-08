# TwitterAdapter

## 1. Class Profile
- **Package**: `com.example.aikef.service.channel.twitter`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `Slf4j`, `RequiredArgsConstructor`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: Twitter (X) Account Activity API 适配器。处理私信（Direct Message）交互。

## 2. Method Deep Dive

### `generateCrcResponse`
- **Signature**: `public String generateCrcResponse(OfficialChannelConfig config, String crcToken)`
- **Description**: 处理 Twitter 的 CRC (Challenge-Response Check) 验证请求。
- **Logic**: 使用 `consumerSecret` 对 `crc_token` 进行 HMAC-SHA256 计算，返回 `sha256=` 格式的 hash。

### `sendMessage`
- **Signature**: `public void sendMessage(OfficialChannelConfig config, String recipientId, String content, List<Attachment> attachments)`
- **Description**: 发送私信（DM）。
- **Status**: 待完善（需复杂的 OAuth 1.0a 签名支持）。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `RestTemplate`: HTTP 请求。
  - `ObjectMapper`: JSON 处理。

## 4. Usage Guide
### 场景：Twitter DM 客服
配置 Account Activity API。Twitter 定期发送 CRC 请求验证服务器所有权。当用户发送 DM 时，WebHook 触发，适配器处理消息。
