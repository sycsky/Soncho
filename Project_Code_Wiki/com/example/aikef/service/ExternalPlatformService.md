# ExternalPlatformService

## 1. Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `RequiredArgsConstructor`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 第三方平台集成核心服务。充当统一的网关，处理来自 WhatsApp, Line, Email 等外部渠道的消息接收，以及向这些渠道发送消息。负责消息的标准化、会话映射管理和 AI 工作流触发。

## 2. Method Deep Dive

### `handleWebhookMessage`
- **Signature**: `public WebhookMessageResponse handleWebhookMessage(String platformName, WebhookMessageRequest request)`
- **Description**: 处理来自外部平台的入站 Webhook 消息。
- **Logic**:
  1. **验证**: 检查平台配置是否存在且启用。
  2. **映射**: 调用 `findOrCreateMapping` 关联外部用户 ID 和内部系统客户/会话。
  3. **语言处理**: 检测或更新客户的语言偏好。
  4. **消息创建**: 将外部消息转换为内部 `Message` 实体并保存（支持翻译）。
  5. **广播**: 通过 WebSocket 通知前端客服。
  6. **AI 触发**: 注册事务同步回调，在事务提交后触发 AI 工作流处理该消息。

### `forwardMessageToExternalPlatform`
- **Signature**: `public void forwardMessageToExternalPlatform(UUID sessionId, String content, SenderType senderType, List<Attachment> attachments)`
- **Annotations**: `@Async`
- **Description**: 将系统内部产生的消息（客服回复或 AI 回复）异步转发到对应的外部平台。
- **Logic**:
  1. 查找会话对应的 `ExternalSessionMapping`。
  2. 检查平台回调 URL 配置。
  3. **翻译**: 如果发送者是客服/AI，尝试将内容翻译为客户的目标语言。
  4. **构建请求**: 封装标准化的 JSON 请求体，包含文本、附件等。
  5. **发送**: 使用 `RestTemplate` 发送 HTTP POST 请求到外部适配器/平台。

### `findOrCreateMapping`
- **Signature**: `private ExternalSessionMapping findOrCreateMapping(...)`
- **Description**: 维护 `ExternalPlatform` <-> `Customer` <-> `ChatSession` 的绑定关系。如果是不存在的外部用户，会自动创建新客户和新会话。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `ExternalPlatformRepository`, `ExternalSessionMappingRepository`: 平台与映射存储。
  - `CustomerRepository`, `ChatSessionRepository`, `MessageRepository`: 核心业务数据。
  - `TranslationService`: 多语言翻译支持。
  - `WebSocketEventService`: AI 工作流触发入口。
  - `OfficialChannelMessageService`: 官方渠道适配器（循环依赖，使用 @Lazy）。

## 4. Usage Guide
### 场景：WhatsApp 消息收发
1. **接收**: WhatsApp Webhook -> `WebhookController` -> `ExternalPlatformService.handleWebhookMessage` -> 保存消息 -> 触发 AI。
2. **发送**: 客服在界面回复 -> `SessionMessageGateway` -> `ExternalPlatformService.forwardMessageToExternalPlatform` -> 调用 WhatsApp API 发送给用户。
