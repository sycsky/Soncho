# RoleDto

## 1. Class Profile
- **Class Name**: `RoleDto`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record`
- **Role**: Data Transfer Object
- **Purpose**: Transfers security role definitions and their permission maps.

## 2. Method Deep Dive
### Fields
- `id`: Role UUID.
- `name`: Role name (e.g., "Admin").
- `description`: Usage description.
- `system`: Flag for built-in roles.
- `permissions`: JSON map of granted authorities (e.g., `{"workflow.edit": true}`).

## 3. Dependency Graph
- **Internal Dependencies**: None.

## 4. Usage Guide
Used by `RoleController`.
- **Permission Check**: The frontend uses this to conditionally render UI elements (e.g., hiding the "Settings" button if `permissions['settings.view']` is false).

## 5. Source Link
[RoleDto.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/RoleDto.java)
