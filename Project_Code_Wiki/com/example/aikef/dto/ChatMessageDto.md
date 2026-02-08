# ChatMessageDto

## 1. Class Profile
- **Class Name**: `ChatMessageDto`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: Represents a chat message sent to the frontend client (Customer or Agent UI). It includes UI-specific flags and metadata.

## 2. Method Deep Dive
### Fields
- `id`: Message UUID.
- `sessionId`: The session this message belongs to.
- `senderType`: `CUSTOMER`, `AGENT`, or `SYSTEM`.
- `agentId` / `agentName`: Details if sent by an agent.
- `text`: Message content.
- `internal`: Flag for internal notes (invisible to customer).
- `isMine`: Boolean flag indicating if the current user sent this message (computed at runtime).
- `translationData`: Map containing translated content (if applicable).
- `mentionAgentIds`: List of agents mentioned in the message.
- `attachments`: List of file attachments.
- `agentMetadata`: **Crucial Field**. Contains hidden metadata visible ONLY to agents (e.g., sentiment score, debug info), never sent to customers.
- `createdAt`: Timestamp.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.enums.SenderType`
  - `com.example.aikef.dto.AttachmentDto`

## 4. Usage Guide
Used by `ChatController` and WebSocket handlers to push messages to clients.
- **UI Rendering**: The frontend uses `isMine` to align messages (left/right).
- **Security**: The backend must ensure `agentMetadata` is stripped or null when sending to a `CUSTOMER` client (though the field definition suggests it's designed to hold agent-visible data, the filtering logic usually happens in the service/controller layer).

## 5. Source Link
[ChatMessageDto.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/ChatMessageDto.java)
