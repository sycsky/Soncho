# StartNode

## 1. 类档案 (Class Profile)
- **功能定义**：工作流的起始节点，负责初始化执行环境，特别是注入多租户上下文信息。
- **注解与配置**：
  - `@LiteflowComponent("start")`: 注册为 LiteFlow 组件，ID 为 "start"。
- **继承/实现**：继承自 `BaseWorkflowNode`。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `process` | - | 1. 获取 `WorkflowContext` 中的 sessionId。<br>2. 查询数据库获取 `ChatSession`。<br>3. 提取 session 中的 `tenantId` 并设置到 `TenantContext`。<br>4. 记录节点执行日志。 | **租户隔离关键点**：确保后续节点操作数据库时能正确过滤租户数据。捕获所有异常以防阻断流程。 |

## 3. 依赖全景 (Dependency Graph)
- **`ChatSessionRepository`**: 用于查询会话信息以获取 tenantId。
- **`TenantContext`**: 线程本地变量 (ThreadLocal)，存储当前租户 ID。

## 4. 调用指南 (Usage Guide)
在 LiteFlow EL 表达式中作为链的第一个节点使用：
```java
THEN(start, node1, node2, end)
```
