# EmailAdapter

## 1. Class Profile
- **Package**: `com.example.aikef.service.channel.email`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `Slf4j`, `RequiredArgsConstructor`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 邮件渠道适配器。用于将电子邮件作为客户咨询渠道接入系统。支持邮件内容的解析和回复发送。

## 2. Method Deep Dive

### `parseMessage`
- **Signature**: `public Map<String, Object> parseMessage(String body)`
- **Description**: 解析接收到的邮件内容（通常来自邮件服务商的 Webhook，如 SendGrid/Mailgun）。

### `sendMessage`
- **Signature**: `public void sendMessage(OfficialChannelConfig config, String toEmail, String content, List<Attachment> attachments)`
- **Description**: 发送回复邮件。
- **Logic**: 从 `OfficialChannelConfig` 中读取 SMTP 配置（host, port, auth），使用 JavaMail 或第三方 API 发送邮件。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `ObjectMapper`: JSON 处理。

## 4. Usage Guide
### 场景：邮件工单
客户发送邮件到 `support@example.com`。邮件服务商（如 SendGrid）收到邮件后触发 Webhook。`EmailAdapter` 解析 Webhook 内容，提取发件人、主题和正文，并在客服系统中创建一个新的聊天会话。客服的回复将作为邮件回复发送给客户。
