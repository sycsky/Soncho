# 转移会话 API 文档

## 功能概述

转移会话功能允许主要负责客服将会话转移给其他客服。转移后，新客服将成为该会话的主要负责人。

### 权限说明

⚠️ **重要**：只有当前会话的**主要负责客服**才能执行转移操作。支持客服无法转移会话。

## API 接口

### 1. 获取可转移的客服列表

获取可以接收会话转移的客服列表，排除当前会话的主要负责客服。

#### 请求

```
GET /api/v1/chat/sessions/{sessionId}/transferable-agents
```

#### 请求头

```
Authorization: Bearer {agent_token}
```

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| sessionId | UUID | 是 | 会话ID |

#### 响应

**成功 (200 OK)**

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "username": "agent_zhang",
    "name": "张三",
    "email": "zhang@example.com",
    "avatarUrl": "https://example.com/avatar/zhang.png",
    "status": "ONLINE",
    "role": {
      "id": "11111111-1111-1111-1111-111111111111",
      "name": "客服主管",
      "level": 2
    }
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440002",
    "username": "agent_li",
    "name": "李四",
    "email": "li@example.com",
    "avatarUrl": null,
    "status": "ONLINE",
    "role": {
      "id": "22222222-2222-2222-2222-222222222222",
      "name": "普通客服",
      "level": 1
    }
  }
]
```

**错误响应**

| 状态码 | 说明 |
|-------|------|
| 401 | 未认证 |
| 403 | 非主要负责客服，无权限 |
| 404 | 会话不存在 |

---

### 2. 转移会话

将会话转移给新的主要负责客服。

#### 请求

```
POST /api/v1/chat/sessions/{sessionId}/transfer
```

#### 请求头

```
Authorization: Bearer {agent_token}
Content-Type: application/json
```

#### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| sessionId | UUID | 是 | 会话ID |

#### 请求体

```json
{
  "targetAgentId": "550e8400-e29b-41d4-a716-446655440001",
  "keepAsSupport": false
}
```

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| targetAgentId | UUID | 是 | 目标客服ID |
| keepAsSupport | boolean | 否 | 是否将原主要客服保留为支持客服，默认 `false` |

#### 响应

**成功 (204 No Content)**

无响应体

**错误响应**

| 状态码 | 说明 |
|-------|------|
| 400 | 请求参数错误（如：转移给自己） |
| 401 | 未认证 |
| 403 | 非主要负责客服，无权限 |
| 404 | 会话或目标客服不存在 |

---

## 前端接入示例

### TypeScript 类型定义

```typescript
// 客服信息
interface AgentDto {
  id: string;
  username: string;
  name: string;
  email: string | null;
  avatarUrl: string | null;
  status: 'ONLINE' | 'OFFLINE' | 'BUSY' | 'AWAY';
  role: {
    id: string;
    name: string;
    level: number;
  } | null;
}

// 转移会话请求
interface TransferSessionRequest {
  targetAgentId: string;
  keepAsSupport?: boolean;
}
```

### API 调用示例

```typescript
// 获取可转移的客服列表
async function getTransferableAgents(sessionId: string): Promise<AgentDto[]> {
  const response = await fetch(
    `/api/v1/chat/sessions/${sessionId}/transferable-agents`,
    {
      headers: {
        'Authorization': `Bearer ${agentToken}`
      }
    }
  );
  
  if (!response.ok) {
    if (response.status === 403) {
      throw new Error('只有主要负责客服可以转移会话');
    }
    throw new Error('获取可转移客服列表失败');
  }
  
  return response.json();
}

// 转移会话
async function transferSession(
  sessionId: string, 
  targetAgentId: string,
  keepAsSupport: boolean = false
): Promise<void> {
  const response = await fetch(
    `/api/v1/chat/sessions/${sessionId}/transfer`,
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${agentToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        targetAgentId,
        keepAsSupport
      })
    }
  );
  
  if (!response.ok) {
    if (response.status === 403) {
      throw new Error('只有主要负责客服可以转移会话');
    }
    if (response.status === 400) {
      throw new Error('不能将会话转移给自己');
    }
    throw new Error('转移会话失败');
  }
}
```

### React 组件示例

```tsx
import React, { useState, useEffect } from 'react';

interface TransferDialogProps {
  sessionId: string;
  isOpen: boolean;
  onClose: () => void;
  onTransferSuccess: () => void;
}

const TransferDialog: React.FC<TransferDialogProps> = ({
  sessionId,
  isOpen,
  onClose,
  onTransferSuccess
}) => {
  const [agents, setAgents] = useState<AgentDto[]>([]);
  const [selectedAgentId, setSelectedAgentId] = useState<string | null>(null);
  const [keepAsSupport, setKeepAsSupport] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 加载可转移的客服列表
  useEffect(() => {
    if (isOpen) {
      loadTransferableAgents();
    }
  }, [isOpen, sessionId]);

  const loadTransferableAgents = async () => {
    try {
      setLoading(true);
      setError(null);
      const agentList = await getTransferableAgents(sessionId);
      setAgents(agentList);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载失败');
    } finally {
      setLoading(false);
    }
  };

  const handleTransfer = async () => {
    if (!selectedAgentId) return;
    
    try {
      setLoading(true);
      setError(null);
      await transferSession(sessionId, selectedAgentId, keepAsSupport);
      onTransferSuccess();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : '转移失败');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal">
      <div className="modal-header">
        <h3>转移会话</h3>
        <button onClick={onClose}>×</button>
      </div>
      
      <div className="modal-body">
        {error && <div className="error-message">{error}</div>}
        
        {loading ? (
          <div className="loading">加载中...</div>
        ) : (
          <>
            <div className="agent-list">
              <label>选择目标客服：</label>
              {agents.map(agent => (
                <div 
                  key={agent.id}
                  className={`agent-item ${selectedAgentId === agent.id ? 'selected' : ''}`}
                  onClick={() => setSelectedAgentId(agent.id)}
                >
                  <img 
                    src={agent.avatarUrl || '/default-avatar.png'} 
                    alt={agent.name}
                    className="avatar"
                  />
                  <div className="agent-info">
                    <span className="name">{agent.name}</span>
                    <span className="role">{agent.role?.name}</span>
                    <span className={`status ${agent.status.toLowerCase()}`}>
                      {agent.status === 'ONLINE' ? '在线' : 
                       agent.status === 'BUSY' ? '忙碌' :
                       agent.status === 'AWAY' ? '离开' : '离线'}
                    </span>
                  </div>
                </div>
              ))}
            </div>
            
            <div className="options">
              <label>
                <input
                  type="checkbox"
                  checked={keepAsSupport}
                  onChange={(e) => setKeepAsSupport(e.target.checked)}
                />
                保留我为支持客服
              </label>
            </div>
          </>
        )}
      </div>
      
      <div className="modal-footer">
        <button onClick={onClose}>取消</button>
        <button 
          onClick={handleTransfer}
          disabled={!selectedAgentId || loading}
          className="primary"
        >
          确认转移
        </button>
      </div>
    </div>
  );
};

export default TransferDialog;
```

---

## 业务流程

```
┌─────────────────┐
│  主要负责客服    │
│  点击"转移会话"  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ GET /transferable│
│     -agents     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  显示可转移的    │
│  客服列表        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  选择目标客服    │
│  是否保留支持    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ POST /transfer  │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────┐
│           后端处理逻辑               │
├─────────────────────────────────────┤
│ 1. 验证当前用户是主要负责客服         │
│ 2. 验证目标客服存在                   │
│ 3. 更新会话主要负责客服               │
│ 4. 处理支持客服列表                   │
│ 5. 更新分组映射关系                   │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────┐
│  转移成功        │
│  前端更新UI      │
└─────────────────┘
```

---

## 转移逻辑说明

### keepAsSupport = false（默认）

1. 新客服成为主要负责客服
2. 原主要客服从会话中移除
3. 原主要客服的分组映射被删除

### keepAsSupport = true

1. 新客服成为主要负责客服
2. 原主要客服变为支持客服
3. 原主要客服保留分组映射

### 特殊情况处理

- **目标客服原本是支持客服**：从支持客服列表中移除，升级为主要负责客服
- **转移给自己**：返回 400 错误
- **非主要负责客服操作**：返回 403 错误

---

## WebSocket 事件（可选扩展）

转移会话后，可以通过 WebSocket 通知相关客服：

```json
{
  "type": "session_transferred",
  "payload": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "fromAgentId": "old-agent-id",
    "toAgentId": "new-agent-id",
    "keepAsSupport": false
  }
}
```

> 注意：WebSocket 事件通知需要后端额外实现

---

## 常见问题

### Q: 支持客服可以转移会话吗？

A: 不可以。只有主要负责客服才有权限转移会话。

### Q: 转移后原客服还能看到这个会话吗？

A: 取决于 `keepAsSupport` 参数：
- `false`：不能，会话从原客服的分组中移除
- `true`：可以，原客服变为支持客服

### Q: 转移后会话的历史消息会丢失吗？

A: 不会。所有历史消息都会保留，新的主要负责客服可以看到完整的聊天记录。

### Q: 可以同时转移给多个客服吗？

A: 不可以。一次只能转移给一个客服作为主要负责人。如需添加多个支持客服，请使用分配支持客服接口。

