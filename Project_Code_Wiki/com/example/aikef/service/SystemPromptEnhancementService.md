# SystemPromptEnhancementService

## 1. Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: System Prompt 增强/美化服务。利用 LLM 自身的生成能力，根据用户简单的指令和节点上下文（如工具列表、节点类型），自动生成专业、结构化的高质量 System Prompt。

## 2. Method Deep Dive

### `enhanceSystemPrompt`
- **Signature**: `public String enhanceSystemPrompt(String nodeType, List<UUID> toolIds, String userInput)`
- **Description**: 核心增强方法。
- **Parameters**:
  - `nodeType`: 节点类型（llm, intent, parameter_extraction 等）。
  - `toolIds`: 该节点可用的工具 ID 列表。
  - `userInput`: 用户输入的原始简单指令。
- **Returns**: 优化后的 System Prompt 文本。
- **Logic**:
  1. 构建 Meta-Prompt（元提示词），告诉 LLM 它是一个 "Prompt 优化专家"。
  2. 注入节点类型说明和可用工具的详细描述。
  3. 调用 LLM 生成优化后的 Prompt。
  4. 包含失败回退机制，如果生成失败则返回默认模板。

### `buildToolsInfo`
- **Signature**: `private String buildToolsInfo(List<UUID> toolIds)`
- **Description**: 将工具列表转换为 LLM 可理解的文本描述（名称、用途等）。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `LangChainChatService`: LLM 调用。
  - `AiToolRepository`: 工具信息查询。

## 4. Usage Guide
### 场景：低代码配置
用户在配置 AI 节点时，只需输入简单的指令："做一个热情的客服，能查订单"。
点击 "AI 优化" 按钮，前端调用此服务。
服务返回："你是一名专业的电商客服专员...你的语气热情友好...你可以使用 'OrderSearchTool' 来查询订单信息，使用前请先询问订单号..."
用户确认后，该高质量 Prompt 被应用到节点配置中。
