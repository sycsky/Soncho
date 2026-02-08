# CreateRoleRequest

## 1. Class Profile
- **Class Name**: `CreateRoleRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: Request payload to define a new security role with specific permissions.

## 2. Method Deep Dive
### Fields
- `name`: Unique role name (e.g., "Supervisor").
- `description`: Role purpose.
- `permissions`: Set of permission strings (e.g., "user:read", "settings:write").

## 3. Dependency Graph
- **Internal Dependencies**: None.

## 4. Usage Guide
Used by `RoleController.create`.
- **RBAC**: Admins use this to define custom roles beyond the standard "Admin" and "Agent".

## 5. Source Link
[CreateRoleRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/CreateRoleRequest.java)
