# WebSocketEnvelope

## 1. Class Profile
- **Class Name**: `WebSocketEnvelope`
- **Package**: `com.example.aikef.dto.websocket`
- **Type**: `Record`
- **Role**: Data Transfer Object (WebSocket)
- **Purpose**: Represents a message sent **from** the client **to** the server. It wraps the raw JSON payload with metadata.

## 2. Method Deep Dive
### Fields
- `eventId`: Client-generated UUID for tracking/ack.
- `timestamp`: Sending time.
- `event`: Event type (e.g., `heartbeat`, `typing`).
- `payload`: Raw JSON node.

## 3. Dependency Graph
- **External Dependencies**:
  - `com.fasterxml.jackson.databind.JsonNode`

## 4. Usage Guide
Used by `ChatWebSocketHandler`.
- **Handling**: `handleTextMessage` parses the string into this envelope, then switches on `event` to dispatch to the correct handler.

## 5. Source Link
[WebSocketEnvelope.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/websocket/WebSocketEnvelope.java)
