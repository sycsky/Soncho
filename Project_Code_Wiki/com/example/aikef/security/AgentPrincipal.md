# AgentPrincipal

## Class Profile
`AgentPrincipal` implements Spring Security's `UserDetails` interface. It wraps the `Agent` entity and provides the necessary user information (ID, email, password, authorities) to the security framework.

## Method Deep Dive

### Constructor
- **Parameters**: `Agent` entity, `Collection<? extends GrantedAuthority>`.
- **Logic**: Copies fields from the entity to the principal.

### `getTenantId()`
- **Description**: Exposes the tenant ID for multi-tenancy support.

## Dependency Graph
- `Agent` entity.

## Usage Guide
This object is stored in the `SecurityContext` after successful authentication.

```java
AgentPrincipal user = (AgentPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
UUID agentId = user.getId();
```

## Source Link
[AgentPrincipal.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/security/AgentPrincipal.java)
