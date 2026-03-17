# AdvancedAgentNode

## 1. 类档案 (Class Profile)
- **功能定义**：高级 Agent 节点，负责动态拼装上下文、工具集并执行多轮推理与工具调用。
- **注解与配置**：
  - `@LiteflowComponent("advancedAgent")`：注册为 LiteFlow 节点。
- **继承/实现**：继承 `BaseWorkflowNode`。

## 2. 核心逻辑详解 (Logic Deep Dive)
1. 读取模型、温度、最大迭代次数。
2. 主线程先读取节点配置（如 `systemPrompt`、`modelId`、`toolAgentModelId`、`tools`），再将配置作为参数传入异步任务。
3. 并发构建上下文与工具集合：`buildDynamicContext` 与 `assembleTools` 同时执行以降低首轮延迟。
4. 异步任务内部不再访问 `NodeComponent` 上下文方法（如 `getNodeConfig/getConfigString`），避免跨线程 `slot` 丢失导致空指针。
5. 上下文注入长期记忆与程序性记忆（`# LONG_TERM_MEMORY`、`# PROCEDURAL_MEMORY`）。
6. 程序性记忆注入会按当前用户 query 在最近经验中匹配最相关任务，而非只取最近会话草稿。
7. 使用 Tool Agent 从候选工具中筛选本轮最相关工具。
8. 进入自主循环：LLM 决策 -> 调用工具 -> 回填结果 -> 继续或输出最终答案。
9. 节点在工具结果与最终回复中检测“完成信号”（如 DONE/APPROVED/完成/就按这个办），自动触发 `manageProceduralMemory` 沉淀经验，不依赖用户显式说“请记住”。
10. 当用户未说完成但明显转移到新话题时，节点会自动把当前活动流程封存为阻塞草稿，避免长期挂在 ACTIVE 状态。
11. 通过系统提示词约束工具调用：上下文缺少必需参数时必须先澄清，不允许模型自行臆造参数。
12. 关键触发点均有日志：工具触发、最终回复触发、话题转移判定与 `manageProceduralMemory` 返回结果，便于链路排查。
13. 长期记忆注入优先使用 `summary`，若摘要为空会回退注入 `memories` 列表，避免长期记忆“存在但未进提示词”。

## 3. 依赖全景 (Dependency Graph)
- **`HistoryMessageLoader`**：加载短期上下文。
- **`ChatMemoryService`**：加载语义相关历史片段。
- **`CustomerCoreMemoryRepository`**：读取长期记忆摘要。
- **`ProceduralMemoryRepository`**：读取程序性记忆草稿并注入上下文。
- **`ToolCallProcessor`**：执行内部/系统工具调用。
- **`LangChainChatService`**：模型推理与结构化输出。

## 4. 调用指南 (Usage Guide)
```java
AdvancedAgentNode node = new AdvancedAgentNode();
node.process();
```

## 5. 架构师备注 (Architect's Notes)
- 该节点承担“长期记忆注入 + 实时记忆更新触发”的关键职责。
- 记忆语义已扩展为“用户事实 + 偏好 + 对 Agent 的行为约束 + 可复用任务经验”。
- 该节点支持跨长周期的相似任务召回，不依赖连续聊天上下文。
