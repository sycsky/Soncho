# BootstrapService

## 1. Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `Transactional(readOnly = true)`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 引导服务（Bootstrap）。负责聚合应用启动或刷新时所需的各类初始化数据，减少前端的请求次数。

## 2. Method Deep Dive

### `bootstrap`
- **Signature**: `public BootstrapResponse bootstrap(UUID agentId)`
- **Description**: 获取指定客服的完整初始化数据包。
- **Parameters**:
  - `agentId`: 当前登录客服 ID。
- **Returns**: `BootstrapResponse` - 包含分组、会话、客服信息、角色、快捷回复、知识库状态等。
- **Logic**:
  1. 加载客服的所有会话分组 (`SessionGroup`)。
  2. 并行加载每个分组下的会话列表 (`ChatSession`)。
  3. 区分 "主责会话" 和 "支持会话"。
  4. 批量查询未读消息数（主责会话）和未读 @ 数（支持会话）。
  5. 组装并返回包含所有字典数据（角色、快捷回复等）的响应对象。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `SessionGroupRepository`, `SessionGroupMappingRepository`: 分组数据。
  - `AgentRepository`, `RoleRepository`: 用户与权限数据。
  - `QuickReplyRepository`: 快捷回复数据。
  - `ReadRecordService`: 未读数统计。
  - `EntityMapper`: DTO 转换。

## 4. Usage Guide
### 场景：客服登录
当客服登录客服工作台时，前端调用 `/api/v1/bootstrap`。后端 `BootstrapService` 一次性返回左侧会话列表（带未读红点）、当前用户信息、可用的快捷回复列表等，使页面能立即渲染完整状态，无需后续多次加载。
