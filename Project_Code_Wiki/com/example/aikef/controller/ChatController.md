# ChatController

## 1. 类档案 (Class Profile)
- **功能定义**：聊天会话管理 API，提供会话详情查询、历史消息获取、支持客服分配与会话转移功能。
- **注解与配置**：
  - `@RestController`: REST API 控制器。
  - `@RequestMapping("/api/v1/chat")`: 基础路径。
- **继承/实现**：无。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `getSession` | In: `sessionId`, `auth`<br>Out: `ChatSessionDto` | 1. 获取当前客服 ID。<br>2. 调用 Service 获取会话详情。 | 强制要求客服身份认证。 |
| `getSessionMessages` | In: `sessionId`, `pageable`, `auth`<br>Out: `Page<ChatMessageDto>` | 1. 识别调用者身份（客服或客户）。<br>2. 调用 Service 获取消息（根据身份自动过滤）。 | 支持双视角（客/服）复用同一接口。 |
| `assignSupportAgent` | In: `sessionId`, `agentId`<br>Out: `void` | 为会话添加协作客服。 | |
| `getTransferableAgents` | In: `sessionId`<br>Out: `List<AgentDto>` | 获取可接收转接的客服列表（排除当前负责人）。 | |

## 3. 依赖全景 (Dependency Graph)
- **`MessageService`**: 获取消息。
- **`ChatSessionService`**: 会话管理。
- **`SessionSummaryService`**: 摘要服务（注入但未在代码片段中显示使用，可能用于其他方法）。

## 4. 调用指南 (Usage Guide)
```http
GET /api/v1/chat/sessions/{sessionId}/messages
Authorization: Bearer {token}
```
