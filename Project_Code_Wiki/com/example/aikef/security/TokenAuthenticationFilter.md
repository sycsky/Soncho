# TokenAuthenticationFilter

## Class Profile
`TokenAuthenticationFilter` is a legacy filter for agent authentication. It extracts the Bearer token, validates it via `TokenService`, and sets the security context.

## Method Deep Dive
- **Logic**:
    1.  Check `Authorization` header.
    2.  If `Bearer ...`, resolve token.
    3.  If valid, set `SecurityContext`.
    4.  Refresh token TTL in Redis.

## Note
This filter is likely superseded or complemented by `UnifiedAuthenticationFilter` which handles both agents and customers.

## Source Link
[TokenAuthenticationFilter.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/security/TokenAuthenticationFilter.java)
