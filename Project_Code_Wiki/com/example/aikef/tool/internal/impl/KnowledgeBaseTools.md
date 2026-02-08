# KnowledgeBaseTools

## 1. 类档案 (Class Profile)
- **功能定义**：知识库搜索工具。允许 AI 根据用户问题智能选择相关的知识库并进行搜索。
- **注解与配置**：
  - `@Component`: 注册为 Spring 组件。
  - `@Tool`: 暴露给 AI 使用。
- **继承/实现**：无。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `searchKnowledgeBaseByKeyword` | In: `userQuestion`<br>Out: `String` (Formatted Results) | 1. 获取所有启用的知识库列表。<br>2. **AI 路由**：调用 LLM (`chatWithFieldDefinitions`)，让 AI 根据问题决定查询哪些知识库（Selection）。<br>3. **向量搜索**：在选定的知识库中执行 `searchMultiple`。<br>4. 格式化返回结果。 | 实现了 "Routing -> Retrieval" 的 RAG 模式，比全库搜索更精准。 |

## 3. 依赖全景 (Dependency Graph)
- **`KnowledgeBaseService`**: 知识库元数据和搜索逻辑。
- **`LangChainChatService`**: 用于知识库选择的推理。

## 4. 调用指南 (Usage Guide)
**AI 调用示例**：
`searchKnowledgeBaseByKeyword(userQuestion="How do I return a product?")`
