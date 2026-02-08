# WebSocketConfig

## Class Profile
`WebSocketConfig` configures the WebSocket endpoints and handlers. It implements `WebSocketConfigurer`.

## Method Deep Dive

### `registerWebSocketHandlers(...)`
- **Logic**:
    - Registers `/ws/chat` endpoint.
    - Assigns `ChatWebSocketHandler` to handle messages.
    - Adds `TokenHandshakeInterceptor` for authentication during handshake.
    - Configures CORS (`*`).
    - Registers both raw WebSocket and SockJS fallback versions.

## Dependency Graph
- `ChatWebSocketHandler`
- `TokenHandshakeInterceptor`

## Source Link
[WebSocketConfig.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/config/WebSocketConfig.java)
