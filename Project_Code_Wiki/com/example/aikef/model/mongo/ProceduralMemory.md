# ProceduralMemory

## 1. 类档案 (Class Profile)
- **功能定义**：程序性记忆实体，保存“任务执行经验库”，支持从草稿沉淀为可复用 playbook。
- **注解与配置**：
  - `@Document(collection = "procedural_memory")`
  - `@CompoundIndexes`：按 `customerId + scenarioKey` 与 `customerId + status + lastUpdated` 建索引。
- **继承/实现**：普通 Mongo 实体。

## 2. 核心逻辑详解 (Logic Deep Dive)
1. `scenarioKey` 用于归一化同类任务（如周报审批、报销提单）。
2. `status` 记录流程状态（ACTIVE/ABORTED/COMPLETED/BLOCKED）。
3. `memoryKind` 与 `reusable` 区分草稿与可复用 playbook。
4. `triggerPhrases` 记录触发语义，便于后续相似任务召回。
5. `steps` 保存可执行步骤序列，每步包含目标、动作类型、动作内容、执行结果与状态。
6. `currentStep`、`slots`、`successCount`、`completedAt` 支撑跨时段复用与成功经验积累。

## 3. 依赖全景 (Dependency Graph)
- **`ProceduralMemoryRepository`**：提供按客户、场景、状态读取最近草稿能力。
- **`ProceduralMemoryTools`**：按用户自然语言实时创建/更新草稿。
- **`AdvancedAgentNode`**：读取并注入程序性记忆摘要到系统上下文。

## 4. 调用指南 (Usage Guide)
```java
ProceduralMemory memory = new ProceduralMemory();
memory.setCustomerId(customerId);
memory.setScenarioKey("refund_order");
memory.setStatus("ACTIVE");
memory.setCurrentStep(2);
repository.save(memory);
```

## 5. 架构师备注 (Architect's Notes)
- 该实体定位为“长期经验记忆”，核心目标是相似任务再次出现时直接复用步骤。
- 该实体不替代 `WorkflowPausedState` 的暂停恢复职责。
