# UpdateRoleRequest

## 1. Class Profile
- **Class Name**: `UpdateRoleRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: Request payload to modify a security role's permissions.

## 2. Method Deep Dive
### Fields
- `name`: Role name.
- `description`: Description.
- `permissions`: Map of permission flags (Note: type is `Map<String, Object>` here vs `Set<String>` in create request, likely due to frontend JSON structure).

## 3. Dependency Graph
- **Internal Dependencies**: None.

## 4. Usage Guide
Used by `RoleController.update`.
- **Access Control**: Granting or revoking capabilities for a group of users.

## 5. Source Link
[UpdateRoleRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/UpdateRoleRequest.java)
