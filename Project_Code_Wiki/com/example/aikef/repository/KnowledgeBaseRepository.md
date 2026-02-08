# KnowledgeBaseRepository

## 1. Class Profile
- **Class Name**: `KnowledgeBaseRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `KnowledgeBase` entities.

## 2. Method Deep Dive
### Query Methods
- `findByEnabledTrue()`: List active KBs.
- `findByIndexName(String indexName)`: Lookup by the technical vector index name.
- `existsByName(String name)`: Uniqueness check.
- `findByCreatedByAgent_Id(UUID agentId)`: Ownership filtering.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.KnowledgeBase`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `AiKnowledgeService` and `KnowledgeBaseController`.
- **RAG Execution**: When performing a search, the system first retrieves the `KnowledgeBase` to get the `embeddingModelId` and `indexName` before querying the vector store.

## 5. Source Link
[KnowledgeBaseRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/KnowledgeBaseRepository.java)
