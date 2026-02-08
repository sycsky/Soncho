# TenantInterceptor

## Class Profile
`TenantInterceptor` is a Spring MVC `HandlerInterceptor` that resolves the tenant ID from incoming HTTP requests and populates the `TenantContext`. It serves as the entry point for the multi-tenancy context.

## Method Deep Dive

### `preHandle(...)`
- **Logic**:
    1.  Checks if SaaS is enabled.
    2.  Checks if the request path is whitelisted (e.g., login, webhooks).
    3.  **Resolution Strategy**:
        - **Token**: Extracts from `AgentPrincipal` or `CustomerPrincipal` in `SecurityContext`.
        - **Header**: Checks `X-Tenant-ID`.
        - **Parameter**: Checks `?tenantId=...`.
    4.  If resolved, sets `TenantContext`.
    5.  If unresolved and not whitelisted, returns 400 Bad Request.

### `afterCompletion(...)`
- **Logic**: Clears the `TenantContext` to ensure thread safety.

## Dependency Graph
- `TenantContext`: To store the resolved ID.
- `SecurityContextHolder`: To access authentication details.

## Usage Guide
Registered automatically by `SaasConfig`. To bypass tenant checks for a new public API, add the path to `WHITELIST_PATHS`.

## Source Link
[TenantInterceptor.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/saas/interceptor/TenantInterceptor.java)
