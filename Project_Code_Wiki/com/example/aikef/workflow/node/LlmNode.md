# LlmNode

## 1. 类档案 (Class Profile)
- **功能定义**：LLM 调用节点，工作流中最核心的智能节点。负责调用大语言模型，支持普通对话、历史记录回溯以及**工具调用 (Tool Calling)**。
- **注解与配置**：
  - `@LiteflowComponent("llm")`: 注册为 LiteFlow 组件。
- **继承/实现**：继承自 `BaseWorkflowNode`。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `process` | - | 1. 获取节点配置（modelId, prompt, tools 等）。<br>2. 判断是否绑定了工具。<br>3. 分支处理：<br>   - **无工具**：调用 `handleNormalLlmCall`。<br>   - **有工具**：调用 `handleLlmCallWithTools`。<br>4. 捕获异常并设置错误输出。 | 异常时返回友好提示 "抱歉，我暂时无法处理..." 并记录错误日志。 |
| `handleLlmCallWithTools` | In: `messages`, `toolSpecs`... | 1. 调用 `langChainChatService.chatWithTools`。<br>2. 解析 LLM 响应 (`AiMessage`)。<br>3. **关键分支**：<br>   - 如果包含 `ToolExecutionRequest`：调用 `handleToolExecutionRequests` 进行工具执行。<br>   - 否则：直接输出 LLM 回复。 | 集成了 LangChain4j 的工具调用能力。 |
| `handleToolExecutionRequests` | In: `requests`... | 1. 遍历所有工具请求。<br>2. 验证参数完整性（必填参数检查）。<br>3. 查找工具定义。<br>4. 将工具请求存入上下文状态 (`ToolCallState`)。<br>5. **暂停流程**：抛出 `WorkflowPausedException`，挂起工作流，等待工具执行结果（通常是前端交互或异步执行）。 | **工作流暂停机制**：这里不直接执行工具，而是暂停流程，等待外部系统（如前端 Widget）收集参数或确认后再恢复。 |
| `processNormalLlmCall` | In: `ctx`... | 1. 构建消息历史（System + History + User）。<br>2. 调用 LLM 服务。<br>3. 设置输出。 | 标准 LLM 对话模式。 |

## 3. 依赖全景 (Dependency Graph)

### 核心依赖
- **`LangChainChatService`**: 封装底层 LLM 调用（OpenAI, Azure 等）。
- **`ToolCallProcessor`**: 处理工具定义转换 (`ToolSpecification`) 和参数解析。
- **`WorkflowPauseService`**: 处理工作流的暂停和状态保存。
- **`HistoryMessageLoader`**: 加载数据库中的历史会话消息。

### 关键配置
- **`useHistory`**: 是否携带历史上下文。
- **`enableToolCall`**: 是否启用工具调用能力。

## 4. 调用指南 (Usage Guide)
在工作流设计器中拖拽 "LLM Node" 并配置：
1.  选择模型 (Model)。
2.  编写 System Prompt（支持 `{{sys.query}}` 变量）。
3.  勾选需要使用的工具 (Tools)。

**LiteFlow EL**:
```java
THEN(start, llm, end)
```
