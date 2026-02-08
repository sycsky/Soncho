# SessionGroupService

## 1. Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `Transactional`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 会话分组服务。管理客服的会话列表分组（文件夹），包括系统默认分组（Open, Resolved）和自定义分组。支持会话在分组间的移动以及分类与分组的自动绑定。

## 2. Method Deep Dive

### `ensureDefaultGroups`
- **Signature**: `public void ensureDefaultGroups(Agent agent)`
- **Annotations**: `@Transactional(propagation = Propagation.REQUIRES_NEW)`
- **Description**: 保证客服至少拥有 "Open" 和 "Resolved" 两个系统分组。通常在客服创建时调用。

### `createGroup` / `updateGroup` / `deleteGroup`
- **Description**: 自定义分组的 CRUD。
- **Special Logic**: 删除分组时，会将组内的所有会话自动转移到默认的 "Open" 分组，防止会话丢失。

### `bindCategoryToGroup`
- **Signature**: `public void bindCategoryToGroup(UUID groupId, UUID categoryId, UUID agentId)`
- **Description**: 建立分类与分组的映射关系。
- **Logic**: 当会话被标记为某个分类时（如 "VIP客户"），系统可自动将其移动到绑定的分组（如 "重点维护" 文件夹）中。

### `findGroupByCategoryOrDefault`
- **Signature**: `public SessionGroup findGroupByCategoryOrDefault(Agent agent, UUID categoryId)`
- **Description**: 路由逻辑。根据会话的分类 ID，查找该客服是否配置了对应的目标分组；如果没有，返回默认分组。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `SessionGroupRepository`: 分组存储。
  - `SessionGroupMappingRepository`: 会话-分组关系。
  - `SessionGroupCategoryBindingRepository`: 分组-分类绑定关系。

## 4. Usage Guide
### 场景：自动归档
客服设置了一个 "已解决" 分组，并将其与 "完成" 状态或特定分类绑定。当会话结束并被标记为已解决时，该会话会自动从 "进行中" 列表移动到 "已解决" 列表，保持工作台整洁。
