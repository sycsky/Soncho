# OfficialChannelConfig

## 1. Class Profile
- **Class Name**: `OfficialChannelConfig`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Configuration Entity
- **Purpose**: Stores credentials and settings for third-party messaging platforms (WeChat, WhatsApp, Line, etc.).

## 2. Method Deep Dive
### Fields
- `channelType`: Enum identifier (e.g., `WECHAT_OFFICIAL`).
- `displayName`: Friendly name.
- `enabled`: Toggle switch.
- `configJson`: Encrypted JSON blob containing platform-specific keys (AppID, Secret, Tokens).
- `webhookSecret`: Secret token used to verify incoming webhook signatures.
- `webhookUrl`: The system endpoint provided to the third-party platform.
- `categoryId`: Default session category for chats started via this channel.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.base.AuditableEntity`: Base class.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
This entity manages the "connection" to the outside world.
- **Setup**: Admin configures "WeChat" in the settings. This record is created/updated.
- **Runtime**: When the `WechatOfficialAdapter` needs to send a message, it loads this config to get the `access_token`.

## 5. Source Link
[OfficialChannelConfig.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/OfficialChannelConfig.java)
