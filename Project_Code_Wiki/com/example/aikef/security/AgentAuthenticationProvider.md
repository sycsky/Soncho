# AgentAuthenticationProvider

## Class Profile
`AgentAuthenticationProvider` is a Spring Security component responsible for authenticating agents (customer support staff) using their email and password. It verifies credentials against the database and creates an authenticated `AgentPrincipal`.

## Method Deep Dive

### `authenticate(Authentication authentication)`
- **Logic**:
    1.  Extracts email and password from the authentication token.
    2.  Looks up the agent by email using `AgentRepository`.
    3.  Verifies the password using `PasswordEncoder`.
    4.  If successful, creates an `AgentPrincipal` and returns a fully populated `UsernamePasswordAuthenticationToken`.
    5.  Throws `BadCredentialsException` if authentication fails.

## Dependency Graph
- `AgentRepository`: To retrieve agent details.
- `PasswordEncoder`: To verify password hashes.

## Usage Guide
Registered in `SecurityConfig` to handle standard username/password login requests.

## Source Link
[AgentAuthenticationProvider.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/security/AgentAuthenticationProvider.java)
