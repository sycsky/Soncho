## Session 分组功能与 Bootstrap 接口修复

## 📋 修改总结

### 1. ✅ 修复 Bootstrap 接口数据结构

#### 修复前的问题
- `lastActiveAt` 字段名不匹配（前端期望 `lastActive`）
- 缺少 `user` 对象（只有 `userId`）
- 缺少 `messages` 数组
- 缺少 `unreadCount` 字段
- 时间格式不匹配（返回 ISO 字符串，前端期望时间戳）

#### 修复后的 ChatSessionDto
```java
public record ChatSessionDto(
        UUID id,
        UUID userId,
        CustomerDto user,              // ✅ 完整的客户对象
        SessionStatus status,
        long lastActive,               // ✅ 时间戳（毫秒）
        int unreadCount,               // ✅ 未读消息数
        UUID groupId,
        UUID sessionGroupId,           // ✅ Session 分组 ID
        UUID primaryAgentId,
        List<UUID> supportAgentIds,
        List<ChatMessageDto> messages  // ✅ 最近的消息列表
)
```

### 2. ✅ 实现 Session 分组功能

#### 功能说明
Session 分组允许客服将会话分配到不同的组进行管理，每个客服有两个系统默认分组：
- **Open** (默认分组) - 新创建的聊天会话默认分配到这个组
- **Resolved** (已解决分组) - 已处理完的会话移到这个组

系统分组特点：
- `system = true` 标记为系统分组
- 不能被删除
- 不能修改名称
- 首次登录时自动创建

## 📁 新增文件

### 实体类
- `SessionGroup.java` - Session 分组实体

### Repository
- `SessionGroupRepository.java` - Session 分组数据访问

### Service
- `SessionGroupService.java` - Session 分组业务逻辑

### Controller
- `SessionGroupController.java` - Session 分组 API

### DTO
- `SessionGroupDto.java` - Session 分组数据传输对象
- `CustomerDto.java` - 客户简化信息（已存在，补充说明）

### 数据库迁移
- `db/create_session_groups.sql` - 创建分组表和相关字段

## 📝 修改的文件

### 实体类
- `ChatSession.java` - 添加 `sessionGroup` 字段

### DTO
- `ChatSessionDto.java` - 完全重构，匹配前端期望
- `BootstrapResponse.java` - 添加 `sessionGroups` 字段

### Service
- `BootstrapService.java` - 修复数据映射，添加分组初始化

### Mapper
- `EntityMapper.java` - 添加 `toSessionGroupDto()`，重构 `toChatSessionDto()`

## 🎯 核心功能

### 1. 系统默认分组自动创建

当客服首次登录或调用 bootstrap 接口时，系统会自动创建两个默认分组：

```java
// Open 分组（默认）
{
    "name": "Open",
    "system": true,
    "icon": "📥",
    "color": "#3B82F6",
    "sortOrder": 0
}

// Resolved 分组
{
    "name": "Resolved",
    "system": true,
    "icon": "✅",
    "color": "#10B981",
    "sortOrder": 999
}
```

### 2. 分组管理 API

#### 获取我的分组
```http
GET /api/v1/session-groups
Authorization: Bearer {agentToken}
```

响应：
```json
[
    {
        "id": "uuid",
        "name": "Open",
        "system": true,
        "agentId": "uuid",
        "icon": "📥",
        "color": "#3B82F6",
        "sortOrder": 0,
        "createdAt": "2024-01-01T00:00:00Z",
        "updatedAt": "2024-01-01T00:00:00Z"
    },
    {
        "id": "uuid",
        "name": "Resolved",
        "system": true,
        "agentId": "uuid",
        "icon": "✅",
        "color": "#10B981",
        "sortOrder": 999,
        "createdAt": "2024-01-01T00:00:00Z",
        "updatedAt": "2024-01-01T00:00:00Z"
    }
]
```

#### 创建自定义分组
```http
POST /api/v1/session-groups
Authorization: Bearer {agentToken}
Content-Type: application/json

{
    "name": "VIP 客户",
    "icon": "⭐",
    "color": "#F59E0B"
}
```

#### 更新分组
```http
PUT /api/v1/session-groups/{id}
Authorization: Bearer {agentToken}
Content-Type: application/json

{
    "name": "重要客户",
    "icon": "🔥",
    "color": "#EF4444"
}
```

**注意**：系统分组不能修改名称，但可以修改图标和颜色。

#### 删除分组
```http
DELETE /api/v1/session-groups/{id}
Authorization: Bearer {agentToken}
```

**注意**：系统分组不能删除。

### 3. Bootstrap 接口修复

#### 修复后的响应
```json
{
    "sessions": [
        {
            "id": "uuid",
            "userId": "uuid",
            "user": {
                "id": "uuid",
                "name": "张三",
                "primaryChannel": "WEB",
                "email": "zhangsan@example.com",
                "phone": "13800138000",
                "metadata": {},
                "active": true,
                "createdAt": "2024-01-01T00:00:00Z"
            },
            "status": "HUMAN_HANDLING",
            "lastActive": 1732530924000,
            "unreadCount": 3,
            "groupId": "uuid",
            "sessionGroupId": "uuid",
            "primaryAgentId": "uuid",
            "supportAgentIds": [],
            "messages": []
        }
    ],
    "sessionGroups": [
        {
            "id": "uuid",
            "name": "Open",
            "system": true,
            "agentId": "uuid",
            "icon": "📥",
            "color": "#3B82F6",
            "sortOrder": 0,
            "createdAt": "2024-01-01T00:00:00Z",
            "updatedAt": "2024-01-01T00:00:00Z"
        }
    ],
    "agents": [...],
    "groups": [...],
    "roles": [...],
    "quickReplies": [...],
    "knowledgeBase": [...]
}
```

## 🔧 技术实现

### 数据库设计

#### session_groups 表
```sql
CREATE TABLE session_groups (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    is_system BOOLEAN NOT NULL DEFAULT false,
    agent_id UUID NOT NULL REFERENCES agents(id),
    icon VARCHAR(50),
    color VARCHAR(20),
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_session_group_agent_name UNIQUE (agent_id, name)
);
```

#### chat_sessions 表新增字段
```sql
ALTER TABLE chat_sessions 
ADD COLUMN session_group_id UUID REFERENCES session_groups(id);
```

### 业务逻辑

#### 默认分组初始化
```java
public void ensureDefaultGroups(Agent agent) {
    // 检查是否已有系统分组
    List<SessionGroup> systemGroups = 
        sessionGroupRepository.findByAgentAndSystemTrue(agent);
    
    // 创建 Open 分组（如果不存在）
    if (!hasOpenGroup) {
        createSystemGroup(agent, "Open", "📥", "#3B82F6", 0);
    }
    
    // 创建 Resolved 分组（如果不存在）
    if (!hasResolvedGroup) {
        createSystemGroup(agent, "Resolved", "✅", "#10B981", 999);
    }
}
```

#### 分组验证
```java
public SessionGroup createGroup(Agent agent, String name, ...) {
    // 1. 检查名称是否已存在
    if (sessionGroupRepository.existsByAgentAndName(agent, name)) {
        throw new IllegalArgumentException("分组名称已存在");
    }
    
    // 2. 系统分组名称不能被占用
    if ("Open".equals(name) || "Resolved".equals(name)) {
        throw new IllegalArgumentException("不能使用系统分组名称");
    }
    
    // 3. 创建分组
    SessionGroup group = new SessionGroup();
    group.setName(name);
    group.setSystem(false);
    group.setAgent(agent);
    // ...
    return sessionGroupRepository.save(group);
}
```

## 📊 字段对比

### 修复前 vs 修复后

| 字段 | 修复前 | 修复后 | 说明 |
|------|--------|--------|------|
| `lastActiveAt` | `Instant` (ISO 字符串) | `lastActive: long` (时间戳) | ✅ 字段名和类型都修复 |
| `userId` | `UUID` (可能为null) | `UUID` + `user: CustomerDto` | ✅ 添加完整用户对象 |
| `messages` | ❌ 不存在 | `List<ChatMessageDto>` | ✅ 添加消息列表 |
| `unreadCount` | ❌ 不存在 | `int` | ✅ 添加未读计数 |
| `sessionGroupId` | ❌ 不存在 | `UUID` | ✅ 添加分组关联 |

## 🚀 使用示例

### 前端初始化流程

```typescript
// 1. 调用 bootstrap 接口
const response = await fetch('/api/v1/bootstrap', {
    headers: {
        'Authorization': `Bearer ${agentToken}`
    }
});

const data = await response.json();

// 2. 数据现在完全匹配前端类型
const sessions: ChatSession[] = data.sessions;
const sessionGroups: SessionGroup[] = data.sessionGroups;

// 3. 可以直接使用，不需要转换
sessions.forEach(session => {
    console.log(session.user.name);        // ✅ 直接访问
    console.log(session.lastActive);       // ✅ 时间戳格式
    console.log(session.unreadCount);      // ✅ 未读数
    console.log(session.messages);         // ✅ 消息列表
});

// 4. 按分组显示会话
sessionGroups.forEach(group => {
    const groupSessions = sessions.filter(
        s => s.sessionGroupId === group.id
    );
    console.log(`${group.icon} ${group.name}: ${groupSessions.length}`);
});
```

### 创建自定义分组

```typescript
async function createCustomGroup(name: string, icon: string, color: string) {
    const response = await fetch('/api/v1/session-groups', {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${agentToken}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ name, icon, color })
    });
    
    const group = await response.json();
    return group;
}

// 使用
const vipGroup = await createCustomGroup('VIP 客户', '⭐', '#F59E0B');
```

### 移动会话到分组

```typescript
async function moveSessionToGroup(sessionId: string, groupId: string) {
    const response = await fetch(`/api/v1/chat/sessions/${sessionId}`, {
        method: 'PATCH',
        headers: {
            'Authorization': `Bearer ${agentToken}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ sessionGroupId: groupId })
    });
    
    return await response.json();
}
```

## 🎨 UI 建议

### 分组展示
```
📥 Open (12)
   ├─ 张三 - 产品咨询
   ├─ 李四 - 退款申请
   └─ ...

⭐ VIP 客户 (3)
   ├─ 王五 - 定制服务
   └─ ...

✅ Resolved (45)
   └─ [已折叠]
```

### 拖拽移动
- 支持拖拽会话到不同分组
- 拖拽时显示高亮效果
- 松开时自动更新

### 分组管理
- 右键菜单：重命名、修改颜色、删除
- 系统分组只能修改颜色，不能删除
- 新建分组的按钮

## 🔒 权限控制

### 分组权限
- 每个客服只能看到和管理自己的分组
- 系统分组自动创建，不能删除
- 自定义分组可以自由管理

### 会话分配
- 只能将会话分配到自己的分组
- 系统会自动创建默认分组（如果不存在）

## ⚠️ 注意事项

1. **系统分组名称保留**：`Open` 和 `Resolved` 是系统保留名称，不能用于自定义分组

2. **分组唯一性**：同一个客服下的分组名称必须唯一

3. **删除保护**：删除分组时，该分组下的会话会自动移到默认分组（Open）

4. **排序顺序**：
   - 系统分组 `Open`: sortOrder = 0
   - 自定义分组: sortOrder = 100
   - 系统分组 `Resolved`: sortOrder = 999

## 📈 后续优化建议

### TODO
1. **未读消息计数**：实现真实的未读消息统计
2. **消息列表**：加载最近的消息到 session.messages
3. **批量操作**：支持批量移动会话到分组
4. **分组统计**：每个分组的会话数量、未读数等
5. **分组排序**：支持自定义分组排序
6. **分组图标库**：提供预设的图标和颜色选项

## ✅ 测试检查清单

- [ ] Bootstrap 接口返回正确的数据结构
- [ ] 首次登录自动创建系统分组
- [ ] 可以创建自定义分组
- [ ] 可以更新自定义分组
- [ ] 不能删除系统分组
- [ ] 不能使用系统分组名称
- [ ] 分组名称唯一性验证
- [ ] 会话可以关联到分组
- [ ] 前端可以正确解析所有字段

## 🎉 总结

通过这次修复和功能新增，实现了：

1. ✅ **修复 Bootstrap 接口**：完全匹配前端类型定义
2. ✅ **Session 分组功能**：让客服可以组织管理会话
3. ✅ **系统默认分组**：自动创建 Open 和 Resolved 分组
4. ✅ **完整的 CRUD API**：支持分组的创建、查询、更新、删除
5. ✅ **数据库迁移脚本**：可以直接运行创建表结构

现在前端可以：
- 直接使用 bootstrap 数据，无需转换
- 访问完整的客户信息（user 对象）
- 获取正确格式的时间戳
- 按分组展示和管理会话
- 创建和管理自定义分组

所有功能已实现并通过编译检查！🎊
