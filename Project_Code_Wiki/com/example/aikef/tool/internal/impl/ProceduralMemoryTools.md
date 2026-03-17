# ProceduralMemoryTools

## 1. 类档案 (Class Profile)
- **功能定义**：程序性记忆内部工具，按用户自然语言维护任务经验，并把完成任务沉淀为长期可复用 playbook。
- **注解与配置**：
  - `@Component`
  - `@Tool` 暴露 `manageProceduralMemory` 与 `getLatestProceduralMemory`
- **继承/实现**：普通 Spring 组件。

## 2. 核心逻辑详解 (Logic Deep Dive)
1. 解析 `customerId`（入参优先，缺失时从 `WorkflowContext` 回退）。
2. 读取最近记忆集合，调用 LLM 产出结构化决策（CREATE/UPDATE/REPLAN/ABORT/COMPLETE）。
3. 按 `scenarioKey` 命中记忆，不存在则创建新草稿。
4. 将 LLM 返回映射到 `status/memoryKind/reusable/currentStep/slots/steps/summary/triggerPhrases`。
5. 当状态为 `COMPLETED` 时自动提升为可复用 playbook，并累积成功次数。
6. 更新版本号与时间戳后持久化。
7. 支持来自节点侧的自动信号输入（工具状态与对话完成语义），无需用户显式触发记忆指令。
8. 若 LLM 结构化决策解析失败，会启用兜底决策构建最小记忆并落库，避免整条链路静默丢失。
9. 关键处理节点均输出日志：入口参数、LLM/兜底决策来源、目标记录命中情况、最终保存结果。
10. 注意：底层消息构建会跳过空 userMessage，避免触发 LangChain4j 空文本校验异常。

## 3. 依赖全景 (Dependency Graph)
- **`ProceduralMemoryRepository`**：草稿查询与保存。
- **`LangChainChatService`**：把自然语言需求转为结构化步骤决策。
- **`ObjectMapper`**：决策 JSON 解析与字段映射。
- **`WorkflowContext`**：工具调用上下文 customerId 注入。

## 4. 调用指南 (Usage Guide)
```java
String result = proceduralMemoryTools.manageProceduralMemory(
        null,
        "这个流程不要了，换成先验地址再下单",
        workflowContext
);
```

## 5. 架构师备注 (Architect's Notes)
- 该工具支持“边做边记”和“完成后沉淀复用经验”两种阶段。
- 面向长期复用目标，适用于隔周、隔月再次出现的同类任务。
