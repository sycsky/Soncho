# MessageDto

## 1. Class Profile
- **Class Name**: `MessageDto`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record`
- **Role**: Data Transfer Object
- **Purpose**: Represents a single chat message in the history list. Includes text, attachments, translation data, and mentions.

## 2. Method Deep Dive
### Fields
- `id`: Message ID.
- `senderType`: `CUSTOMER`, `AGENT`, `AI`, `SYSTEM`.
- `agentId`: Sender ID (if agent).
- `text`: Message content.
- `internal`: `true` if it's a private note/thought.
- `translationData`: Map containing translations (e.g., `{"en": "Hello", "zh": "你好"}`).
- `mentions`: List of mentioned agent UUIDs.
- `attachments`: List of files/images attached.
- `agentMetadata`: Extra info about the sender agent (e.g., name, avatar).
- `createdAt`: Timestamp.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `AttachmentDto`: Nested file info.
  - `com.example.aikef.model.enums.SenderType`: Sender enum.

## 4. Usage Guide
Used by `MessageController` and WebSocket events.
- **History**: `GET /api/v1/sessions/{id}/messages` returns a list of these.
- **Real-time**: Pushed to the frontend via WebSocket when a new message arrives.

## 5. Source Link
[MessageDto.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/MessageDto.java)
