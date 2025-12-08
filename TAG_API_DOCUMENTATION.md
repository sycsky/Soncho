# 客户标签管理 API 对接文档

## 概述

客户标签管理 API 提供了对客户标签的完整管理功能，支持区分**手动标签**（客服手动添加）和 **AI 标签**（AI 自动生成）。

### 基础信息

- **基础路径**: `/api/v1/customers/{customerId}/tags`
- **认证方式**: 需要 Agent 认证
- **内容类型**: `application/json`

### 重要说明

- 标签属于**客户（Customer）**实体，使用 `customerId` 作为标识
- 数据存储在 `user_tags` 和 `user_ai_tags` 表中（表名虽为 user，但实际存储的是 Customer 的标签）

---

## API 端点列表

| 方法 | 路径 | 功能 | 标签类型 |
|------|------|------|---------|
| GET | `/api/v1/customers/{customerId}/tags` | 获取所有标签 | 手动 + AI |
| GET | `/api/v1/customers/{customerId}/tags/manual` | 获取手动标签 | 手动 |
| GET | `/api/v1/customers/{customerId}/tags/ai` | 获取 AI 标签 | AI |
| POST | `/api/v1/customers/{customerId}/tags/manual` | 添加手动标签 | 手动 |
| DELETE | `/api/v1/customers/{customerId}/tags/manual` | 删除手动标签 | 手动 |
| POST | `/api/v1/customers/{customerId}/tags/ai` | 添加 AI 标签 | AI |
| DELETE | `/api/v1/customers/{customerId}/tags/ai` | 删除 AI 标签 | AI |
| PUT | `/api/v1/customers/{customerId}/tags/manual` | 批量设置手动标签 | 手动 |
| PUT | `/api/v1/customers/{customerId}/tags/ai` | 批量设置 AI 标签 | AI |

---

## 详细接口说明

### 1. 获取客户所有标签

获取客户的所有标签信息，包括手动标签和 AI 标签。

**请求**

```http
GET /api/v1/customers/{customerId}/tags
```

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| customerId | UUID | 是 | 客户 ID |

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "张三",
    "primaryChannel": "WECHAT",
    "email": "zhangsan@example.com",
    "phone": "13800138000",
    "wechatOpenId": "oABC123xyz",
    "whatsappId": null,
    "lineId": null,
    "telegramId": null,
    "facebookId": null,
    "avatarUrl": "https://example.com/avatar.jpg",
    "location": "北京",
    "notes": "这是客户备注",
    "metadata": {
      "company": "ABC公司",
      "position": "技术总监"
    },
    "active": true,
    "lastInteractionAt": "2025-01-26T10:30:00Z",
    "createdAt": "2025-01-01T08:00:00Z",
    "tags": [
      "VIP客户",
      "企业客户",
      "重点关注"
    ],
    "aiTags": [
      "潜在流失",
      "高价值",
      "活跃客户"
    ]
  }
}
```

**响应字段说明**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 客户 ID |
| name | String | 客户姓名 |
| primaryChannel | String | 主要联系渠道 (WECHAT/WHATSAPP/LINE/TELEGRAM/FACEBOOK/EMAIL/PHONE/WEB/APP/SMS) |
| email | String | 邮箱 |
| phone | String | 手机号 |
| wechatOpenId | String | 微信 OpenID |
| whatsappId | String | WhatsApp ID |
| lineId | String | Line ID |
| telegramId | String | Telegram ID |
| facebookId | String | Facebook ID |
| avatarUrl | String | 头像 URL |
| location | String | 位置 |
| notes | String | 客户备注 |
| metadata | Object | 自定义字段 |
| active | Boolean | 是否活跃 |
| lastInteractionAt | Timestamp | 最后互动时间 |
| createdAt | Timestamp | 创建时间 |
| tags | Array<String> | 手动添加的标签列表 |
| aiTags | Array<String> | AI 生成的标签列表 |

---

### 2. 获取手动标签

仅获取客服手动添加的标签。

**请求**

```http
GET /api/v1/customers/{customerId}/tags/manual
```

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| customerId | UUID | 是 | 客户 ID |

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    "VIP客户",
    "企业客户",
    "重点关注"
  ]
}
```

---

### 3. 获取 AI 标签

仅获取 AI 自动生成的标签。

**请求**

```http
GET /api/v1/customers/{customerId}/tags/ai
```

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| customerId | UUID | 是 | 客户 ID |

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    "潜在流失",
    "高价值",
    "活跃客户"
  ]
}
```

---

### 4. 添加手动标签

客服手动为客户添加标签。

**请求**

```http
POST /api/v1/customers/{customerId}/tags/manual
Content-Type: application/json
```

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| customerId | UUID | 是 | 客户 ID |

**请求体**

```json
{
  "tag": "VIP客户"
}
```

**请求字段说明**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| tag | String | 是 | 要添加的标签名称 |

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "张三",
    "primaryChannel": "WECHAT",
    "email": "zhangsan@example.com",
    "phone": "13800138000",
    "tags": [
      "VIP客户",
      "企业客户"
    ],
    "aiTags": [
      "潜在流失",
      "高价值"
    ],
    ...
  }
}
```

**注意事项**

- 如果标签已存在，不会重复添加
- 标签名称区分大小写

---

### 5. 删除手动标签

客服手动删除客户的标签。

**请求**

```http
DELETE /api/v1/customers/{customerId}/tags/manual
Content-Type: application/json
```

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| customerId | UUID | 是 | 客户 ID |

**请求体**

```json
{
  "tag": "VIP客户"
}
```

**请求字段说明**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| tag | String | 是 | 要删除的标签名称 |

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "张三",
    "tags": [
      "企业客户"
    ],
    "aiTags": [
      "潜在流失",
      "高价值"
    ],
    ...
  }
}
```

**注意事项**

- 如果标签不存在，不会报错，直接返回成功

---

### 6. 添加 AI 标签

AI 系统自动为客户添加标签。

**请求**

```http
POST /api/v1/customers/{customerId}/tags/ai
Content-Type: application/json
```

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| customerId | UUID | 是 | 客户 ID |

**请求体**

```json
{
  "tag": "潜在流失"
}
```

**请求字段说明**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| tag | String | 是 | 要添加的 AI 标签名称 |

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "张三",
    "tags": [
      "VIP客户"
    ],
    "aiTags": [
      "潜在流失",
      "高价值"
    ],
    ...
  }
}
```

**使用场景**

- AI 分析客户行为后自动打标签
- 智能客服系统根据对话内容打标签
- 风控系统根据客户行为打风险标签

---

### 7. 删除 AI 标签

AI 系统自动删除客户的标签。

**请求**

```http
DELETE /api/v1/customers/{customerId}/tags/ai
Content-Type: application/json
```

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| customerId | UUID | 是 | 客户 ID |

**请求体**

```json
{
  "tag": "潜在流失"
}
```

**请求字段说明**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| tag | String | 是 | 要删除的 AI 标签名称 |

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "张三",
    "tags": [
      "VIP客户"
    ],
    "aiTags": [
      "高价值"
    ],
    ...
  }
}
```

---

### 8. 批量设置手动标签

批量设置客户的手动标签，会覆盖原有的手动标签。

**请求**

```http
PUT /api/v1/customers/{customerId}/tags/manual
Content-Type: application/json
```

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| customerId | UUID | 是 | 客户 ID |

**请求体**

```json
[
  "VIP客户",
  "企业客户",
  "重点关注",
  "技术咨询"
]
```

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "张三",
    "tags": [
      "VIP客户",
      "企业客户",
      "重点关注",
      "技术咨询"
    ],
    "aiTags": [
      "潜在流失",
      "高价值"
    ],
    ...
  }
}
```

**注意事项**

- 此操作会**完全覆盖**原有的手动标签
- 传入空数组 `[]` 将清空所有手动标签
- AI 标签不受影响

---

### 9. 批量设置 AI 标签

批量设置客户的 AI 标签，会覆盖原有的 AI 标签。

**请求**

```http
PUT /api/v1/customers/{customerId}/tags/ai
Content-Type: application/json
```

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| customerId | UUID | 是 | 客户 ID |

**请求体**

```json
[
  "潜在流失",
  "高价值",
  "活跃客户",
  "技术型客户"
]
```

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "张三",
    "tags": [
      "VIP客户",
      "企业客户"
    ],
    "aiTags": [
      "潜在流失",
      "高价值",
      "活跃客户",
      "技术型客户"
    ],
    ...
  }
}
```

**注意事项**

- 此操作会**完全覆盖**原有的 AI 标签
- 传入空数组 `[]` 将清空所有 AI 标签
- 手动标签不受影响

---

## 错误码说明

| HTTP 状态码 | 错误码 | 说明 |
|-------------|--------|------|
| 400 | 400 | 请求参数错误 |
| 401 | 401 | 未授权，需要登录 |
| 403 | 403 | 无权限访问 |
| 404 | 404 | 客户不存在 |
| 500 | 500 | 服务器内部错误 |

**错误响应示例**

```json
{
  "code": 404,
  "message": "客户不存在",
  "data": null
}
```

---

## 使用场景示例

### 场景 1: 客服标记 VIP 客户

```javascript
// 1. 客服手动添加 VIP 标签
POST /api/v1/customers/550e8400-e29b-41d4-a716-446655440000/tags/manual
{
  "tag": "VIP客户"
}

// 2. 查看客户所有标签
GET /api/v1/customers/550e8400-e29b-41d4-a716-446655440000/tags
```

### 场景 2: AI 系统分析客户行为

```javascript
// AI 系统分析后批量设置标签
PUT /api/v1/customers/550e8400-e29b-41d4-a716-446655440000/tags/ai
[
  "高活跃度",
  "购买意向强",
  "价格敏感"
]
```

### 场景 3: 客户画像展示

```javascript
// 获取客户完整信息（包括所有标签）
GET /api/v1/customers/550e8400-e29b-41d4-a716-446655440000/tags

// 前端展示时区分手动标签和 AI 标签
// 手动标签: 蓝色标记，可编辑
// AI 标签: 绿色标记，只读
```

### 场景 4: 标签清理

```javascript
// 清空所有 AI 标签
PUT /api/v1/customers/550e8400-e29b-41d4-a716-446655440000/tags/ai
[]

// 或逐个删除
DELETE /api/v1/customers/550e8400-e29b-41d4-a716-446655440000/tags/ai
{
  "tag": "过期标签"
}
```

---

## 前端集成建议

### 1. 标签展示

```javascript
// 获取客户标签
const response = await fetch(`/api/v1/customers/${customerId}/tags`);
const customer = await response.json();

// 展示手动标签（蓝色，可编辑）
customer.data.tags.forEach(tag => {
  renderTag(tag, 'manual', '#1890ff');
});

// 展示 AI 标签（绿色，只读）
customer.data.aiTags.forEach(tag => {
  renderTag(tag, 'ai', '#52c41a');
});
```

### 2. 添加标签

```javascript
// 添加手动标签
async function addManualTag(customerId, tagName) {
  const response = await fetch(`/api/v1/customers/${customerId}/tags/manual`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ tag: tagName })
  });
  return await response.json();
}
```

### 3. 删除标签

```javascript
// 删除手动标签
async function removeManualTag(customerId, tagName) {
  const response = await fetch(`/api/v1/customers/${customerId}/tags/manual`, {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ tag: tagName })
  });
  return await response.json();
}
```

---

## 数据库结构说明

### user_tags 表（手动标签）

| 字段 | 类型 | 说明 |
|------|------|------|
| user_id | UUID | 客户 ID（外键，关联 customers 表） |
| tag | VARCHAR(255) | 标签名称 |

### user_ai_tags 表（AI 标签）

| 字段 | 类型 | 说明 |
|------|------|------|
| user_id | UUID | 客户 ID（外键，关联 customers 表） |
| tag | VARCHAR(255) | AI 标签名称 |

**注意**：
- 两个表的结构相同，但逻辑上分离存储，便于区分管理和权限控制
- 表名虽为 `user_tags` 和 `user_ai_tags`，但 `user_id` 列实际存储的是 Customer 的 ID（对应 `customers` 表的 `id`）

---

## 常见问题 FAQ

### Q1: 手动标签和 AI 标签有什么区别？

**A**: 
- **手动标签**：由客服人员手动添加，用于标记客户特征、需求等
- **AI 标签**：由 AI 系统自动生成，基于客户行为分析、智能推荐等
- 两者在数据库中分开存储，前端展示时建议用不同颜色区分

### Q2: 可以将 AI 标签转为手动标签吗？

**A**: 可以，但需要分两步操作：
1. 删除 AI 标签：`DELETE /api/v1/customers/{customerId}/tags/ai`
2. 添加手动标签：`POST /api/v1/customers/{customerId}/tags/manual`

### Q3: 标签名称有长度限制吗？

**A**: 建议标签名称不超过 50 个字符，以保证良好的展示效果。

### Q4: 批量设置标签会影响另一类标签吗？

**A**: 不会。批量设置手动标签只影响手动标签，AI 标签保持不变，反之亦然。

### Q5: customerId 从哪里获取？

**A**: 
- 在 Bootstrap 接口返回的 session 列表中，每个 session 都包含 `customer.id`
- 也可以从客户列表 API 中获取

### Q6: 为什么表名是 user_tags 但存储的是 Customer 数据？

**A**: 这是历史遗留命名，表名虽为 `user_tags`，但 `user_id` 列实际关联的是 `customers` 表的 `id`。在 API 层面，我们使用 `customerId` 作为参数名以保持语义清晰。

---

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.1 | 2025-01-26 | 更新：标签从 UserProfile 迁移到 Customer 实体，API 路径从 /users 改为 /customers |
| v1.0 | 2025-01-26 | 初始版本，支持手动标签和 AI 标签的完整管理 |

---

## 技术支持

如有问题，请联系技术支持团队。
