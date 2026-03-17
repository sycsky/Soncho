# CustomerCoreMemory

## 1. 类档案 (Class Profile)
- **功能定义**：客户长期记忆 Mongo 实体，承载结构化画像、长期记忆列表与摘要文本。
- **注解与配置**：
  - `@Document(collection = "customer_core_memory")`
  - `@Indexed(unique = true)` 作用于 `customerId`
- **继承/实现**：普通数据实体，无继承。

## 2. 核心逻辑详解 (Logic Deep Dive)
1. `customerId` 唯一标识一个客户的长期记忆集合。
2. `memories` 存储记忆条目数组，支持“用户事实 + 偏好 + 对 Agent 行为约束”。
3. `summary` 存储可注入 System Prompt 的摘要文本。
4. `profile` 仍保留以兼容既有画像结构。
5. `createdAt` 与 `lastUpdated` 跟踪生命周期。

## 3. 依赖全景 (Dependency Graph)
- **`CustomerCoreMemoryRepository`**：该实体的 Mongo 读写入口。
- **`CoreMemoryTools`**：实时维护 `memories` 与 `summary`。
- **`AdvancedAgentNode`**：读取 `summary` 注入长期记忆上下文。

## 4. 调用指南 (Usage Guide)
```java
CustomerCoreMemory memory = new CustomerCoreMemory();
memory.setCustomerId(customerId);
memory.setMemories(List.of("我叫Tom", "不要频繁提醒我工作进度"));
memory.setSummary("用户叫Tom，希望减少工作提醒频率。");
repository.save(memory);
```

## 5. 架构师备注 (Architect's Notes)
- `memories` 是当前长期记忆主数据结构，适配自然语言记忆管理场景。
- `summary` 是推理注入层，建议与 `memories` 同步更新，避免语义漂移。
