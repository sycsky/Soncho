# AiToolRepository

## 1. 类档案
- **接口名**: `AiToolRepository`
- **包路径**: `com.example.aikef.tool.repository`
- **核心职责**: 提供 `AiTool` 的增删改查与语义检索回查所需查询能力。
- **本次关键变更**: 新增 `findEnabledByIdsWithSchema(List<UUID> ids)`，支持“向量检索命中 ID 后批量回查工具实体”。

## 2. 核心逻辑详解
- **工具基础查询**:
  1. `findByName`：按工具名精确查。
  2. `findByIdWithSchema` / `findByNameWithSchema`：工具+Schema 联查。
  3. `findEnabledToolsWithSchema`：启用工具全量加载。
- **检索相关查询**:
  1. `searchByKeyword`：关键词检索兜底能力。
  2. `findEnabledByIdsWithSchema`：按 ID 集合回查启用工具并预加载 Schema。
- **向量初始化辅助查询**:
  1. `findByEmbeddingIsNull`：启动时补齐缺失向量。

## 3. 依赖全景
- `JpaRepository<AiTool, UUID>`：基础仓储能力。
- `AiTool`：工具聚合根实体。
- `@Query`：用于 join fetch 场景，避免运行时 N+1 查询。

## 4. 调用指南
```java
List<UUID> ids = List.of(
    UUID.fromString("11111111-1111-1111-1111-111111111111"),
    UUID.fromString("22222222-2222-2222-2222-222222222222")
);
List<AiTool> tools = aiToolRepository.findEnabledByIdsWithSchema(ids);
```

## 5. 架构师备注
- **检索分层**: 向量召回在 PGVector 完成，Repository 负责关系库实体回查，不直接参与向量计算。
- **性能考虑**: 回查接口使用 `LEFT JOIN FETCH`，避免 Agent 组装工具规格时重复访问 Schema。
