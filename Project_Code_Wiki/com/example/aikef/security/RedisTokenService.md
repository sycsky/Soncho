# RedisTokenService

## Class Profile
`RedisTokenService` is the production-grade implementation of `TokenService`. It stores agent authentication tokens in Redis, ensuring they persist across application restarts and can be shared in a clustered environment.

## Method Deep Dive

### `issueToken(AgentPrincipal principal)`
- **Logic**:
    1.  Generates a random UUID token.
    2.  Stores the mapping `agent_token:{token} -> {agentId}` in Redis.
    3.  Sets a TTL (default 30 days).

### `resolve(String token)`
- **Logic**:
    1.  Looks up the agent ID from Redis using the token.
    2.  If found, loads the full `Agent` entity (including Role) from the database to ensure permissions are up-to-date.
    3.  Returns an `AgentPrincipal`.

### `refreshToken(String token)`
- **Logic**: Resets the TTL of the token key in Redis to extend the session.

## Dependency Graph
- `StringRedisTemplate`: For Redis operations.
- `AgentRepository`: To load agent details.

## Usage Guide
Automatically used by `TokenAuthenticationFilter` (and `UnifiedAuthenticationFilter`) to validate requests.

## Source Link
[RedisTokenService.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/security/RedisTokenService.java)
