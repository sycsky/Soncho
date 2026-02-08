# RoleService

## 1. Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `Transactional(readOnly = true)`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 角色与权限管理服务。负责管理系统中的角色（Role）定义及其关联的权限集（Permissions）。

## 2. Method Deep Dive

### `createRole`
- **Signature**: `public RoleDto createRole(CreateRoleRequest request)`
- **Description**: 创建新角色。
- **Logic**: 接收角色名称、描述和权限列表（List<String>），将其转换为 Map 存储并保存。

### `updateRole`
- **Signature**: `public RoleDto updateRole(UUID roleId, UpdateRoleRequest request)`
- **Description**: 更新现有角色的基本信息和权限配置。

### `deleteRole`
- **Signature**: `public void deleteRole(UUID roleId)`
- **Description**: 删除角色。

### `listRoles`
- **Signature**: `public List<RoleDto> listRoles()`
- **Description**: 获取系统中所有角色的列表。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `RoleRepository`: 角色数据存储。
  - `EntityMapper`: DTO 转换。

## 4. Usage Guide
### 场景：权限控制
管理员创建一个 "实习客服" 角色，配置权限仅包含 `VIEW_SESSION` 和 `SEND_MESSAGE`，不包含 `DELETE_SESSION` 或 `MANAGE_SETTINGS`。然后将该角色分配给新入职的员工，从而限制其系统操作范围。
