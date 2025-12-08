# Session Group API 文档

## 概述

Session Group（会话分组）功能允许客服对会话进行自定义分类和管理。每个客服可以创建自己的分组，并将会话移动到不同的分组中。

**注意**：所有接口都需要客服认证（AgentPrincipal）。

---

## 接口列表

### 1. 获取当前客服的所有分组

**接口**: `GET /api/v1/session-groups`

**描述**: 获取当前登录客服的所有分组，包括每个分组下的会话列表。

**请求头**:
```
Authorization: Bearer {token}
```

**响应示例**:
```json
[
  {
    "id": "uuid-group-1",
    "name": "待处理",
    "system": false,
    "agentId": "uuid-agent-1",
    "icon": "inbox",
    "color": "#FF5722",
    "sortOrder": 1,
    "sessions": [
      {
        "id": "uuid-session-1",
        "userId": "uuid-customer-1",
        "user": {
          "id": "uuid-customer-1",
          "name": "张三",
          "primaryChannel": "WECHAT",
          "email": "zhang@example.com",
          "phone": "13800138000",
          "wechatOpenId": "openid123",
          "avatar": "https://...",
          "tags": ["VIP"],
          "aiTags": ["high_value"],
          "notes": "重要客户"
        },
        "status": "HUMAN_HANDLING",
        "lastActive": 1700000000000,
        "unreadCount": 3,
        "groupId": "uuid-chatgroup-1",
        "sessionGroupId": "uuid-group-1",
        "primaryAgentId": "uuid-agent-1",
        "supportAgentIds": ["uuid-agent-2"],
        "lastMessage": {
          "id": "uuid-message-1",
          "content": "最后一条消息内容",
          "senderId": "uuid-customer-1",
          "senderType": "USER",
          "timestamp": 1700000000000,
          "attachments": []
        }
      }
    ],
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  },
  {
    "id": "uuid-group-2",
    "name": "进行中",
    "system": false,
    "agentId": "uuid-agent-1",
    "icon": "chat",
    "color": "#4CAF50",
    "sortOrder": 2,
    "sessions": [],
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
]
```

**字段说明**:
- `system`: 是否为系统分组（系统分组不可删除，如"Open"、"Resolved"）
- `sessions`: 该分组下的所有会话列表
- `sessionGroupId`: 会话在当前客服视角下所属的分组ID

---

### 2. 创建新分组

**接口**: `POST /api/v1/session-groups`

**描述**: 为当前客服创建一个新的会话分组。

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**:
```json
{
  "name": "重要客户",
  "icon": "star",
  "color": "#FFC107"
}
```

**请求参数**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 分组名称，不能为空 |
| icon | String | 否 | 图标名称 |
| color | String | 否 | 颜色代码（如 #FF5722） |

**响应示例**:
```json
{
  "id": "uuid-new-group",
  "name": "重要客户",
  "system": false,
  "agentId": "uuid-agent-1",
  "icon": "star",
  "color": "#FFC107",
  "sortOrder": 3,
  "sessions": null,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

**错误响应**:
```json
{
  "error": "分组名称不能为空"
}
```

---

### 3. 更新分组信息

**接口**: `PUT /api/v1/session-groups/{id}`

**描述**: 更新指定分组的名称、图标或颜色。

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**路径参数**:
- `id`: 分组ID（UUID）

**请求体**:
```json
{
  "name": "高优先级",
  "icon": "flag",
  "color": "#E91E63"
}
```

**请求参数**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 否 | 新的分组名称 |
| icon | String | 否 | 新的图标名称 |
| color | String | 否 | 新的颜色代码 |

**响应示例**:
```json
{
  "id": "uuid-group-1",
  "name": "高优先级",
  "system": false,
  "agentId": "uuid-agent-1",
  "icon": "flag",
  "color": "#E91E63",
  "sortOrder": 1,
  "sessions": null,
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-15T10:35:00Z"
}
```

**注意**:
- 系统分组（system=true）不可修改
- 只能修改当前客服创建的分组

---

### 4. 删除分组

**接口**: `DELETE /api/v1/session-groups/{id}`

**描述**: 删除指定的会话分组。删除分组时，该分组下的所有会话会自动转移到客服的默认分组（Open）中。

**请求头**:
```
Authorization: Bearer {token}
```

**路径参数**:
- `id`: 分组ID（UUID）

**响应示例**:
```json
{
  "defaultGroupId": "uuid-default-group"
}
```

**响应字段说明**:
- `defaultGroupId`: 默认分组ID，被删除分组下的所有会话已转移到该分组

**业务逻辑**:
1. 查找要删除的分组
2. 验证分组不是系统分组（系统分组不可删除）
3. 获取该客服的默认分组（Open）
4. 将该分组下的所有会话映射（SessionGroupMapping）转移到默认分组
5. 删除该分组
6. 返回默认分组ID

**注意**:
- 系统分组（system=true，如"Open"、"Resolved"）不可删除
- 删除分组不会删除会话本身，只是将会话重新归类到默认分组
- 其他客服对这些会话的分组不受影响
- 返回的`defaultGroupId`可用于前端立即更新UI，将会话显示在默认分组中

---

### 5. 移动会话到指定分组

**接口**: `POST /api/v1/session-groups/{groupId}/sessions/{sessionId}`

**描述**: 将指定会话移动到指定分组。这个操作只影响当前客服的视角，不影响其他客服对该会话的分组。

**请求头**:
```
Authorization: Bearer {token}
```

**路径参数**:
- `groupId`: 目标分组ID（UUID）
- `sessionId`: 会话ID（UUID）

**请求示例**:
```
POST /api/v1/session-groups/uuid-group-2/sessions/uuid-session-1
```

**响应**:
```
HTTP 204 No Content
```

**业务逻辑**:
1. 检查分组和会话是否存在
2. 检查当前客服是否是该会话的参与者（主责客服或支持客服）
3. 创建或更新 SessionGroupMapping 记录
4. 如果会话已在其他分组，会自动移出原分组

**错误响应**:
```json
{
  "error": "分组不存在"
}
```
或
```json
{
  "error": "会话不存在"
}
```

---

### 6. 移出分组（取消分组）

**接口**: `DELETE /api/v1/session-groups/sessions/{sessionId}`

**描述**: 将指定会话从当前客服的所有分组中移出。会话仍然存在，只是不属于任何自定义分组。

**请求头**:
```
Authorization: Bearer {token}
```

**路径参数**:
- `sessionId`: 会话ID（UUID）

**请求示例**:
```
DELETE /api/v1/session-groups/sessions/uuid-session-1
```

**响应**:
```
HTTP 204 No Content
```

**业务逻辑**:
1. 删除该会话在当前客服视角下的分组映射（SessionGroupMapping）
2. 会话本身不会被删除
3. 其他客服对该会话的分组不受影响

---

## 数据模型

### SessionGroupDto

```typescript
interface SessionGroupDto {
  id: string;                    // 分组ID
  name: string;                  // 分组名称
  system: boolean;               // 是否系统分组
  agentId: string;               // 所属客服ID
  icon?: string;                 // 图标名称
  color?: string;                // 颜色代码
  sortOrder?: number;            // 排序序号
  sessions?: ChatSessionDto[];   // 分组下的会话列表（可选）
  createdAt: string;             // 创建时间（ISO 8601）
  updatedAt: string;             // 更新时间（ISO 8601）
}
```

### ChatSessionDto

```typescript
interface ChatSessionDto {
  id: string;                    // 会话ID
  userId: string;                // 客户ID
  user: CustomerDto;             // 客户详细信息
  status: SessionStatus;         // 会话状态
  lastActive: number;            // 最后活跃时间戳（毫秒）
  unreadCount: number;           // 未读消息数
  groupId: string;               // 聊天群组ID
  sessionGroupId?: string;       // 当前客服视角下的分组ID
  primaryAgentId: string;        // 主责客服ID
  supportAgentIds: string[];     // 支持客服ID列表
  lastMessage?: SessionMessageDto; // 最后一条消息
}
```

### SessionStatus（枚举）

```typescript
enum SessionStatus {
  AI_HANDLING = "AI_HANDLING",         // AI处理中
  HUMAN_HANDLING = "HUMAN_HANDLING",   // 人工处理中
  RESOLVED = "RESOLVED"                // 已解决
}
```

---

## 使用场景示例

### 场景1: 初始化客服工作台

```javascript
// 1. 获取所有分组和会话
const groups = await fetch('/api/v1/session-groups', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
}).then(res => res.json());

// groups 包含所有分组及其会话
groups.forEach(group => {
  console.log(`分组: ${group.name}, 会话数: ${group.sessions.length}`);
});
```

### 场景2: 创建自定义分组

```javascript
// 2. 创建新分组
const newGroup = await fetch('/api/v1/session-groups', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    name: '紧急处理',
    icon: 'alert',
    color: '#F44336'
  })
}).then(res => res.json());

console.log(`新分组ID: ${newGroup.id}`);
```

### 场景3: 移动会话到分组

```javascript
// 3. 将会话移动到"紧急处理"分组
await fetch(`/api/v1/session-groups/${newGroup.id}/sessions/${sessionId}`, {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

console.log('会话已移动到新分组');
```

### 场景4: 重命名分组

```javascript
// 4. 更新分组名称
const updatedGroup = await fetch(`/api/v1/session-groups/${groupId}`, {
  method: 'PUT',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    name: '超级紧急'
  })
}).then(res => res.json());

console.log(`分组已重命名为: ${updatedGroup.name}`);
```

### 场景5: 移出分组

```javascript
// 5. 将会话从所有分组中移出
await fetch(`/api/v1/session-groups/sessions/${sessionId}`, {
  method: 'DELETE',
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

console.log('会话已移出分组');
```

### 场景6: 删除分组

```javascript
// 6. 删除不再需要的分组
const response = await fetch(`/api/v1/session-groups/${groupId}`, {
  method: 'DELETE',
  headers: {
    'Authorization': `Bearer ${token}`
  }
}).then(res => res.json());

console.log(`分组已删除，会话已转移到默认分组: ${response.defaultGroupId}`);

// 前端可以立即更新UI，将被删除分组的会话显示在默认分组中
updateUIAfterGroupDeletion(groupId, response.defaultGroupId);
```

---

## 系统分组说明

系统会为每个客服自动创建两个系统分组：

1. **Open（待处理）**
   - `system: true`
   - 包含所有未解决的会话（status != RESOLVED）
   - 不可删除、不可修改名称

2. **Resolved（已解决）**
   - `system: true`
   - 包含所有已解决的会话（status == RESOLVED）
   - 不可删除、不可修改名称

新会话默认会被分配到客服的"Open"分组。

---

## 注意事项

1. **分组独立性**: 每个客服的分组是独立的。同一个会话，不同客服可以放在不同的分组中。

2. **会话归属**: 只有会话的主责客服和支持客服可以对该会话进行分组操作。

3. **系统分组**: 系统分组（`system: true`）不可删除和重命名，但可以修改图标和颜色。

4. **分组删除**: 删除分组时，该分组下的所有会话会自动转移到默认分组（Open），不会丢失会话。接口会返回默认分组ID，方便前端立即更新UI。

5. **并发操作**: 多个客服可以同时对同一个会话进行分组操作，互不影响。

6. **认证要求**: 所有接口都需要客服认证（AgentPrincipal），客户（Customer）无法访问这些接口。

---

## 错误码说明

| HTTP状态码 | 说明 |
|-----------|------|
| 200 | 请求成功（带返回数据） |
| 201 | 创建成功 |
| 204 | 操作成功（无返回内容，如移动会话、移出分组） |
| 400 | 请求参数错误 |
| 401 | 未认证或认证失败 |
| 403 | 无权限操作（如试图修改他人的分组） |
| 404 | 资源不存在（分组或会话不存在） |
| 500 | 服务器内部错误 |

---

## 完整前端示例（React + TypeScript）

```typescript
import { useState, useEffect } from 'react';

interface SessionGroup {
  id: string;
  name: string;
  icon?: string;
  color?: string;
  sessions: ChatSession[];
}

interface ChatSession {
  id: string;
  sessionGroupId?: string;
  user: {
    name: string;
  };
  lastMessage?: {
    content: string;
  };
}

function SessionGroupManager() {
  const [groups, setGroups] = useState<SessionGroup[]>([]);
  const token = localStorage.getItem('authToken');

  // 加载所有分组
  useEffect(() => {
    fetchGroups();
  }, []);

  const fetchGroups = async () => {
    const response = await fetch('/api/v1/session-groups', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const data = await response.json();
    setGroups(data);
  };

  // 创建新分组
  const createGroup = async (name: string) => {
    await fetch('/api/v1/session-groups', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ name, icon: 'folder', color: '#2196F3' })
    });
    fetchGroups(); // 刷新列表
  };

  // 移动会话
  const moveSession = async (sessionId: string, groupId: string) => {
    await fetch(`/api/v1/session-groups/${groupId}/sessions/${sessionId}`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` }
    });
    fetchGroups(); // 刷新列表
  };

  // 删除分组
  const deleteGroup = async (groupId: string) => {
    if (!confirm('确定要删除这个分组吗？分组下的会话将移到默认分组。')) return;
    
    const response = await fetch(`/api/v1/session-groups/${groupId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${token}` }
    }).then(res => res.json());
    
    console.log(`会话已转移到默认分组: ${response.defaultGroupId}`);
    fetchGroups(); // 刷新列表
  };

  return (
    <div>
      <h1>会话分组管理</h1>
      <button onClick={() => createGroup('新分组')}>创建分组</button>
      
      {groups.map(group => (
        <div key={group.id} style={{ borderLeft: `4px solid ${group.color}` }}>
          <h2>{group.name} ({group.sessions.length})</h2>
          {!group.system && (
            <button onClick={() => deleteGroup(group.id)}>删除</button>
          )}
          
          <ul>
            {group.sessions.map(session => (
              <li key={session.id}>
                {session.user.name}: {session.lastMessage?.content}
              </li>
            ))}
          </ul>
        </div>
      ))}
    </div>
  );
}
```

---

## 相关接口

- **Bootstrap接口**: `GET /api/v1/bootstrap` - 获取客服工作台初始化数据（包含分组信息）
- **单个会话查询**: `GET /api/v1/chat/sessions/{sessionId}` - 获取会话详情（包含sessionGroupId）

---

**文档版本**: v1.0  
**最后更新**: 2024-01-15
