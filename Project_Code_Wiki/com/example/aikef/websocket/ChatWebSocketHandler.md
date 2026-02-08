# ChatWebSocketHandler

## Class Profile
`ChatWebSocketHandler` is the main handler for the WebSocket communication in the application. It extends Spring's `TextWebSocketHandler` to manage lifecycle events (connection established, message received, connection closed) for both agents and customers.

## Method Deep Dive

### `afterConnectionEstablished(WebSocketSession session)`
- **Logic**:
    1.  Retrieves `AgentPrincipal` or `CustomerPrincipal` from session attributes (set by handshake interceptor).
    2.  Registers the session with `WebSocketSessionManager`.
    3.  **For Agents**: Pushes any pending offline messages via `pushOfflineMessagesToAgent`.

### `handleTextMessage(WebSocketSession session, TextMessage message)`
- **Logic**:
    1.  Parses the incoming JSON payload into a `WebSocketEnvelope` (event/payload structure).
    2.  Validates the presence of the `event` field.
    3.  Delegates the actual business logic to `WebSocketEventService.handle()`.
    4.  Sends the response back to the client (unless it's a broadcast event like `sendMessage` which is handled separately).

### `pushOfflineMessagesToAgent(...)`
- **Logic**:
    1.  Fetches unsent messages from `OfflineMessageService`.
    2.  Iterates and sends each as a `newMessage` event.
    3.  Sends a completion event `offline_messages_complete`.
    4.  Marks messages as sent in the database.

## Dependency Graph
- `WebSocketSessionManager`: To track active connections.
- `WebSocketEventService`: To process business logic for events.
- `OfflineMessageService`: To handle message delivery guarantees.

## Usage Guide
Configured in `WebSocketConfig` to handle the `/ws/chat` endpoint.

## Source Link
[ChatWebSocketHandler.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/websocket/ChatWebSocketHandler.java)
