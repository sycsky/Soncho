# AI 工作流测试模块 API 文档

## 概述

本模块提供一个对话框式的工作流测试功能，允许前端模拟用户发送消息来测试AI工作流的执行效果。支持多轮对话测试，测试会话保存在内存中，30分钟无活动后自动过期。

## 基础信息

| 项目 | 说明 |
|------|------|
| 基础路径 | `/api/v1/workflow-test` |
| 认证方式 | Bearer Token（需要登录）|
| Content-Type | `application/json` |

---

## API 接口

### 1. 创建测试会话

**请求**

```
POST /api/v1/workflow-test/sessions
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**

```json
{
  "workflowId": "550e8400-e29b-41d4-a716-446655440000",
  "variables": {
    "customerId": "xxx",
    "customerName": "测试用户"
  }
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `workflowId` | UUID | ✅ | 要测试的工作流ID |
| `variables` | Object | ❌ | 初始变量（可选） |

**响应**

```json
{
  "testSessionId": "test_a1b2c3d4e5f6",
  "workflowId": "550e8400-e29b-41d4-a716-446655440000",
  "workflowName": "客服工作流",
  "messages": [],
  "createdAt": "2024-12-15T10:30:00Z",
  "lastActiveAt": "2024-12-15T10:30:00Z"
}
```

---

### 2. 发送测试消息

**请求**

```
POST /api/v1/workflow-test/sessions/{testSessionId}/messages
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**

```json
{
  "message": "你好，我想咨询一下产品"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `message` | String | ✅ | 用户消息内容 |

**响应**

```json
{
  "testSessionId": "test_a1b2c3d4e5f6",
  "workflowId": "550e8400-e29b-41d4-a716-446655440000",
  "workflowName": "客服工作流",
  "messages": [
    {
      "id": "msg-uuid-1",
      "role": "user",
      "content": "你好，我想咨询一下产品",
      "timestamp": "2024-12-15T10:31:00Z",
      "meta": null
    },
    {
      "id": "msg-uuid-2",
      "role": "assistant",
      "content": "您好！很高兴为您服务。请问您想了解哪款产品呢？",
      "timestamp": "2024-12-15T10:31:02Z",
      "meta": {
        "success": true,
        "durationMs": 1850,
        "errorMessage": null,
        "needHumanTransfer": false,
        "nodeDetails": [
          {
            "nodeId": "intent_node_1",
            "nodeType": "intent",
            "input": "你好，我想咨询一下产品",
            "output": "产品咨询",
            "durationMs": 200,
            "success": true
          },
          {
            "nodeId": "llm_node_1",
            "nodeType": "llm",
            "input": "你好，我想咨询一下产品",
            "output": "您好！很高兴为您服务...",
            "durationMs": 1600,
            "success": true
          }
        ]
      }
    }
  ],
  "createdAt": "2024-12-15T10:30:00Z",
  "lastActiveAt": "2024-12-15T10:31:02Z"
}
```

---

### 3. 获取测试会话

**请求**

```
GET /api/v1/workflow-test/sessions/{testSessionId}
Authorization: Bearer <token>
```

**响应**

同上述 `WorkflowTestSessionDto` 格式。

---

### 4. 清除测试会话历史

清除对话历史和变量，但保留会话本身。

**请求**

```
POST /api/v1/workflow-test/sessions/{testSessionId}/clear
Authorization: Bearer <token>
```

**响应**

返回清空后的会话，`messages` 数组为空。

---

### 5. 删除测试会话

**请求**

```
DELETE /api/v1/workflow-test/sessions/{testSessionId}
Authorization: Bearer <token>
```

**响应**

- 成功：`204 No Content`

---

### 6. 设置测试变量

**请求**

```
PUT /api/v1/workflow-test/sessions/{testSessionId}/variables
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**

```json
{
  "customerId": "xxx",
  "categoryId": "yyy",
  "testMode": true
}
```

**响应**

返回更新后的会话。

---

### 7. 获取所有测试会话（管理用）

**请求**

```
GET /api/v1/workflow-test/sessions
Authorization: Bearer <token>
```

**响应**

```json
[
  {
    "testSessionId": "test_a1b2c3d4e5f6",
    "workflowId": "...",
    "workflowName": "客服工作流",
    "messages": [...],
    "createdAt": "...",
    "lastActiveAt": "..."
  }
]
```

---

## 数据结构

### WorkflowTestSessionDto

| 字段 | 类型 | 说明 |
|------|------|------|
| `testSessionId` | String | 测试会话ID（格式：`test_xxxxxx`）|
| `workflowId` | UUID | 工作流ID |
| `workflowName` | String | 工作流名称 |
| `messages` | TestMessage[] | 消息列表 |
| `createdAt` | ISO DateTime | 创建时间 |
| `lastActiveAt` | ISO DateTime | 最后活跃时间 |

### TestMessage

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | String | 消息ID |
| `role` | String | 角色：`user` / `assistant` / `system` |
| `content` | String | 消息内容 |
| `timestamp` | ISO DateTime | 时间戳 |
| `meta` | TestMessageMeta | 元数据（仅 assistant 消息有）|

### TestMessageMeta

| 字段 | 类型 | 说明 |
|------|------|------|
| `success` | Boolean | 执行是否成功 |
| `durationMs` | Long | 执行耗时（毫秒）|
| `errorMessage` | String | 错误信息（失败时）|
| `needHumanTransfer` | Boolean | 是否需要转人工 |
| `nodeDetails` | NodeDetail[] | 节点执行详情 |

### NodeDetail

| 字段 | 类型 | 说明 |
|------|------|------|
| `nodeId` | String | 节点ID |
| `nodeType` | String | 节点类型（`start`/`intent`/`llm`/`end`等）|
| `input` | String | 输入 |
| `output` | String | 输出 |
| `durationMs` | Long | 耗时（毫秒）|
| `success` | Boolean | 是否成功 |

---

## 前端使用示例

### React 组件示例

```tsx
import { useState, useEffect } from 'react';

interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
  meta?: {
    success: boolean;
    durationMs: number;
    errorMessage?: string;
    nodeDetails?: Array<{
      nodeId: string;
      nodeType: string;
      input: string;
      output: string;
      durationMs: number;
      success: boolean;
    }>;
  };
}

interface TestSession {
  testSessionId: string;
  workflowId: string;
  workflowName: string;
  messages: Message[];
  createdAt: string;
  lastActiveAt: string;
}

function WorkflowTestDialog({ 
  workflowId, 
  onClose 
}: { 
  workflowId: string; 
  onClose: () => void;
}) {
  const [testSessionId, setTestSessionId] = useState<string | null>(null);
  const [workflowName, setWorkflowName] = useState('');
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);

  // 创建测试会话
  useEffect(() => {
    const createSession = async () => {
      const response = await fetch('/api/v1/workflow-test/sessions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({ workflowId })
      });
      const data: TestSession = await response.json();
      setTestSessionId(data.testSessionId);
      setWorkflowName(data.workflowName);
    };
    createSession();
  }, [workflowId]);

  // 发送消息
  const sendMessage = async () => {
    if (!input.trim() || !testSessionId || loading) return;

    setLoading(true);
    const userMessage = input;
    setInput('');

    try {
      const response = await fetch(
        `/api/v1/workflow-test/sessions/${testSessionId}/messages`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          },
          body: JSON.stringify({ message: userMessage })
        }
      );
      const data: TestSession = await response.json();
      setMessages(data.messages);
    } catch (error) {
      console.error('发送消息失败', error);
    } finally {
      setLoading(false);
    }
  };

  // 清除历史
  const clearHistory = async () => {
    if (!testSessionId) return;
    
    const response = await fetch(
      `/api/v1/workflow-test/sessions/${testSessionId}/clear`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      }
    );
    const data: TestSession = await response.json();
    setMessages(data.messages);
  };

  // 删除会话（关闭时）
  const handleClose = async () => {
    if (testSessionId) {
      await fetch(
        `/api/v1/workflow-test/sessions/${testSessionId}`,
        {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        }
      );
    }
    onClose();
  };

  return (
    <div className="workflow-test-dialog">
      <div className="header">
        <h3>测试: {workflowName}</h3>
        <div className="actions">
          <button onClick={clearHistory} disabled={loading}>
            清除历史
          </button>
          <button onClick={handleClose}>关闭</button>
        </div>
      </div>
      
      <div className="messages">
        {messages.length === 0 && (
          <div className="empty-state">
            发送消息开始测试工作流
          </div>
        )}
        {messages.map((msg) => (
          <div key={msg.id} className={`message ${msg.role}`}>
            <div className="role-label">
              {msg.role === 'user' ? '用户' : 'AI'}
            </div>
            <div className="content">{msg.content}</div>
            {msg.meta && (
              <div className="meta">
                <span className={msg.meta.success ? 'success' : 'error'}>
                  {msg.meta.success ? '✅ 成功' : '❌ 失败'}
                </span>
                <span className="duration">
                  耗时: {msg.meta.durationMs}ms
                </span>
                {msg.meta.errorMessage && (
                  <span className="error-msg">
                    {msg.meta.errorMessage}
                  </span>
                )}
              </div>
            )}
          </div>
        ))}
        {loading && (
          <div className="message assistant loading">
            <div className="typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>
        )}
      </div>
      
      <div className="input-area">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && sendMessage()}
          placeholder="输入测试消息..."
          disabled={loading || !testSessionId}
        />
        <button 
          onClick={sendMessage} 
          disabled={loading || !testSessionId || !input.trim()}
        >
          发送
        </button>
      </div>
    </div>
  );
}

export default WorkflowTestDialog;
```

### CSS 样式示例

```css
.workflow-test-dialog {
  display: flex;
  flex-direction: column;
  height: 600px;
  width: 500px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.workflow-test-dialog .header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #f5f5f5;
  border-bottom: 1px solid #e0e0e0;
}

.workflow-test-dialog .header h3 {
  margin: 0;
  font-size: 16px;
}

.workflow-test-dialog .messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.workflow-test-dialog .message {
  max-width: 80%;
  padding: 12px;
  border-radius: 12px;
}

.workflow-test-dialog .message.user {
  align-self: flex-end;
  background: #007bff;
  color: white;
}

.workflow-test-dialog .message.assistant {
  align-self: flex-start;
  background: #f0f0f0;
}

.workflow-test-dialog .message .role-label {
  font-size: 12px;
  opacity: 0.7;
  margin-bottom: 4px;
}

.workflow-test-dialog .message .meta {
  margin-top: 8px;
  font-size: 12px;
  opacity: 0.8;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.workflow-test-dialog .message .meta .success {
  color: #28a745;
}

.workflow-test-dialog .message .meta .error {
  color: #dc3545;
}

.workflow-test-dialog .input-area {
  display: flex;
  gap: 8px;
  padding: 16px;
  border-top: 1px solid #e0e0e0;
}

.workflow-test-dialog .input-area input {
  flex: 1;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
}

.workflow-test-dialog .input-area button {
  padding: 12px 24px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.workflow-test-dialog .input-area button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.workflow-test-dialog .empty-state {
  text-align: center;
  color: #999;
  padding: 40px;
}

.workflow-test-dialog .typing-indicator {
  display: flex;
  gap: 4px;
}

.workflow-test-dialog .typing-indicator span {
  width: 8px;
  height: 8px;
  background: #999;
  border-radius: 50%;
  animation: typing 1s infinite;
}

.workflow-test-dialog .typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.workflow-test-dialog .typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}
```

---

## 注意事项

1. **会话过期**：测试会话在30分钟无活动后自动过期删除
2. **内存存储**：测试会话存储在服务器内存中，服务重启后会丢失
3. **无真实会话**：测试不会创建真实的 `ChatSession`，不影响生产数据
4. **节点详情**：`nodeDetails` 包含工作流每个节点的执行信息，方便调试
5. **并发限制**：建议同一用户不要创建过多测试会话，避免内存占用过大

---

## 错误响应

### 会话不存在

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "测试会话不存在或已过期: test_xxxxxx"
}
```

### 工作流不存在

```json
{
  "status": 400,
  "error": "Bad Request", 
  "message": "工作流不存在: 550e8400-e29b-41d4-a716-446655440000"
}
```

---

**文档版本**: v1.0  
**最后更新**: 2024-12-15  
**维护团队**: AI 开发组

