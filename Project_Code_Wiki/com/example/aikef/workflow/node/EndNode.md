# EndNode

## 1. 类档案 (Class Profile)
- **功能定义**：工作流的结束节点，负责整理最终输出结果。
- **注解与配置**：
  - `@LiteflowComponent("end")`: 注册为 LiteFlow 组件，ID 为 "end"。
- **继承/实现**：继承自 `BaseWorkflowNode`。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `process` | - | 1. 检查上下文中的 `FinalReply` 是否为空。<br>2. 如果为空，将 `LastOutput`（最后一个节点的输出）提升为最终回复。<br>3. 记录执行结束日志。 | 兜底逻辑：确保工作流总是有输出返回给用户。 |

## 3. 依赖全景 (Dependency Graph)
- **`WorkflowContext`**: 读取和更新最终回复状态。

## 4. 调用指南 (Usage Guide)
在 LiteFlow EL 表达式中作为链的最后一个节点使用：
```java
THEN(start, ..., end)
```
