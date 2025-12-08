# AI 工作流分类绑定 API 文档

## 概述

AI 工作流支持绑定会话分类，实现根据客户会话的分类自动匹配对应的工作流进行处理。

### 关系说明

- **一个工作流可以绑定多个分类**
- **一个分类只能绑定一个工作流**（保证分类唯一对应）

### 工作流执行逻辑

```
客户发送消息 
    ↓
获取会话分类
    ↓
根据分类查找绑定的工作流
    ↓
如果没有绑定 → 使用默认工作流
    ↓
执行工作流
```

---

## 会话创建 API（含 metadata）

### 创建客户会话

```http
POST /api/v1/public/customer-token
Content-Type: application/json

{
  "name": "张三",
  "channel": "WEB",
  "email": "zhangsan@example.com",
  "phone": "13800138000",
  "channelUserId": "web_user_123",
  "metadata": {
    "categoryId": "uuid-of-category",
    "source": "homepage",
    "referrer": "https://www.google.com",
    "device": "mobile",
    "browser": "Chrome",
    "customField1": "value1",
    "customField2": 123
  }
}
```

**metadata 字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `categoryId` | String (UUID) | 会话分类ID，用于自动匹配工作流 |
| `source` | String | 来源渠道（如 homepage, product_page） |
| `referrer` | String | 来源页面 URL |
| `device` | String | 设备类型（mobile, desktop, tablet） |
| `browser` | String | 浏览器信息 |
| `*` | Any | 其他自定义字段 |

**响应**：

```json
{
  "customerId": "customer-uuid",
  "token": "cust_xxx",
  "name": "张三",
  "channel": "WEB",
  "sessionId": "session-uuid"
}
```

---

## 工作流 API 接口

### 基础路径

```
/api/v1/ai-workflows
```

---

## 1. 工作流 CRUD（含分类绑定）

### 1.1 创建工作流

```http
POST /api/v1/ai-workflows
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "售后服务工作流",
  "description": "处理售后相关问题的AI工作流",
  "nodesJson": "[...]",
  "edgesJson": "[...]",
  "triggerType": "CATEGORY",
  "triggerConfig": null,
  "categoryIds": [
    "category-uuid-1",
    "category-uuid-2"
  ]
}
```

**响应**：

```json
{
  "id": "workflow-uuid",
  "name": "售后服务工作流",
  "description": "处理售后相关问题的AI工作流",
  "nodesJson": "[...]",
  "edgesJson": "[...]",
  "liteflowEl": "THEN(...)",
  "version": 1,
  "enabled": false,
  "isDefault": false,
  "createdByAgentId": "agent-uuid",
  "createdByAgentName": "管理员",
  "triggerType": "CATEGORY",
  "triggerConfig": null,
  "categoryIds": ["category-uuid-1", "category-uuid-2"],
  "categories": [
    {
      "id": "category-uuid-1",
      "name": "退款咨询",
      "color": "#FF5722",
      "icon": "refund"
    },
    {
      "id": "category-uuid-2",
      "name": "物流查询",
      "color": "#2196F3",
      "icon": "logistics"
    }
  ],
  "createdAt": "2025-12-03T10:00:00Z",
  "updatedAt": "2025-12-03T10:00:00Z"
}
```

### 1.2 更新工作流

```http
PUT /api/v1/ai-workflows/{workflowId}
Content-Type: application/json

{
  "name": "售后服务工作流 V2",
  "description": "更新后的工作流",
  "nodesJson": "[...]",
  "edgesJson": "[...]",
  "categoryIds": [
    "category-uuid-1",
    "category-uuid-3"
  ]
}
```

> **注意**：更新时 `categoryIds` 会完全替换原有绑定

### 1.3 获取工作流详情

```http
GET /api/v1/ai-workflows/{workflowId}
```

**响应**：包含 `categoryIds` 和 `categories` 字段

### 1.4 获取所有工作流

```http
GET /api/v1/ai-workflows
```

**响应**：每个工作流都包含分类绑定信息

### 1.5 删除工作流

```http
DELETE /api/v1/ai-workflows/{workflowId}
```

> 删除工作流时会自动解除所有分类绑定

---

## 2. 分类绑定管理

### 2.1 获取可绑定分类列表（新建工作流时）

获取未被任何工作流绑定的分类。

```http
GET /api/v1/ai-workflows/available-categories
```

**响应**：

```json
[
  {
    "id": "category-uuid-1",
    "name": "退款咨询",
    "description": "处理退款相关问题",
    "icon": "refund",
    "color": "#FF5722",
    "sortOrder": 0
  },
  {
    "id": "category-uuid-2",
    "name": "物流查询",
    "description": "处理物流相关问题",
    "icon": "logistics",
    "color": "#2196F3",
    "sortOrder": 1
  }
]
```

### 2.2 获取可绑定分类列表（编辑工作流时）

获取未被**其他**工作流绑定的分类 + 当前工作流已绑定的分类。

```http
GET /api/v1/ai-workflows/{workflowId}/available-categories
```

**响应**：同上

### 2.3 获取工作流已绑定的分类ID

```http
GET /api/v1/ai-workflows/{workflowId}/categories
```

**响应**：

```json
["category-uuid-1", "category-uuid-2"]
```

### 2.4 单独更新分类绑定

```http
PUT /api/v1/ai-workflows/{workflowId}/categories
Content-Type: application/json

{
  "categoryIds": ["category-uuid-1", "category-uuid-3"]
}
```

**响应**：`200 OK`（无响应体）

### 2.5 根据分类查找工作流

```http
GET /api/v1/ai-workflows/by-category/{categoryId}
```

**响应**：返回绑定该分类的工作流（如果存在），否则返回 `null`

---

## 3. 错误处理

### 分类已被绑定

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "code": 400,
  "message": "分类 [退款咨询] 已被其他工作流绑定",
  "timestamp": "2025-12-03T10:00:00Z"
}
```

### 分类不存在

```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "code": 404,
  "message": "分类不存在: category-uuid",
  "timestamp": "2025-12-03T10:00:00Z"
}
```

---

## 4. 前端对接指南

### 4.1 新建工作流页面

1. **加载可绑定分类**
   ```javascript
   const categories = await fetch('/api/v1/ai-workflows/available-categories').then(r => r.json());
   ```

2. **创建工作流（含分类）**
   ```javascript
   const workflow = await fetch('/api/v1/ai-workflows', {
     method: 'POST',
     headers: { 'Content-Type': 'application/json' },
     body: JSON.stringify({
       name: '我的工作流',
       nodesJson: JSON.stringify(nodes),
       edgesJson: JSON.stringify(edges),
       categoryIds: selectedCategoryIds  // 选中的分类ID数组
     })
   }).then(r => r.json());
   ```

### 4.2 编辑工作流页面

1. **加载可绑定分类**（包含当前已绑定的）
   ```javascript
   const categories = await fetch(`/api/v1/ai-workflows/${workflowId}/available-categories`).then(r => r.json());
   ```

2. **加载工作流详情**（含已绑定分类）
   ```javascript
   const workflow = await fetch(`/api/v1/ai-workflows/${workflowId}`).then(r => r.json());
   // workflow.categoryIds 是已绑定的分类ID
   // workflow.categories 是分类详情
   ```

3. **更新工作流**
   ```javascript
   await fetch(`/api/v1/ai-workflows/${workflowId}`, {
     method: 'PUT',
     headers: { 'Content-Type': 'application/json' },
     body: JSON.stringify({
       ...workflowData,
       categoryIds: newSelectedCategoryIds
     })
   });
   ```

### 4.3 UI 设计建议

```
┌────────────────────────────────────────────────────────┐
│  工作流配置                                              │
├────────────────────────────────────────────────────────┤
│  名称: [售后服务工作流________________]                   │
│                                                        │
│  描述: [处理售后相关问题________________]                 │
│                                                        │
│  绑定分类:                                              │
│  ┌──────────────────────────────────────────────────┐ │
│  │  ☑ 退款咨询       ☑ 物流查询       ☐ 投诉建议   │ │
│  │  ☐ 商品咨询       ☐ 会员服务       ☐ 其他       │ │
│  │                                                  │ │
│  │  提示: 灰色选项已被其他工作流绑定                   │ │
│  └──────────────────────────────────────────────────┘ │
│                                                        │
│  [保存]  [取消]                                         │
└────────────────────────────────────────────────────────┘
```

---

## 5. 数据库表结构

```sql
CREATE TABLE workflow_category_bindings (
    id CHAR(36) PRIMARY KEY,
    workflow_id CHAR(36) NOT NULL,          -- 工作流ID
    category_id CHAR(36) NOT NULL UNIQUE,   -- 分类ID（唯一）
    priority INT DEFAULT 0,                 -- 优先级
    created_at DATETIME NOT NULL,
    
    FOREIGN KEY (workflow_id) REFERENCES ai_workflows(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES session_categories(id) ON DELETE CASCADE
);
```

---

## 6. 会话消息入口服务

`SessionMessageGateway` 用于AI工作流发送消息到WebSocket：

```java
@Resource
private SessionMessageGateway messageGateway;

// 发送 AI 消息
messageGateway.sendAiMessage(sessionId, "您好，请问有什么可以帮您？");

// 发送工作流消息（带元数据）
messageGateway.sendWorkflowMessage(sessionId, reply, workflowId, nodeId);

// 模拟客户消息
messageGateway.sendAsCustomer(sessionId, "我要退款");

// 模拟客服消息
messageGateway.sendAsAgent(sessionId, "好的，我来帮您处理", agentId);

// 发送系统消息
messageGateway.sendSystemMessage(sessionId, "会话已转接人工客服");
```

