# AI 客服后端 API 接入文档 (V2)

## 统一响应格式

所有 API 接口的响应都遵循统一的 `Result<T>` 格式，无论成功或失败。这使得前端处理数据和错误的方式更加一致。

-   **成功响应结构**:
    ```json
    {
      "code": 200,
      "message": "Success",
      "data": { ... } // 具体的业务数据
    }
    ```
-   **失败响应结构**:
    ```json
    {
      "code": 404, // HTTP 状态码
      "message": "用户不存在", // 错误信息
      "data": null
    }
    ```

---

## 第一步：登录

首先，前端需要调用登录接口，使用坐席的邮箱和密码获取认证 `token`。

-   **接口**: `POST /api/v1/auth/login`
-   **请求体 (Content-Type: application/json)**:
    ```json
    {
      "email": "admin@nexus.com",
      "password": "Admin@123"
    }
    ```
    > 我们已为您预置了管理员账号 `admin@nexus.com`，密码 `Admin@123`。

-   **成功响应 (200 OK)**:
    响应体中的 `data` 字段包含 `token` 和坐席信息。
    ```json
    {
      "code": 200,
      "message": "Success",
      "data": {
        "token": "a-long-random-jwt-string",
        "agent": {
          "id": "22222222-2222-2222-2222-222222222222",
          "name": "Nexus Admin",
          "email": "admin@nexus.com",
          "status": "ONLINE",
          "roleName": "Administrator"
        }
      }
    }
    ```
    -   `token`: 后续所有请求都需要在 HTTP `Authorization` 头中携带此 `token`。

---

## 第二步：设置通用请求头

获取到 `token` 后，请在前端的 HTTP 客户端（如 Axios、Fetch）中设置全局请求头。

-   **Header**: `Authorization`
-   **Value**: `Bearer a-long-random-jwt-string` (注意 `Bearer` 和 `token` 之间的空格)

---

## 第三步：获取应用初始化数据

登录成功后，调用此接口一次性加载坐席工作台所需的所有基础数据。

-   **接口**: `GET /api/v1/bootstrap`
-   **请求头**: `Authorization: Bearer <your-token>`
-   **成功响应 (200 OK)**:
    `data` 字段包含多项应用所需的数据。
    ```json
    {
      "code": 200,
      "message": "Success",
      "data": {
        "sessions": [ /* ... */ ],
        "agents": [ /* ... */ ],
        "groups": [ /* ... */ ],
        "roles": [ /* ... */ ],
        "quickReplies": [ /* ... */ ],
        "knowledgeBase": [ /* ... */ ]
      }
    }
    ```
    前端应将 `data` 中的这些数据存储在状态管理库（如 Redux、Pinia）中。

---

## 第四步：建立 WebSocket 连接

为了接收实时事件（如新消息、会話状态变更等），前端需要建立 WebSocket 连接。**此部分不受统一响应格式影响。**

-   **Endpoint**: `/ws/chat`
-   **协议**: `SockJS`
-   **URL 示例**: `http://localhost:8080/ws/chat`

连接建立后，服务器会通过此通道实时推送事件。

---

## 前端开发流程总结

1.  **显示登录页**，调用 `POST /api/v1/auth/login`。
2.  从响应的 `data.token` 中获取 `token`，存入 `localStorage` 或内存，并设置到全局 HTTP 请求头。
3.  **跳转到主工作台**:
    -   调用 `GET /api/v1/bootstrap` 获取初始化数据，从 `data` 字段中取出应用所需数据并填充页面。
    -   建立 `/ws/chat` 的 WebSocket 连接，开始监听实时事件。
4.  **发起业务请求**:
    -   所有 API 请求都会返回 `Result<T>` 格式的响应。前端只需判断 `code` 是否为 `200`，然后从 `data` 字段获取业务数据。
    -   所有请求都必须携带 `Authorization: Bearer <token>`。
