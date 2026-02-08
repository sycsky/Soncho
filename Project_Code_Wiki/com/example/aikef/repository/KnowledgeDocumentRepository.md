# KnowledgeDocumentRepository

## 1. Class Profile
- **Class Name**: `KnowledgeDocumentRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `KnowledgeDocument` entities, managing the content within knowledge bases.

## 2. Method Deep Dive
### Query Methods
- `findByKnowledgeBase_Id(...)`: Lists documents in a specific KB (supports pagination).
- `findByStatus(ProcessStatus status)`: Finds documents waiting for processing (e.g., finding `PENDING` items for the job queue).
- `getTotalChunkCount(UUID kbId)`: Aggregates the total number of vectors in a KB.

### Modification Methods
- `deleteByKnowledgeBase_Id(UUID kbId)`: Cleanup when a KB is deleted.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.KnowledgeDocument`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `AiKnowledgeService` and background processing tasks.
- **Job Queue**: A scheduled task calls `findByStatus(PENDING)` to find new uploads that need vectorization.

## 5. Source Link
[KnowledgeDocumentRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/KnowledgeDocumentRepository.java)
