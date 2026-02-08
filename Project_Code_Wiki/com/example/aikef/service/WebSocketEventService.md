# WebSocketEventService

## 1. Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: WebSocket 事件处理器。作为 WebSocket 连接的后端处理逻辑，负责分发和处理来自前端的各类实时指令（如发送消息、更新状态、输入中状态等），并协调消息的广播和业务流程触发。

## 2. Method Deep Dive

### `handle`
- **Signature**: `public ServerEvent handle(String event, JsonNode payload, AgentPrincipal agentPrincipal, CustomerPrincipal customerPrincipal)`
- **Description**: WebSocket 消息的顶层分发器。根据 `event` 类型调用具体的处理方法。

### `handleSendMessage`
- **Signature**: `private ServerEvent handleSendMessage(...)`
- **Description**: 处理 `sendMessage` 事件。
- **Logic**:
  1. 调用 `conversationService.sendMessage` 保存消息。
  2. 处理 @Mentions 逻辑。
  3. 广播消息给会话参与者。
  4. 如果是客户消息，触发 `triggerAiWorkflowIfNeeded`。
  5. 如果是客服消息，尝试转发到外部平台。

### `triggerAiWorkflowIfNeeded`
- **Signature**: `public void triggerAiWorkflowIfNeeded(UUID sessionId, String userMessage, UUID messageId)`
- **Description**: 触发 AI 接管逻辑。
- **Logic**: 检查会话状态是否为 `AI_HANDLING`，如果是，则将消息提交给 `WorkflowExecutionScheduler` 进行调度执行。

### `broadcastStatusToSession`
- **Signature**: `public void broadcastStatusToSession(...)`
- **Description**: 广播会话状态变更事件（如 AI 开始思考、工作流执行中）。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `ConversationService`: 消息核心业务。
  - `WebSocketSessionManager`: 连接管理。
  - `AiWorkflowService`, `WorkflowExecutionScheduler`: AI 工作流。
  - `ExternalPlatformService`: 外部消息转发。

## 4. Usage Guide
### 场景：实时聊天
前端 Widget 通过 WebSocket 发送 JSON：`{ "event": "sendMessage", "payload": { "text": "Help!" } }`。
`WebSocketEventService` 接收后：
1. 保存消息。
2. 立即向客服端 WebSocket 推送新消息事件。
3. 发现当前是 AI 接管模式，触发 AI 工作流。
4. 返回 `ServerEvent` 确认消息已接收。
