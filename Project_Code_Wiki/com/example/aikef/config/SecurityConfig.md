# SecurityConfig

## Class Profile
`SecurityConfig` is the main Spring Security configuration class. It defines the security filter chain, CORS policies, authentication managers, and public/private endpoint rules.

## Method Deep Dive

### `securityFilterChain(HttpSecurity http, ...)`
- **Logic**:
    - Disables CSRF (stateless API).
    - Configures CORS.
    - Sets Session Management to `STATELESS`.
    - **Authorization Rules**:
        - Public: `/api/health`, `/api/v1/auth/login`, webhooks, static files.
        - Protected: All other endpoints require authentication.
    - Adds `UnifiedAuthenticationFilter` before the standard username/password filter to handle custom Token-based auth.

### `corsConfigurationSource()`
- **Logic**: Allows all origins (`*`) with credentials support (using `AllowedOriginPatterns`).

## Dependency Graph
- `UnifiedAuthenticationFilter`: Custom filter.
- `AgentAuthenticationProvider`: Custom auth provider.

## Usage Guide
Loaded automatically. To open a new endpoint, modify `authorizeHttpRequests`.

## Source Link
[SecurityConfig.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/config/SecurityConfig.java)
