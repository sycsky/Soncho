# Class Profile: WebhookController

**File Path**: `com/example/aikef/controller/WebhookController.java`
**Type**: Controller (`@RestController`)
**Purpose**: A generic, unified webhook receiver for third-party platforms. Unlike `OfficialChannelWebhookController` which is specific to "Official Accounts", this controller is designed for broader platform integrations (ExternalPlatform), allowing dynamic configuration of new platforms without code changes.

# Method Deep Dive

## Webhook Handling
- **`receiveMessage(platformName, request)`**:
  - **Endpoint**: `POST /api/v1/webhook/{platformName}/message`
  - **Logic**: Receives a standardized `WebhookMessageRequest`, finds the platform configuration by name, and delegates to `ExternalPlatformService` to process the message.
- **`verifyWebhook(platformName, ...)`**:
  - **Endpoint**: `GET /api/v1/webhook/{platformName}/verify`
  - **Purpose**: URL verification challenge (common in WeChat/Facebook). Returns `echostr` if present.

## Platform Management (CRUD)
- **`getAllPlatforms()`, `getPlatform(name)`**: Retrieval.
- **`createPlatform(...)`, `updatePlatform(...)`**: Configuration of new external integrations (callback URLs, auth types).

## Metadata
- **`getPlatformTypes()`**, **`getAuthTypes()`**: Helper endpoints for UI configuration forms.

# Dependency Graph

**Core Dependencies**:
- `ExternalPlatformService`: Logic for handling generic external platforms.
- `ExternalPlatform`: Entity model.

**Key Imports**:
```java
import com.example.aikef.service.ExternalPlatformService;
import com.example.aikef.model.ExternalPlatform;
import org.springframework.web.bind.annotation.*;
```

# Usage Guide

## Configuring a Custom Integration
`POST /api/v1/webhook/platforms`
```json
{
  "name": "my-crm",
  "platformType": "CUSTOM",
  "authType": "API_KEY",
  "authCredential": "xyz",
  "enabled": true
}
```

## Receiving a Message
`POST /api/v1/webhook/my-crm/message`
```json
{
  "threadId": "ticket-101",
  "content": "New comment on ticket",
  "userName": "Client A"
}
```

# Source Link
[WebhookController.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/controller/WebhookController.java)
