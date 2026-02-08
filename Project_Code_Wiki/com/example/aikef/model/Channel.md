# Channel

## 1. Class Profile
- **Class Name**: `Channel`
- **Package**: `com.example.aikef.model`
- **Type**: `Enum`
- **Role**: Enumeration
- **Purpose**: Lists all supported communication channels.

## 2. Method Deep Dive
### Values
- `WEB`: Website widget.
- `WECHAT`: WeChat Official Account.
- `WHATSAPP` / `LINE` / `TELEGRAM`: Messaging apps.
- `EMAIL`: Email integration.
- `CUSTOM`: Generic webhook.
- ... and others (`DOUYIN`, `REDBOOK`).

## 3. Usage Guide
Used in `Customer` entity to denote `primaryChannel` and in `OfficialChannelConfig`.

## 4. Source Link
[Channel.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/Channel.java)
