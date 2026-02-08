# MessageService

## 1. 类档案 (Class Profile)
- **功能定义**：消息处理服务。负责消息的存储、查询（区分客/服视图）、DTO 转换以及工作流日志关联。
- **注解与配置**：
  - `@Service`: 标记为 Spring 服务。
- **继承/实现**：无。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `getSessionMessages` | In: `sessionId`, `userPrincipal`...<br>Out: `Page<ChatMessageDto>` | 1. 鉴权：检查调用者是否为会话成员。<br>2. **视图过滤**：<br>   - **客户**：过滤掉 `isInternal=true` 的内部消息，排除 `agentMetadata`。<br>   - **客服**：可见所有消息，包含元数据。<br>3. 查询数据库并转换为 DTO。 | 实现了严格的数据权限控制和视图隔离。 |
| `toMessageDto` | (Internal) | 将实体转换为 DTO，并注入工作流执行日志 (`workflowExecution`) 到 `agentMetadata` 中（如果是客服视图）。 | 动态关联工作流日志，方便前端调试 AI 执行过程。 |
| `sendMessage` | In: `sessionId`, `text`, `senderType`... | 1. 保存消息实体。<br>2. 更新会话 `lastActiveAt`。<br>3. 如果是客服发送，自动更新其已读时间。 | 联动更新会话活跃度和已读状态。 |

## 3. 依赖全景 (Dependency Graph)
- **`MessageRepository`**: 消息存储。
- **`WorkflowExecutionLogRepository`**: 获取关联的 AI 执行日志。
- **`ReadRecordService`**: 管理已读状态。

## 4. 调用指南 (Usage Guide)
```java
// 客服发送消息
messageService.sendMessage(
    sessionId, 
    "Hello", 
    SenderType.AGENT, 
    agentId, 
    null
);
```
