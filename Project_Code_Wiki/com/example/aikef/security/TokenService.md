# TokenService

## Class Profile
`TokenService` is the interface definition for token management operations (issue, resolve, revoke).

## Methods
- `String issueToken(AgentPrincipal principal)`
- `Optional<AgentPrincipal> resolve(String token)`
- `void revoke(String token)`

## Implementations
- `RedisTokenService` (Primary)
- `InMemoryTokenService` (Deprecated)

## Source Link
[TokenService.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/security/TokenService.java)
