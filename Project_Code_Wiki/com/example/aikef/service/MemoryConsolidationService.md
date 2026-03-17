# MemoryConsolidationService

## 1. 类档案 (Class Profile)
- **功能定义**：保留历史的记忆固化服务入口，当前事件监听不再执行周期性固化。
- **注解与配置**：
  - `@Service`：Spring 服务组件。
  - `@EventListener` + `@Async`：保留消息事件监听入口。
- **继承/实现**：无继承。

## 2. 核心逻辑详解 (Logic Deep Dive)
1. `onMessageSent` 监听 `MessageSentEvent`，当前直接返回，不再按消息阈值触发固化。
2. `consolidateMemory` 方法仍保留，可用于手动或后续策略调用。
3. 该服务的职责从“主路径实时更新”调整为“兼容保留与可选补充路径”。

## 3. 依赖全景 (Dependency Graph)
- **`CustomerCoreMemoryRepository`**：历史固化路径的持久化依赖。
- **`LangChainChatService`**：历史固化路径的摘要与画像生成依赖。
- **`MessageRepository`**：读取会话消息用于构造固化输入。

## 4. 调用指南 (Usage Guide)
```java
memoryConsolidationService.consolidateMemory(
        sessionId,
        customerId.toString()
);
```

## 5. 架构师备注 (Architect's Notes)
- 主路径记忆更新已切换到 `CoreMemoryTools.manageLongTermMemory` 的实时模式。
- 该类保留是为了兼容历史能力与后续可配置策略切换，避免一次性删除造成回归风险。
