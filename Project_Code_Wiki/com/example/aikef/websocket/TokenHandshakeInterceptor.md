# TokenHandshakeInterceptor

## Class Profile
`TokenHandshakeInterceptor` is a WebSocket handshake interceptor that authenticates users *before* the WebSocket connection is established. It validates the authentication token passed in the query parameter.

## Method Deep Dive

### `beforeHandshake(...)`
- **Logic**:
    1.  Extracts the `token` parameter from the request URL (e.g., `ws://...?token=xyz`).
    2.  **Customer Auth**: If token starts with `cust_`, validates via `CustomerTokenService`.
    3.  **Agent Auth**: Otherwise, validates via `TokenService`.
    4.  If valid, stores the `Principal` object in the WebSocket session attributes (`CUSTOMER_PRINCIPAL` or `AGENT_PRINCIPAL`).
    5.  Refreshes the token TTL.
    6.  Returns `true` to allow connection, or `false` to reject it.

## Dependency Graph
- `TokenService` / `RedisTokenService`: For agent tokens.
- `CustomerTokenService`: For customer tokens.

## Usage Guide
Registered in `WebSocketConfig`. Clients must append `?token=<valid_token>` to the WebSocket URL.

## Source Link
[TokenHandshakeInterceptor.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/websocket/TokenHandshakeInterceptor.java)
