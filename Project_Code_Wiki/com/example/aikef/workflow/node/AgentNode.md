# AgentNode

## 1. 类档案 (Class Profile)
- **功能定义**：高级 Agent 节点，实现自主的多步推理和工具使用（ReAct 模式）。
- **注解与配置**：
  - `@LiteflowComponent("agent")`: 注册为 LiteFlow 组件。
- **继承/实现**：继承自 `BaseWorkflowNode`。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `process` | - | 1. **初始化**：加载配置、工具定义，构建 System Prompt。<br>2. **自动注入**：自动注入 `getWorkflowContext` 等基础工具。<br>3. **自主循环 (Loop)**：<br>   - 调用 LLM 获取思考结果。<br>   - **Tool Decision**：如果 LLM 决定调用工具：<br>     - 执行工具 (`executeTool`)。<br>     - 将结果追加到历史。<br>     - 继续下一轮循环。<br>   - **Final Answer**：如果 LLM 输出纯文本，结束循环。<br>4. 设置最终输出。 | 实现了完整的 Agent Loop，包含思考(Thinking)、行动(Action)、观察(Observation)。 |
| `executeTool` | In: `request` | 调用 `ToolCallProcessor` 执行具体工具，并捕获结果或异常。 | |
| `saveToolBatchToDatabase` | - | 将 Agent 的思考过程和工具调用记录批量保存到数据库，用于前端展示 "Thinking..." 过程。 | 特殊的 Message 结构存储 (SenderType.TOOL)。 |

## 3. 依赖全景 (Dependency Graph)
- **`LangChainChatService`**: 提供 `chatWithTools` 能力。
- **`ToolCallProcessor`**: 工具执行引擎。
- **`ContextTools`**: 提供上下文感知的内置工具。

## 4. 调用指南 (Usage Guide)
这是最强大的节点类型，适用于复杂任务。
配置：
- `goal`: 任务目标。
- `tools`: 允许使用的工具列表。
- `maxIterations`: 最大循环次数（防止死循环）。
