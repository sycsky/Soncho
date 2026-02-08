# Class Profile: AdminController

**File Path**: `com/example/aikef/controller/AdminController.java`
**Type**: Controller (`@RestController`)
**Purpose**: Provides administrative endpoints for managing Agents (support staff) and Roles (permissions). It handles CRUD operations for `Agent` and `Role` entities, ensuring that only authorized administrators can modify system access configurations.

# Method Deep Dive

## Agent Management
- **`listAgents(String name, String role, Pageable pageable)`**: Retrieves a paginated list of agents, optionally filtered by name or role.
- **`createAgent(CreateAgentRequest request)`**: Registers a new agent in the system.
- **`updateAgent(UUID id, UpdateAgentRequest request)`**: Updates an existing agent's details.

## Role Management
- **`listRoles()`**: Returns all defined roles in the system.
- **`createRole(CreateRoleRequest request)`**: Defines a new role with specific permissions.
- **`updateRole(UUID id, UpdateRoleRequest request)`**: Modifies an existing role.
- **`deleteRole(UUID id)`**: Removes a role from the system (note: handled with care if assigned to agents).

# Dependency Graph

**Core Dependencies**:
- `AgentService`: Business logic for agent management.
- `RoleService`: Business logic for role management.
- `AgentDto`, `RoleDto`: Data transfer objects for responses.
- `CreateAgentRequest`, `CreateRoleRequest`, etc.: Request DTOs.

**Key Imports**:
```java
import com.example.aikef.service.AgentService;
import com.example.aikef.service.RoleService;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
```

# Usage Guide

These endpoints are typically protected by security configurations (e.g., Spring Security) requiring ADMIN authority.

## Example: Create a new Agent
`POST /api/v1/admin/agents`
```json
{
  "username": "support_lead",
  "email": "lead@example.com",
  "roleId": "uuid-of-admin-role"
}
```

# Source Link
[AdminController.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/controller/AdminController.java)
