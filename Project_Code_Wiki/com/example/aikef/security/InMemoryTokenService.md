# InMemoryTokenService

## Class Profile
**Deprecated**. An implementation of `TokenService` that stores tokens in a local `ConcurrentHashMap`.

## Warning
> **Note**: Tokens are lost when the application restarts. Use `RedisTokenService` for production.

## Method Deep Dive
- `issueToken`: Generates a UUID and stores it in memory.
- `resolve`: Looks up the UUID in the map.
- `revoke`: Removes the UUID from the map.

## Source Link
[InMemoryTokenService.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/security/InMemoryTokenService.java)
