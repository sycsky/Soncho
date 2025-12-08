# 客户模块 API 总结

## 概述

客户模块提供了完整的客户管理和 WebSocket 接入功能，支持多渠道客户接入（Web、微信、WhatsApp、Line 等）。

---

## 公开 API（无需认证）

### 1. 获取客户 Token

**端点**: `POST /api/v1/public/customer-token`

**说明**: 为客户快速生成访问 Token，如果客户不存在则自动创建。

**请求体**:
```json
{
  "name": "张三",
  "channel": "WEB",
  "channelId": "web_user_123456"
}
```

**支持的渠道**:
- `WEB` - 网页
- `WECHAT` - 微信
- `WHATSAPP` - WhatsApp
- `LINE` - Line
- `TELEGRAM` - Telegram
- `FACEBOOK` - Facebook Messenger
- `EMAIL` - 邮件
- `SMS` - 短信
- `PHONE` - 电话
- `APP` - 移动应用

**响应**:
```json
{
  "success": true,
  "data": {
    "customerId": "550e8400-e29b-41d4-a716-446655440000",
    "token": "cust_a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "name": "张三",
    "channel": "WEB"
  }
}
```

---

## 客户管理 API（需要坐席认证）

### 2. 查询客户列表

**端点**: `GET /api/v1/customers`

**查询参数**:
- `name` (可选) - 客户姓名（模糊查询）
- `channel` (可选) - 渠道类型
- `tag` (可选) - 标签（模糊查询）
- `active` (可选) - 是否活跃
- `page` (可选) - 页码，默认 0
- `size` (可选) - 每页数量，默认 20
- `sort` (可选) - 排序字段，默认 `createdAt,desc`

**示例**:
```
GET /api/v1/customers?name=张&channel=WEB&active=true&page=0&size=20
Authorization: Bearer {agent-token}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "name": "张三",
        "primaryChannel": "WEB",
        "email": "zhangsan@example.com",
        "phone": "+8613800138000",
        "wechatOpenId": null,
        "whatsappId": null,
        "lineId": null,
        "telegramId": null,
        "facebookId": null,
        "avatarUrl": "https://...",
        "location": "北京",
        "notes": "VIP 客户",
        "tags": ["VIP", "已购买"],
        "customFields": {
          "industry": "IT",
          "company": "ABC Corp"
        },
        "active": true,
        "lastInteractionAt": "2024-01-15T10:30:00Z",
        "createdAt": "2024-01-01T08:00:00Z"
      }
    ],
    "pageable": {...},
    "totalElements": 100,
    "totalPages": 5,
    "size": 20,
    "number": 0
  }
}
```

### 3. 获取客户详情

**端点**: `GET /api/v1/customers/{customerId}`

**示例**:
```
GET /api/v1/customers/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer {agent-token}
```

**响应**: 同上客户对象

### 4. 创建客户

**端点**: `POST /api/v1/customers`

**请求体**:
```json
{
  "name": "李四",
  "primaryChannel": "WECHAT",
  "email": "lisi@example.com",
  "phone": "+8613900139000",
  "wechatOpenId": "oAbCd1234567890",
  "location": "上海",
  "notes": "潜在客户",
  "tags": ["潜在客户", "高意向"],
  "customFields": {
    "source": "广告投放",
    "budget": "10000"
  }
}
```

**必填字段**:
- `name` - 客户姓名
- `primaryChannel` - 主要渠道

**响应**: 创建的客户对象

### 5. 更新客户信息

**端点**: `PUT /api/v1/customers/{customerId}`

**请求体**: 所有字段可选
```json
{
  "name": "李四（VIP）",
  "phone": "+8613900139001",
  "tags": ["VIP", "已购买"],
  "notes": "已成交，满意度高",
  "active": true
}
```

**响应**: 更新后的客户对象

### 6. 删除客户

**端点**: `DELETE /api/v1/customers/{customerId}`

**响应**: 204 No Content

### 7. 为客户生成 Token

**端点**: `POST /api/v1/customers/{customerId}/token`

**说明**: 为已存在的客户生成 WebSocket 连接 Token

**响应**:
```json
{
  "success": true,
  "data": {
    "customerId": "uuid",
    "token": "cust_xxxxxxxx",
    "name": "张三",
    "channel": "WEB"
  }
}
```

---

## WebSocket 连接

### 端点
```
ws://127.0.0.1:8080/ws/chat?token={customer-token}
```

### 认证方式
- 客户 Token: `cust_` 前缀
- 坐席 Token: 普通格式

### 消息格式

**客户端发送**:
```json
{
  "conversationId": "session-uuid",  // 可选
  "senderId": "customer-id",
  "content": "消息内容",
  "metadata": {}
}
```

**服务端响应**:
```json
{
  "channel": "WEB",
  "conversationId": "session-uuid",
  "senderId": "agent-id",
  "content": "客服回复",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## 数据库表结构

### customers 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | CHAR(36) | 主键 UUID |
| name | VARCHAR(255) | 客户姓名 |
| primary_channel | VARCHAR(50) | 主要渠道 |
| email | VARCHAR(255) | 邮箱（唯一） |
| phone | VARCHAR(50) | 手机号（唯一） |
| wechat_openid | VARCHAR(255) | 微信 OpenID（唯一） |
| whatsapp_id | VARCHAR(255) | WhatsApp ID（唯一） |
| line_id | VARCHAR(255) | Line ID（唯一） |
| telegram_id | VARCHAR(255) | Telegram ID（唯一） |
| facebook_id | VARCHAR(255) | Facebook ID（唯一） |
| avatar_url | VARCHAR(500) | 头像 URL |
| location | VARCHAR(255) | 位置 |
| notes | TEXT | 备注 |
| tags | JSON | 标签数组 |
| custom_fields | JSON | 自定义字段 |
| active | BOOLEAN | 是否活跃 |
| last_interaction_at | TIMESTAMP | 最后交互时间 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

---

## 错误码

| HTTP 状态码 | 说明 |
|------------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 204 | 删除成功（无内容） |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 冲突（如邮箱已存在） |
| 500 | 服务器错误 |

---

## 使用流程

### 客户端接入流程

```
1. 调用 POST /api/v1/public/customer-token 获取 Token
   ↓
2. 使用 Token 连接 WebSocket
   ↓
3. 发送和接收消息
```

### 坐席管理客户流程

```
1. 坐席登录获取 Agent Token
   ↓
2. 使用 Agent Token 调用客户管理 API
   ↓
3. 创建/查询/更新客户信息
   ↓
4. 为客户生成 Token（如需）
```

---

## 注意事项

1. **Token 安全**: 客户 Token 以 `cust_` 开头，坐席 Token 为普通格式
2. **唯一性约束**: email, phone, wechatOpenId 等字段具有唯一性约束
3. **自动创建**: 通过 `/public/customer-token` 接口会自动创建不存在的客户
4. **去重机制**: 相同 channelId 不会重复创建客户
5. **标签查询**: 支持 JSON 数组字段的模糊查询
6. **活跃状态**: 可通过 active 字段标记客户是否活跃

---

**文档版本**: v1.0  
**最后更新**: 2024-01-15
