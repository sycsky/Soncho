# 标签和备注重构文档

## 重构概述

本次重构主要完成以下目标：
1. 将标签（tags）从 `Customer` 表迁移到 `UserProfile` 表（users表）
2. 区分 AI 生成的标签和手动添加的标签，分别存储在 `user_ai_tags` 和 `user_tags` 表
3. 确保 bootstrap 接口返回的 session 包含用户的 notes 字段
4. 重构标签相关的 API，支持分别操作 AI 标签和手动标签

## 数据库更改

### 1. 数据库迁移脚本
文件：`db/refactor_tags_to_user_profile.sql`

**说明**：
- `user_tags` 表：存储手动添加的标签（客服添加）
- `user_ai_tags` 表：存储 AI 生成的标签
- 移除 `customers` 表的 `tags` 字段

**注意事项**：
- `user_tags` 和 `user_ai_tags` 表由 JPA 自动创建（已在 `UserProfile` 实体中定义）
- 执行迁移脚本前，需要确保已将现有 customer.tags 数据迁移到相应的用户标签表

## 模型层更改

### 1. Customer 模型
文件：`src/main/java/com/example/aikef/model/Customer.java`

**更改**：
- ❌ 移除 `tags` 字段
- ✅ 保留 `notes` 字段（用于客户备注）

### 2. UserProfile 模型
文件：`src/main/java/com/example/aikef/model/UserProfile.java`

**现有字段**（保持不变）：
- `tags`：手动添加的标签（映射到 `user_tags` 表）
- `aiTags`：AI 生成的标签（映射到 `user_ai_tags` 表）
- `notes`：用户备注

## DTO 层更改

### 1. CustomerDto
文件：`src/main/java/com/example/aikef/dto/CustomerDto.java`

**更改**：
- ❌ 移除 `tags` 字段
- ✅ 保留 `notes` 字段

### 3. CreateCustomerRequest
文件：`src/main/java/com/example/aikef/dto/request/CreateCustomerRequest.java`

**更改**：
- ❌ 移除 `tags` 字段

### 4. UpdateCustomerRequest
文件：`src/main/java/com/example/aikef/dto/request/UpdateCustomerRequest.java`

**更改**：
- ❌ 移除 `tags` 字段

## Service 层更改

### 1. CustomerTagService（重构为用户标签服务）
文件：`src/main/java/com/example/aikef/service/CustomerTagService.java`

**重大更改**：
- 从操作 `Customer` 改为操作 `UserProfile`
- 区分手动标签和 AI 标签

**新方法**：
- `addManualTag(UUID userId, String tag)`：添加手动标签
- `removeManualTag(UUID userId, String tag)`：删除手动标签
- `addAiTag(UUID userId, String tag)`：添加 AI 标签
- `removeAiTag(UUID userId, String tag)`：删除 AI 标签
- `getManualTags(UUID userId)`：获取手动标签
- `getAiTags(UUID userId)`：获取 AI 标签
- `getAllTags(UUID userId)`：获取所有标签（返回 UserProfileDto）
- `setManualTags(UUID userId, List<String> tags)`：批量设置手动标签
- `setAiTags(UUID userId, List<String> tags)`：批量设置 AI 标签

### 2. CustomerService
文件：`src/main/java/com/example/aikef/service/CustomerService.java`

**更改**：
- ❌ 移除创建和更新客户时对 `tags` 字段的处理

### 3. EntityMapper
文件：`src/main/java/com/example/aikef/mapper/EntityMapper.java`

**重要更改**：

#### 新增方法：`toCustomerDtoFromSession(ChatSession session)`
- 用于从 `ChatSession` 转换为 `CustomerDto`
- 从 `Customer` 获取客户基本信息

#### 修改方法：`toChatSessionDtoForAgent(ChatSession session, UUID agentId)`
- 改为调用 `toCustomerDtoFromSession(session)` 而不是 `toSimpleCustomerDto(customer)`
- **新增**：从 `session.getNote()` 获取会话备注并填充到 `ChatSessionDto.note` 字段

#### 修改方法：`toCustomerDto(Customer customer)`
- ❌ 移除对 `customer.getTags()` 的处理

#### 标记废弃：`toSimpleCustomerDto(Customer customer)`
- 标记为 `@Deprecated`
- 建议使用 `toCustomerDtoFromSession` 代替

## Controller 层更改

### 1. CustomerTagController（重构为用户标签控制器）
文件：`src/main/java/com/example/aikef/controller/CustomerTagController.java`

**路径更改**：
- 旧路径：`/api/v1/customers/{customerId}/tags`
- 新路径：`/api/v1/users/{userId}/tags`

**API 端点更改**：

| 方法 | 路径 | 说明 | 返回类型 |
|-----|------|-----|---------|
| GET | `/api/v1/users/{userId}/tags` | 获取所有标签（手动+AI） | UserProfileDto |
| GET | `/api/v1/users/{userId}/tags/manual` | 获取手动标签 | List<String> |
| GET | `/api/v1/users/{userId}/tags/ai` | 获取 AI 标签 | List<String> |
| POST | `/api/v1/users/{userId}/tags/manual` | 添加手动标签 | UserProfileDto |
| DELETE | `/api/v1/users/{userId}/tags/manual` | 删除手动标签 | UserProfileDto |
| POST | `/api/v1/users/{userId}/tags/ai` | 添加 AI 标签 | UserProfileDto |
| DELETE | `/api/v1/users/{userId}/tags/ai` | 删除 AI 标签 | UserProfileDto |
| PUT | `/api/v1/users/{userId}/tags/manual` | 批量设置手动标签 | UserProfileDto |
| PUT | `/api/v1/users/{userId}/tags/ai` | 批量设置 AI 标签 | UserProfileDto |

## Bootstrap 接口更改

### BootstrapService
文件：`src/main/java/com/example/aikef/service/BootstrapService.java`

**更改**：
- 修改构造 `ChatSessionDto` 时添加 `session.note()` 参数
- 通过 `EntityMapper.toChatSessionDtoForAgent()` 自动返回包含 note 的 `ChatSessionDto`

**工作流程**：
1. `BootstrapService.bootstrap()` 调用 `entityMapper.toChatSessionDtoForAgent(session, agentId)`
2. `toChatSessionDtoForAgent()` 从 `session.getNote()` 获取会话备注
3. 返回的 `ChatSessionDto` 中包含 `note` 字段
4. `BootstrapService` 在更新未读数时保留 `note` 字段

## 数据流说明

### 1. 会话备注（Notes）的数据流
```
ChatSession.note (String)
    ↓
EntityMapper.toChatSessionDtoForAgent()
    ↓
ChatSessionDto.note
    ↓
Bootstrap 响应
```

**注意**：`note` 字段存储在 `chat_sessions` 表中，而不是 `users` 或 `customers` 表中。每个会话有独立的备注。

### 2. 用户标签的数据流

#### 手动标签（Manual Tags）
```
UserProfile.tags (List<String>)
    ↓ (存储在 user_tags 表)
CustomerTagService.addManualTag()
    ↓
UserProfileDto.tags
    ↓
API 响应
```

#### AI 标签（AI Tags）
```
UserProfile.aiTags (List<String>)
    ↓ (存储在 user_ai_tags 表)
CustomerTagService.addAiTag()
    ↓
UserProfileDto.aiTags
    ↓
API 响应
```

## 特殊处理说明

### 1. 会话备注的存储位置
- `note` 字段存储在 `ChatSession` 实体中（对应 `chat_sessions` 表的 `note` 列）
- 每个会话有独立的备注，不同客服对同一客户的会话可以有不同的备注
- 备注通过 `ChatSessionDto.note` 字段返回给前端

### 2. ChatSession、UserProfile 和 Customer 的关系
- `ChatSession` 同时关联 `UserProfile`（user 字段）和 `Customer`（customer 字段）
- `UserProfile` 存储用户的个人信息和标签（tags、aiTags）
- `Customer` 存储客户的联系信息和元数据
- `ChatSession` 存储会话级别的信息，包括会话备注（note 字段）

## API 使用示例

### 1. 添加手动标签
```http
POST /api/v1/users/{userId}/tags/manual
Content-Type: application/json

{
  "tag": "VIP客户"
}
```

### 2. 添加 AI 标签
```http
POST /api/v1/users/{userId}/tags/ai
Content-Type: application/json

{
  "tag": "潜在流失"
}
```

### 3. 获取所有标签
```http
GET /api/v1/users/{userId}/tags
```

响应示例：
```json
{
  "id": "...",
  "name": "张三",
  "tags": ["VIP客户", "企业用户"],
  "aiTags": ["潜在流失", "高价值"]
}
```

## 迁移注意事项

1. **数据迁移**：
   - 需要将现有 `customers.tags` 数据迁移到 `user_tags` 表
   - 确定迁移策略（全部作为手动标签还是部分作为 AI 标签）

2. **API 兼容性**：
   - 旧的标签 API 路径已更改（从 `/customers/{customerId}/tags` 到 `/users/{userId}/tags`）
   - 客户端需要更新 API 调用

3. **前端适配**：
   - 前端需要适配新的 API 路径和响应格式
   - 需要区分显示手动标签和 AI 标签

4. **测试建议**：
   - 测试 bootstrap 接口返回的 notes 字段
   - 测试手动标签和 AI 标签的添加、删除、查询
   - 测试没有 notes 的用户的回退逻辑

## 文件清单

### 新增文件
- `db/refactor_tags_to_user_profile.sql` - 数据库迁移脚本

### 修改的文件
- `src/main/java/com/example/aikef/model/Customer.java`
- `src/main/java/com/example/aikef/dto/CustomerDto.java`
- `src/main/java/com/example/aikef/dto/ChatSessionDto.java`
- `src/main/java/com/example/aikef/dto/request/CreateCustomerRequest.java`
- `src/main/java/com/example/aikef/dto/request/UpdateCustomerRequest.java`
- `src/main/java/com/example/aikef/service/CustomerTagService.java`
- `src/main/java/com/example/aikef/service/CustomerService.java`
- `src/main/java/com/example/aikef/service/BootstrapService.java`
- `src/main/java/com/example/aikef/service/ChatSessionService.java`
- `src/main/java/com/example/aikef/mapper/EntityMapper.java`
- `src/main/java/com/example/aikef/controller/CustomerTagController.java`

### 未修改但相关的文件
- `src/main/java/com/example/aikef/model/UserProfile.java`（已有 tags 和 aiTags 字段）
- `src/main/java/com/example/aikef/model/ChatSession.java`（已有 note 字段）
