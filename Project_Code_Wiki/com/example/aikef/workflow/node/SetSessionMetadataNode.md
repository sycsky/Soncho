# SetSessionMetadataNode

## 1. 类档案 (Class Profile)
- **功能定义**：会话元数据提取与设置节点。利用 LLM 从对话内容中提取结构化信息（如用户姓名、订单号），并保存到会话的 metadata 中。
- **注解与配置**：
  - `@LiteflowComponent("setSessionMetadata")`: 注册为 LiteFlow 组件。
- **继承/实现**：继承自 `BaseWorkflowNode`。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `process` | - | 1. 获取 `mappings` 配置（字段映射关系）。<br>2. 构建 LLM 结构化输出定义 (`buildFieldDefinitions`)。<br>3. 调用 `langChainChatService.chatWithFieldDefinitions` 进行提取。<br>4. 解析 LLM 返回的 JSON。<br>5. 更新数据库中的 `ChatSession` metadata 字段。 | 使用了 OpenAI 的 Function Calling / Structured Output 能力来保证提取格式的准确性。 |
| `extractDataWithAI` | In: `lastOutput`, `mappings`... | 封装 LangChain4j 的结构化提取调用。 | 设置较低的 temperature (0.3) 以提高稳定性。 |

## 3. 依赖全景 (Dependency Graph)
- **`LangChainChatService`**: 提供结构化提取能力。
- **`ChatSessionRepository`**: 读取和更新会话数据。

## 4. 调用指南 (Usage Guide)
用于 "填槽" (Slot Filling) 场景，将非结构化对话转化为结构化业务数据。
