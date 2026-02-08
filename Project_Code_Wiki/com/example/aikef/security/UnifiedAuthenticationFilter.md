# UnifiedAuthenticationFilter

## Class Profile
`UnifiedAuthenticationFilter` is the main security filter that handles authentication for **both** Agents and Customers. It inspects the `Authorization` header and delegates to the appropriate service based on the token format.

## Method Deep Dive

### `doFilterInternal(...)`
- **Logic**:
    1.  Extracts the Bearer token.
    2.  **Customer Check**: If token starts with `cust_`, delegates to `CustomerTokenService`.
        - Creates a `UsernamePasswordAuthenticationToken` with `CustomerPrincipal`.
    3.  **Agent Check**: Otherwise, delegates to `TokenService` (Redis).
        - Creates a `UsernamePasswordAuthenticationToken` with `AgentPrincipal` and authorities.
    4.  Sets the `SecurityContext`.

## Dependency Graph
- `TokenService`: For agents.
- `CustomerTokenService`: For customers.

## Usage Guide
Registered in `SecurityConfig` before the standard Spring Security filters.

## Source Link
[UnifiedAuthenticationFilter.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/security/UnifiedAuthenticationFilter.java)
