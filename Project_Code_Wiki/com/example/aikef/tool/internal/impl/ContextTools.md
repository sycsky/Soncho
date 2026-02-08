# ContextTools

## 1. 类档案 (Class Profile)
- **功能定义**：上下文工具类，允许 AI 在运行时检查当前工作流的执行状态和变量。
- **注解与配置**：
  - `@Component`: 注册为 Spring 组件。
  - `@Tool`: LangChain4j 工具注解，暴露给 AI 使用。
- **继承/实现**：无。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `getWorkflowContext` | In: `keys` (String[]), `ctx` (WorkflowContext)<br>Out: `String` (JSON) | 1. 检查上下文是否为空。<br>2. 构建包含所有主要状态的 Map (query, variables, nodeOutputs 等)。<br>3. 如果提供了 `keys`，则只过滤返回指定字段。<br>4. 序列化为 JSON 字符串。 | 使用 `@ToolMemoryId` 注解自动注入 `WorkflowContext`，无需 AI 显式传递。 |

## 3. 依赖全景 (Dependency Graph)
- **`WorkflowContext`**: 获取运行时数据。
- **`ObjectMapper`**: JSON 序列化。

## 4. 调用指南 (Usage Guide)
**AI 调用示例**：
`getWorkflowContext(keys=["customerInfo", "lastOutput"])`
**用途**：用于 Debug 或当 Agent 需要根据前序节点的复杂输出做决策时。
