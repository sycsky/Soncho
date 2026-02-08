# WechatOfficialAdapter

## 1. Class Profile
- **Package**: `com.example.aikef.service.channel.wechat`
- **Type**: `Class`
- **Modifiers**: `public`, `Component`, `Slf4j`, `RequiredArgsConstructor`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 微信生态综合适配器。同时支持 **微信服务号**（基于公众号接口）和 **微信客服**（基于企业微信接口）。处理消息接收、验签、解密、同步和发送。

## 2. Method Deep Dive

### `getAccessToken`
- **Signature**: `public String getAccessToken(OfficialChannelConfig config)`
- **Description**: 获取并缓存 Access Token。
- **Logic**: 根据配置的 `appId` 和 `appSecret` 调用微信接口。使用 `ConcurrentHashMap` 进行内存缓存，自动处理过期。

### `syncMessages`
- **Signature**: `public SyncResult syncMessages(OfficialChannelConfig config, String token, String cursor)`
- **Description**: 主动拉取微信客服消息。
- **Logic**: 当收到 `kf_msg_or_event` 事件时调用。循环调用 `/cgi-bin/kf/sync_msg` 接口，直到 `has_more=0`。

### `verifySignature`
- **Signature**: `public boolean verifySignature(...)`
- **Description**: 验证微信签名（SHA1）。支持普通验证和带 `echostr` 的验证。

### `decryptMessage`
- **Signature**: `public String decryptMessage(OfficialChannelConfig config, String encrypt)`
- **Description**: 调用 `WechatAesUtil` 解密消息。

### `parseMessage`
- **Signature**: `public Map<String, Object> parseMessage(String body, OfficialChannelConfig config)`
- **Description**: 解析 XML 消息体为 Map。

### `sendKfMessage`
- **Signature**: `public void sendKfMessage(...)`
- **Description**: 发送消息到微信客服。
- **Logic**: 调用 `/cgi-bin/kf/send_msg`。

### `sendMessage`
- **Signature**: `public void sendMessage(...)`
- **Description**: 发送消息到微信服务号（客服消息接口）。
- **Logic**: 调用 `/cgi-bin/message/custom/send`。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `OfficialChannelService`: 配置管理。
  - `WechatAesUtil`: 加解密。
  - `RestTemplate`: HTTP 请求。

## 4. Usage Guide
### 场景：微信客服同步
1. 微信推送事件通知有新消息。
2. 适配器验证签名并解密事件。
3. 识别为同步事件，调用 `syncMessages`。
4. 获取消息列表，转换为 `WebhookMessageRequest` 列表返回给上层服务处理。
