# IntentNode

## 1. 类档案 (Class Profile)
- **功能定义**：意图识别节点（Switch 类型）。分析用户输入，识别其意图，并路由到相应的分支。
- **注解与配置**：
  - `@LiteflowComponent("intent")`: 注册为 LiteFlow 组件。
- **继承/实现**：继承自 `NodeSwitchComponent`。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `processSwitch` | Out: `String` (Tag) | 1. 获取意图配置列表。<br>2. 加载历史消息（可选）。<br>3. **识别策略**：<br>   - `keyword`: 关键词匹配。<br>   - `llm`: 调用 LLM 进行语义分类。<br>4. 返回匹配意图的 ID (`tag:intentId`)。 | 支持混合策略：LLM 识别失败可回退到关键词匹配。 |
| `recognizeByLlm` | - | 构造专门的 Classification Prompt，让 LLM 从给定选项中选择。 | 包含模糊匹配逻辑，提高容错率。 |
| `recognizeByKeyword` | - | 简单的包含/分词匹配逻辑。 | |

## 3. 依赖全景 (Dependency Graph)
- **`LangChainChatService`**: LLM 识别支持。
- **`HistoryMessageLoader`**: 获取上下文以提升识别准确率。

## 4. 调用指南 (Usage Guide)
**LiteFlow EL**:
```java
SWITCH(intent_node).TO(refund_flow, complaint_flow, default_flow)
```
用于工作流的入口分流。
