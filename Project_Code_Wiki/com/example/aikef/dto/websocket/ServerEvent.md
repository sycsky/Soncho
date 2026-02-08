# ServerEvent

## 1. Class Profile
- **Class Name**: `ServerEvent`
- **Package**: `com.example.aikef.dto.websocket`
- **Type**: `Record`
- **Role**: Data Transfer Object (WebSocket)
- **Purpose**: Represents a message sent **from** the server **to** the client via WebSocket.

## 2. Method Deep Dive
### Fields
- `event`: Event type name (e.g., `message.new`, `session.updated`).
- `payload`: The data object (serialized to JSON).

## 3. Dependency Graph
- **Internal Dependencies**: None.

## 4. Usage Guide
Used by `WebSocketService`.
- **Broadcast**: `webSocketService.broadcastToAgent(agentId, new ServerEvent("message.new", messageDto));`

## 5. Source Link
[ServerEvent.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/websocket/ServerEvent.java)
