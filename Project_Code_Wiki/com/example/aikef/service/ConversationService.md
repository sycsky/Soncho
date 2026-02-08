# ConversationService

## Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: Class
- **Description**: Core service managing chat sessions, message flow, and conversation lifecycle.
- **Key Features**:
  - Send Message (User/Agent).
  - Message Routing (Internal vs External).
  - Translation Support.
  - WebSocket Notification Management.
  - Session Status Management.

## Method Deep Dive

### `sendMessage`
- **Description**: Handles sending a message within a session.
- **Signature**: `public MessageDto sendMessage(SendMessageRequest request, UUID agentId)`
- **Logic**:
  1. Loads session and author (Agent or User).
  2. Creates `Message` entity.
  3. Handles Attachments.
  4. **Translation**: Detects language if User; translates text if enabled.
  5. Persists message.
  6. **Delivery**: Creates `MessageDelivery` records for relevant agents; marks as sent/read based on presence.
  7. **Routing**: If external message, routes via `ChannelRouter` to the customer's channel.
  8. Returns `MessageDto`.

### `updateSessionStatus`
- **Description**: Updates the status of a chat session (e.g., to RESOLVED).
- **Signature**: `public ChatSession updateSessionStatus(UpdateSessionStatusRequest request)`
- **Logic**: Updates status based on action enum (RESOLVED, AI_HANDLING, HUMAN_HANDLING).

### `createMessageDeliveries`
- **Description**: Internal method to track which agents received the message.
- **Logic**:
  1. Identifies recipients (Primary Agent + Support Agents).
  2. Creates `MessageDelivery` records.
  3. Marks as `sent` if agent is online or is the sender.

## Dependency Graph
- **Injected Services**:
  - `ChatSessionRepository`, `MessageRepository`, `MessageDeliveryRepository`
  - `AgentService`
  - `ChannelRouter`: For outbound messages.
  - `WebSocketSessionManager`: For real-time status.
  - `TranslationService`: For auto-translation.
- **DTOs**:
  - `MessageDto`, `SendMessageRequest`

## Usage Guide
```java
// Sending a message
conversationService.sendMessage(new SendMessageRequest("Hello", ...), agentId);
```
