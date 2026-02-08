# WebhookMessageRequest

## 1. Class Profile
- **Class Name**: `WebhookMessageRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object / API Request
- **Purpose**: Generic payload for receiving messages from ANY external platform via webhook.

## 2. Method Deep Dive
### Fields
- `threadId`: External conversation ID (Mandatory).
- `content`: Message text (Mandatory).
- `messageType`: text/image/file (Default: text).
- `externalUserId`, `userName`, `email`: User info for auto-creation.
- `categoryId`: Optional routing hint.
- `language`: User language hint.

### Helper Methods
- `getMessageTypeOrDefault()`: Null-safe accessor.
- `getUserNameOrDefault()`: Smart fallback logic for display name.

## 3. Dependency Graph
- **Internal Dependencies**: None.

## 4. Usage Guide
Used by `WebhookController.handleMessage`.
- **Integration**: Third-party developers use this standard format to push messages into the agent system. The system automatically creates/finds the customer and session based on `threadId` and `externalUserId`.

## 5. Source Link
[WebhookMessageRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/WebhookMessageRequest.java)
