# WebSocketSessionManager

## Class Profile
`WebSocketSessionManager` is a singleton component responsible for tracking all active WebSocket sessions. It maps User IDs (Agents and Customers) to their open WebSocket sessions, supporting multiple device connections per user.

## Method Deep Dive

### `registerAgent(UUID agentId, WebSocketSession session)` / `registerCustomer(...)`
- **Logic**: Adds the session to a thread-safe `Set` associated with the user ID.

### `broadcastToSession(...)`
- **Description**: Sends a message to all participants in a chat session (Primary Agent, Support Agents, Customer), excluding the sender.
- **Logic**:
    1.  Iterates through all relevant participant IDs.
    2.  Filters out the `senderId`.
    3.  Calls `sendToAgent` or `sendToCustomer`.

### `sendToAgent(...)` / `sendToCustomer(...)`
- **Logic**: Looks up the active sessions for the ID and sends the text message. Handles `IOException` gracefully.

### `isAgentOnline(...)` / `isCustomerOnline(...)`
- **Returns**: `true` if the user has at least one open session.

## Dependency Graph
- `ConcurrentHashMap`: For thread-safe session storage.

## Usage Guide
Used by `ChatWebSocketHandler`, `WebSocketEventService`, and other services that need to push real-time updates.

```java
sessionManager.sendToAgent(agentId, jsonMessage);
```

## Source Link
[WebSocketSessionManager.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/websocket/WebSocketSessionManager.java)
