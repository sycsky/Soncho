# KnowledgeNode

## 1. 类档案 (Class Profile)
- **功能定义**：知识库检索节点。使用向量搜索技术从知识库中检索相关内容，并注入到上下文中供后续节点（如 LLM）使用。
- **注解与配置**：
  - `@LiteflowComponent("knowledge")`: 注册为 LiteFlow 组件。
- **继承/实现**：继承自 `BaseWorkflowNode`。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `process` | - | 1. 确定查询词来源（用户输入、上一步输出或自定义）。<br>2. 解析目标知识库 ID 列表（支持多库检索）。<br>3. 调用 `vectorStoreService.search` 执行向量相似度搜索。<br>4. **结果格式化**：支持 combined (拼接), list, json 等格式。<br>5. 将结果存入变量 `knowledgeResults`, `knowledgeContent`。 | 核心 RAG (Retrieval-Augmented Generation) 组件。 |
| `getKnowledgeBaseIds` | - | 解析复杂的配置格式（支持 String, UUID, List, JsonArray 等多种输入格式的兼容处理）。 | 鲁棒性处理，兼容前端传输的各种数据格式。 |

## 3. 依赖全景 (Dependency Graph)
- **`VectorStoreService`**: 向量数据库接口（如 Redis, Milvus）。
- **`KnowledgeBaseService`**: 知识库元数据管理。

## 4. 调用指南 (Usage Guide)
通常放置在 LLM 节点之前。
1.  **KnowledgeNode**: 检索相关文档 -> 存入 `{{knowledgeContent}}`。
2.  **LlmNode**: System Prompt 中引用 `参考信息：{{knowledgeContent}}`。
