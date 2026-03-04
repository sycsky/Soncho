# AiToolService

## 1. 类档案
- **类名**: `AiToolService`
- **包路径**: `com.example.aikef.tool.service`
- **核心职责**: 统一管理工具 CRUD、工具执行、工具参数 Schema、工具向量化与语义检索。
- **本次关键变更**:
  - 工具语义检索改为走独立 PGVector 表 `ai_tool_embeddings`。
  - 检索流程变更为“语义检索命中 toolId -> 回查 `ai_tools` 启用工具 -> 按相关度顺序返回”。
  - 增加向量库初始化失败熔断：初始化异常后自动禁用向量路径并回退关键词检索。
  - 增加 PG 数据库存在性校验：若数据库不存在则先自动创建再初始化向量表。
  - 保留关键词检索作为降级兜底。

## 2. 核心逻辑详解
- **工具入库向量化 (`generateAndSaveEmbedding`)**:
  1. 组装工具语义文本（name/displayName/description/tags）。
  2. 调用 `EmbeddingModel` 生成向量。
  3. 向量一份写入 MySQL `ai_tools.embedding`（JSON），用于运维与排障可见性。
  4. 同步 upsert 到 PGVector（元数据包含 `toolId/toolName/tenantId`）。
- **工具语义搜索 (`searchToolsBySemantic`)**:
  1. 将查询词向量化。
  2. 在 PGVector 表检索相似片段，提取 `toolId` 去重并按相似度保序。
  3. 批量回查启用工具并保序输出。
  4. 若向量存储不可用、初始化失败、检索异常或无命中，回退 `searchByKeyword`。
- **向量存储初始化 (`getToolVectorStore`)**:
  1. 受配置 `tool.search.vector-enabled` 控制是否启用。
  2. 首次初始化失败后将 `toolVectorInitFailed` 置位，后续不再重复初始化。
  3. 初始化前执行数据库存在性检查，不存在时自动创建目标数据库。
  4. 支持配置 `tool.search.vector-create-table` 控制是否自动建表。
- **工具删除 (`deleteTool`)**:
  1. 删除执行记录。
  2. 删除 PGVector 中对应 `toolId` 的向量。
  3. 删除 `ai_tools` 记录。

## 3. 依赖全景
- **数据层**:
  - `AiToolRepository`：工具实体查询与批量回查。
  - `ToolExecutionRepository`：执行记录读写。
  - `ExtractionSchemaRepository`：参数 Schema 持久化。
- **向量与模型**:
  - `EmbeddingModel`：文本向量化。
  - `PgVectorEmbeddingStore`：工具向量存储与检索。
- **执行与上下文**:
  - `RestTemplate`：API 工具执行。
  - `InternalToolRegistry`：内部工具执行。
  - `ChatSessionService`：会话上下文加载。

## 4. 调用指南
```java
List<AiTool> tools = aiToolService.searchToolsBySemantic("查询客户最近订单并总结", 5);
for (AiTool tool : tools) {
    System.out.println(tool.getId() + " -> " + tool.getName());
}
```

## 5. 架构师备注
- **一致性策略**: MySQL 与 PGVector 双写采用“最终一致性”，向量写入失败不阻断主交易链路。
- **租户隔离**: 向量元数据携带 `tenantId`，实体回查阶段仍受租户过滤器约束。
- **降级策略**: PGVector 不可用时自动回退关键词检索，确保 Agent 工具选择能力不中断。
