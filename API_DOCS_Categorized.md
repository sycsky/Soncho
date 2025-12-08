# AI 客服后端 API 接入文档 (V2)

本文档旨在为前端开发者提供接入 AI 客服后端服务的详细指南。

## 目录
1.  [全局约定](#全局约定)
    -   [统一响应格式](#统一响应格式)
    -   [认证方式](#认证方式)
2.  [一、认证 (Authentication)](#一认证-authentication)
    -   [1.1 登录](#11-登录)
    -   [1.2 获取当前登录用户信息](#12-获取当前登录用户信息)
3.  [二、核心数据 (Core Data)](#二核心数据-core-data)
    -   [2.1 获取应用初始化数据](#21-获取应用初始化数据)
    -   [2.2 建立 WebSocket 连接](#22-建立-websocket-连接)
4.  [三、会话与消息 (Session & Message)](#三会话与消息-session--message)
    -   [3.1 发送消息](#31-发送消息)
    -   [3.2 更新会话状态](#32-更新会话状态)
5.  [四、AI 服务 (AI Services)](#四ai-服务-ai-services)
    -   [4.1 对话总结](#41-对话总结)
    -   [4.2 文本改写](#42-文本改写)
    -   [4.3 建议标签](#43-建议标签)
6.  [五、管理后台 (Admin)](#五管理后台-admin)
    -   [5.1 获取坐席列表](#51-获取坐席列表)
    -   [5.2 创建新坐席](#52-创建新坐席)
    -   [5.3 获取角色列表](#53-获取角色列表)
    -   [5.4 更新角色信息](#54-更新角色信息)
7.  [六、外部渠道 (External Channels)](#六外部渠道-external-channels)
    -   [6.1 接收渠道消息](#61-接收渠道消息)

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
      "code": 4xx / 5xx, // HTTP 状态码
      "message": "具体的错误信息",
      "data": null
    }
    ```

### 认证方式
除了登录接口 (`/api/v1/auth/login`)，所有其他 API 都需要通过 HTTP Header 提供认证 Token。

-   **Header**: `Authorization`
-   **Value**: `Bearer <your-token>`

---

## 一、认证 (Authentication)

### 1.1 登录
使用坐席的邮箱和密码获取认证 `token`。

-   **Endpoint**: `POST /api/v1/auth/login`
-   **请求体**:
    ```json
    {
      "email": "admin@nexus.com",
      "password": "Admin@123"
    }
    ```
-   **成功响应 (`data` 字段)**:
    ```json
    {
      "token": "a-long-random-jwt-string",
      "agent": {
        "id": "uuid-string",
        "name": "Nexus Admin",
        "email": "admin@nexus.com",
        "status": "ONLINE",
        "roleName": "Administrator"
      }
    }
    ```

### 1.2 获取当前登录用户信息
获取当前 `token` 对应的坐席信息。

-   **Endpoint**: `GET /api/v1/auth/me`
-   **成功响应 (`data` 字段)**:
    ```json
    {
      "id": "uuid-string",
      "name": "Nexus Admin",
      "email": "admin@nexus.com",
      "status": "ONLINE",
      "roleName": "Administrator"
    }
    ```
---

## 二、核心数据 (Core Data)

### 2.1 获取应用初始化数据
登录成功后，调用此接口一次性加载工作台所需的所有基础数据。

-   **Endpoint**: `GET /api/v1/bootstrap`
-   **成功响应 (`data` 字段)**:
    ```json
    {
      "sessions": [ /* 会话列表 */ ],
      "agents": [ /* 坐席列表 */ ],
      "groups": [ /* 聊天分组 */ ],
      "roles": [ /* 角色定义 */ ],
      "quickReplies": [ /* 快捷回复 */ ],
      "knowledgeBase": [ /* 知识库条目 */ ]
    }
    ```

### 2.2 建立 WebSocket 连接
为了接收实时事件（如新消息、状态变更等），前端需要建立 WebSocket 连接。

-   **Endpoint**: `/ws/chat`
-   **协议**: `SockJS`
-   **URL 示例**: `http://localhost:8080/ws/chat`
-   **注意**: 此部分不受 `Result<T>` 格式影响。

---

## 三、会话与消息 (Session & Message)

### 3.1 发送消息
坐席在某个会话中发送消息。

-   **Endpoint**: `/api/v1/conversations/{sessionId}/messages` (暂未实现, 此处为示例)
-   **请求体**:
    ```json
    {
        "text": "您好，请问有什么可以帮您？",
        "attachments": [ /* 附件信息 */ ]
    }
    ```

### 3.2 更新会话状态
更改会话的状态，如“已解决”、“挂起”等。

-   **Endpoint**: `/api/v1/conversations/{sessionId}/status` (暂未实现, 此处为示例)
-   **请求体**:
    ```json
    {
        "status": "RESOLVED"
    }
    ```

---

## 四、AI 服务 (AI Services)

### 4.1 对话总结
对指定会话的所有消息进行总结。

-   **Endpoint**: `POST /api/v1/ai/summary`
-   **请求体**:
    ```json
    {
      "sessionId": "uuid-string"
    }
    ```
-   **成功响应 (`data` 字段)**:
    ```json
    {
        "summary": "本次对话主要讨论了..."
    }
    ```

### 4.2 文本改写
将坐席输入的文本改写得更专业、更礼貌。

-   **Endpoint**: `POST /api/v1/ai/rewrite`
-   **请求体**:
    ```json
    {
      "text": "知道了，稍等"
    }
    ```
-   **成功响应 (`data` 字段)**:
    ```json
    {
        "rewrittenText": "好的，已收到您的请求，请您稍候片刻。"
    }
    ```

### 4.3 建议标签
根据会话内容，为会话建议合适的标签。

-   **Endpoint**: `POST /api/v1/ai/suggest-tags`
-   **请求体**:
    ```json
    {
      "sessionId": "uuid-string"
    }
    ```
-   **成功响应 (`data` 字段)**:
    ```json
    {
        "tags": ["退款咨询", "物流问题"]
    }
    ```

---

## 五、管理后台 (Admin)

### 5.1 获取坐席列表
获取系统内所有坐席的信息。

-   **Endpoint**: `GET /api/v1/admin/agents`
-   **成功响应 (`data` 字段)**: `List<AgentDto>`

### 5.2 创建新坐席
创建一个新的坐席账号。

-   **Endpoint**: `POST /api/v1/admin/agents`
-   **请求体**:
    ```json
    {
      "name": "New Agent",
      "email": "agent@example.com",
      "password": "Password@123",
      "roleId": "uuid-of-role"
    }
    ```
-   **成功响应 (`data` 字段)**: `AgentDto`

### 5.3 获取角色列表
获取所有可用的角色及其权限定义。

-   **Endpoint**: `GET /api/v1/admin/roles`
-   **成功响应 (`data` 字段)**: `List<RoleDto>`

### 5.4 更新角色信息
更新指定角色的信息（如权限）。

-   **Endpoint**: `PUT /api/v1/admin/roles/{id}`
-   **请求体**:
    ```json
    {
      "name": "Supervisor",
      "permissions": ["VIEW_REPORTS", "MANAGE_AGENTS"]
    }
    ```
-   **成功响应 (`data` 字段)**: `RoleDto`

---

## 六、外部渠道 (External Channels)
用于接收来自外部渠道（如 Email、WhatsApp）的消息，通常由第三方服务调用，前端无需关心。

### 6.1 接收渠道消息

-   **Endpoint**: `POST /api/v1/channels/{channel}/messages`
    - `{channel}`可以是 `email` 或 `whatsapp`。
-   **请求体**: `ChannelInboundRequest`
-   **说明**: 此接口会将外部消息转换为内部会话，并通过 WebSocket 推送给前端。
