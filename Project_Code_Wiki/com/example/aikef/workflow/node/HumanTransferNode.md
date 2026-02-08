# HumanTransferNode

## 1. 类档案 (Class Profile)
- **功能定义**：人工转接节点。将当前 AI 接管的会话状态切换为人工处理模式，并通知相关方。
- **注解与配置**：
  - `@LiteflowComponent("human_transfer")`: 注册为 LiteFlow 组件。
- **继承/实现**：继承自 `BaseWorkflowNode`。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `process` | - | 1. 设置上下文标记 `NeedHumanTransfer = true`。<br>2. 更新数据库 `ChatSession` 状态为 `HUMAN_HANDLING`。<br>3. **广播事件**：构建 `sessionUpdated` 事件，通过 WebSocket 推送给前端（客户和客服）。<br>4. 设置最终回复文案（如 "正在为您转接..."）。 | 涉及数据库事务 (`@Transactional`) 和实时消息推送。 |

## 3. 依赖全景 (Dependency Graph)
- **`WebSocketSessionManager`**: 推送实时状态变更。
- **`ChatSessionRepository`**: 更新会话状态。
- **`SessionMessageGateway`**: 发送转接提示消息。

## 4. 调用指南 (Usage Guide)
通常作为意图识别为 "转人工" 后的执行节点。
