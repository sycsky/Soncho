# ProceduralMemoryRepository

## 1. 类档案 (Class Profile)
- **功能定义**：程序性记忆 Mongo 仓储，提供按客户、场景、状态与可复用标记检索长期经验能力。
- **注解与配置**：
  - `@Repository`
  - 继承 `MongoRepository<ProceduralMemory, String>`
- **继承/实现**：MongoRepository 接口扩展。

## 2. 核心逻辑详解 (Logic Deep Dive)
1. 按 `customerId + scenarioKey + archived=false` 查找最近同场景记忆。
2. 按 `customerId + status + archived=false` 查找最近活动记忆。
3. 按 `customerId + reusable=true + archived=false` 查找最近可复用经验。
4. 按 `customerId + archived=false` 查找最近一条记忆。
5. 按 `customerId` 查最近 20 条记忆供相似任务匹配。

## 3. 依赖全景 (Dependency Graph)
- **`ProceduralMemoryTools`**：核心写入与读取方。
- **`AdvancedAgentNode`**：上下文注入时读取活动/最近草稿。

## 4. 调用指南 (Usage Guide)
```java
Optional<ProceduralMemory> active = repository
        .findTopByCustomerIdAndStatusAndArchivedFalseOrderByLastUpdatedDesc(customerId, "ACTIVE");
```

## 5. 架构师备注 (Architect's Notes)
- 使用方法名派生查询，保持与现有 `repository.mongo` 风格一致。
- 检索策略支持“活动草稿 + 已完成可复用经验”双通道召回。
