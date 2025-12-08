# AI 客服后端 API 接入文档 (V3)

本文档为前端开发者提供接入 AI 客服后端服务的完整指南。所有接口已按功能模块划分。

## 目录
1.  [全局约定](#全局约定)
2.  [一、认证 (Authentication)](#一认证-authentication)
3.  [二、核心数据 (Core Data)](#二核心数据-core-data)
4.  [三、管理后台 (Admin)](#三管理后台-admin)
    -   [3.1 坐席管理](#31-坐席管理)
    -   [3.2 角色管理](#32-角色管理)
5.  [四、AI 服务 (AI Services)](#四ai-服务-ai-services)
6.  [五、其他接口](#五其他接口)

---

## 全局约定

### 统一响应格式
所有 API 接口的响应都遵循统一的 `Result<T>` 格式。

-   **成功响应**:
    ```json
    {
      "code": 200,
      "message": "Success",
      "data": { ... } // 具体的业务数据
    }
    ```
-   **失败响应**:
    ```json
    {
      "code": 4xx / 5xx, 
      "message": "具体的错误信息",
      "data": null
    }
    ```

### 认证方式
除了登录接口，所有其他 API 都需要通过 HTTP Header 提供认证 Token。

-   **Header**: `Authorization`
-   **Value**: `Bearer <your-token>`

---

## 一、认证 (Authentication)

### 1.1 登录
-   **Endpoint**: `POST /api/v1/auth/login`
-   **说明**: 使用坐席邮箱和密码登录，获取 Token。
-   **请求体**:
    ```json
    {
      "email": "admin@nexus.com",
      "password": "Admin@123"
    }
    ```
-   **成功响应 (`data` 字段)**: 包含 `token` 和 `agent` 信息。

### 1.2 获取当前用户信息
-   **Endpoint**: `GET /api/v1/auth/me`
-   **说明**: 获取当前 Token 对应坐席的详细信息。
-   **成功响应 (`data` 字段)**: `AgentDto` 对象。

---

## 二、核心数据 (Core Data)

### 2.1 获取应用初始化数据
-   **Endpoint**: `GET /api/v1/bootstrap`
-   **说明**: 登录后调用，一次性加载工作台所需的所有基础数据。
-   **成功响应 (`data` 字段)**: 包含 `sessions`, `agents`, `roles` 等多个列表。

### 2.2 WebSocket 连接
-   **Endpoint**: `/ws/chat`
-   **说明**: 用于接收新消息、状态变更等实时事件。

---

## 三、管理后台 (Admin)

### 3.1 坐席管理

#### 3.1.1 获取坐席列表 (分页/筛选)
-   **Endpoint**: `GET /api/v1/admin/agents`
-   **说明**: 获取坐席列表，支持分页、按姓名模糊查询和按角色精确查询。
-   **查询参数**:
    -   `page`: 页码 (从 0 开始)
    -   `size`: 每页数量
    -   `sort`: 排序字段, 如 `name,asc` 或 `email,desc`
    -   `name`: 坐席姓名 (可选, 模糊匹配)
    -   `role`: 角色名称 (可选, 精确匹配)
-   **成功响应 (`data` 字段)**: Spring `Page` 对象，包含 `content`, `totalPages` 等信息。

#### 3.1.2 创建新坐席
-   **Endpoint**: `POST /api/v1/admin/agents`
-   **状态码**: `201 Created`
-   **请求体 (`CreateAgentRequest`)**:
    ```json
    {
      "name": "New Agent",
      "email": "agent@example.com",
      "password": "Password@123",
      "roleId": "uuid-of-role"
    }
    ```

#### 3.1.3 修改坐席信息
-   **Endpoint**: `PUT /api/v1/admin/agents/{id}`
-   **说明**: 更新坐席信息，可选择性更新姓名、邮箱、状态和角色。
-   **请求体 (`UpdateAgentRequest`)**: 所有字段均为可选。
    ```json
    {
      "name": "Updated Name",
      "status": "OFFLINE",
      "roleId": "new-role-uuid"
    }
    ```

### 3.2 角色管理

#### 3.2.1 获取角色列表
-   **Endpoint**: `GET /api/v1/admin/roles`
-   **说明**: 获取所有可用角色。

#### 3.2.2 创建新角色
-   **Endpoint**: `POST /api/v1/admin/roles`
-   **状态码**: `201 Created`
-   **请求体 (`CreateRoleRequest`)**:
    ```json
    {
      "name": "Supervisor",
      "description": "Can view reports.",
      "permissions": ["VIEW_REPORTS", "EDIT_ARTICLES"]
    }
    ```

#### 3.2.3 修改角色信息
-   **Endpoint**: `PUT /api/v1/admin/roles/{id}`
-   **请求体 (`UpdateRoleRequest`)**:
    ```json
    {
      "name": "Advanced Supervisor",
      "description": "Can also manage agents.",
      "permissions": ["VIEW_REPORTS", "MANAGE_AGENTS"]
    }
    ```

#### 3.2.4 删除角色
-   **Endpoint**: `DELETE /api/v1/admin/roles/{id}`
-   **说明**: 删除一个角色。如果角色仍被坐席使用，可能会失败。
-   **状态码**: `204 No Content`

---

## 四、AI 服务 (AI Services)

### 4.1 对话总结
-   **Endpoint**: `POST /api/v1/ai/summary`
-   **请求体**: `{"sessionId": "uuid-string"}`

### 4.2 文本改写
-   **Endpoint**: `POST /api/v1/ai/rewrite`
-   **请求体**: `{"text": "知道了，稍等"}`

### 4.3 建议标签
-   **Endpoint**: `POST /api/v1/ai/suggest-tags`
-   **请求体**: `{"sessionId": "uuid-string"}`

---

## 五、其他接口

### 5.1 外部渠道消息接收
-   **Endpoint**: `POST /api/v1/channels/{channel}/messages`
-   **说明**: 用于接收外部消息，前端无需关心。
