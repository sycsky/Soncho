# Class Profile: OfficialChannelWebhookController

**File Path**: `com/example/aikef/controller/OfficialChannelWebhookController.java`
**Type**: Controller (`@RestController`)
**Purpose**: The central receiver for webhooks from external messaging platforms. It handles the specific verification protocols (e.g., WeChat's `echostr` challenge, Facebook's `hub.challenge`) and normalizes incoming message payloads before passing them to the message service.

# Method Deep Dive

## WeChat Handlers
- **`wechatWebhook(...)`**: Handles both GET (verification) and POST (message) requests for WeChat Official Accounts.
- **`wechatKfWebhook(...)`**: Handles WeChat Customer Service (WeChat KF) webhooks.

## International Platforms
- **`lineWebhook(...)`**: Validates `X-Line-Signature` and processes Line messages.
- **`whatsappWebhook(...)`**: Validates `X-Hub-Signature-256` and processes WhatsApp Business messages.
- **`facebookWebhook(...)`, `instagramWebhook(...)`**: Handles the Meta Graph API webhook verification and message cycle.
- **`telegramWebhook(...)`**: Processes Telegram updates.
- **`twitterWebhook(...)`**: Handles CRC checks (GET) and Account Activity API events (POST).

## Domestic Platforms
- **`douyinWebhook(...)`**, **`redBookWebhook(...)`**, **`weiboWebhook(...)`**: Handlers for Douyin, Little Red Book, and Weibo.

# Dependency Graph

**Core Dependencies**:
- `OfficialChannelMessageService`: Core logic for signature verification and message parsing.
- `OfficialChannelConfig`: Configuration lookup.

**Key Imports**:
```java
import com.example.aikef.service.OfficialChannelMessageService;
import org.springframework.web.bind.annotation.*;
```

# Usage Guide

These endpoints are configured in the respective developer consoles of the external platforms.

## Example: WeChat Verification
`GET /api/v1/official-channels/wechat_official/webhook?signature=...&timestamp=...&nonce=...&echostr=...`
- The controller verifies the signature using the configured token and returns `echostr`.

# Source Link
[OfficialChannelWebhookController.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/controller/OfficialChannelWebhookController.java)
