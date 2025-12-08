# Bootstrap API 响应与前端类型不匹配分析

## 🔴 核心问题

Bootstrap API 返回的数据结构与前端期望的 `ChatSession` 类型严重不匹配,导致应用崩溃。

## 📊 问题对比

### 后端返回的 Session 数据
```json
{
  "id": "0c8644cf-0bec-4c40-9961-5c0e5a010919",
  "status": "HUMAN_HANDLING",
  "lastActiveAt": "2025-11-25T09:55:24Z",  // ❌ 字段名错误
  "userId": null,                           // ❌ 缺少 user 对象->客户信息去掉userId 改为user对象
  "groupId": "e0843dbe-db32-48a5-b749-a602c76b1153",
  "primaryAgentId": "22222222-2222-2222-2222-222222222222",
  "supportAgentIds": []
  // ❌ 缺少 messages 数组
  // ❌ 缺少 unreadCount
}
```

### 前端期望的 ChatSession 类型
```typescript
export interface ChatSession {
  id: string;
  userId: string;
  user: UserProfile;              // ✅ 需要完整的 user 对象
  messages?: Message[];           // ✅ 需要 messages 数组
  status: ChatStatus;
  lastActive: number;             // ✅ 需要 lastActive (timestamp)
  unreadCount: number;            // ✅ 需要 unreadCount
  groupId: string;
  primaryAgentId: string;
  supportAgentIds: string[];
}
```

## 🚨 具体问题列表

### 1. **字段名不匹配**
| 后端字段 | 前端期望 | 问题 |
|---------|---------|------|
| `lastActiveAt` | `lastActive` | 字段名不同 |
| 无 | `lastActive` | 后端返回的是 ISO 字符串,前端期望的是时间戳(number) |

### 2. **缺少关键字段**
| 字段 | 类型 | 问题 |
|-----|------|------|
| `user` | `UserProfile` | ❌ 完全缺失,只有 `userId` (且为 null) |
| `messages` | `Message[]?` | ❌ 完全缺失 |
| `unreadCount` | `number` | ❌ 完全缺失 |

### 3. **数据类型问题**
- `lastActiveAt` 是 ISO 时间字符串 `"2025-11-25T09:55:24Z"`
- 前端期望 `lastActive` 是时间戳数字 (如 `1732530924000`)

### 4. **userId 为 null**
所有 session 的 `userId` 都是 `null`,这会导致:
- 无法关联用户信息
- 无法显示用户名、头像等
- 前面修复的所有 `session.user` 检查都会触发

