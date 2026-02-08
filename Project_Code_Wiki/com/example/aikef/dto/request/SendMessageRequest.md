# SendMessageRequest

## 1. Class Profile
- **Class Name**: `SendMessageRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record`
- **Role**: Data Transfer Object (Request)
- **Purpose**: Payload for sending a message from an agent to a customer (or internal note).

## 2. Method Deep Dive
### Fields
- `sessionId`: Target chat session.
- `text`: Message body.
- `isInternal`: If true, only visible to agents.
- `attachments`: List of files.
- `mentions`: List of agent IDs to notify.

### Validation Logic
- `isContentValid()`: Custom validation ensuring that either `text` or `attachments` is present. You cannot send an empty message.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `AttachmentPayload`
- **External Dependencies**:
  - `jakarta.validation.constraints.*`

## 4. Usage Guide
Used by `MessageController`.
- **Endpoint**: `POST /api/v1/messages`

## 5. Source Link
[SendMessageRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/SendMessageRequest.java)
