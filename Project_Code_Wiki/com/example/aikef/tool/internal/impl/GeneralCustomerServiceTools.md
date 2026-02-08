# GeneralCustomerServiceTools

## 1. 类档案 (Class Profile)
- **功能定义**：通用客服工具集，主要提供转人工服务的功能。
- **注解与配置**：
  - `@Component`: 注册为 Spring 组件。
  - `@Tool`: 暴露给 AI 使用。
- **继承/实现**：无。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `transferToCustomerService` | In: `reason`, `sessionId`<br>Out: `String` | 1. 查找会话。<br>2. 更新状态为 `HUMAN_HANDLING`。<br>3. **广播通知**：通过 WebSocket 通知前端会话状态已变更。 | `@Transactional` 保证状态更新和通知的一致性（尽力而为）。 |

## 3. 依赖全景 (Dependency Graph)
- **`ChatSessionRepository`**: 更新数据库。
- **`WebSocketSessionManager`**: 实时推送。

## 4. 调用指南 (Usage Guide)
**AI 调用示例**：
`transferToCustomerService(reason="User requested human agent", sessionId="...")`
**Prompt 提示**：工具描述中强调 "must be asked for confirmation"，防止 AI 随意转接。
