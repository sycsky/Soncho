# Role

## 1. Class Profile
- **Class Name**: `Role`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Security Entity
- **Purpose**: Defines a set of permissions that can be assigned to an Agent. Supports Role-Based Access Control (RBAC).

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `name`: Unique role name (e.g., "Administrator", "Support Agent").
- `system`: Flag indicating if this is a built-in immutable role.
- `description`: Explanation of the role's capabilities.
- `permissions`: JSON map defining granular access rights (e.g., `{"workflow.edit": true, "agent.create": false}`).
- `tenantId`: Multi-tenancy identifier.

### Lifecycle
- `onCreate()`: Automatically sets the `tenantId` from the current context if not provided.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.saas.context.TenantContext`: For tenant resolution.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
This entity drives the frontend UI logic and backend security checks.
- **Frontend**: The UI checks `permissions` to decide whether to show the "Settings" button.
- **Backend**: `UnifiedAuthenticationFilter` loads these permissions into the `GrantedAuthority` list for Spring Security.

## 5. Source Link
[Role.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/Role.java)
