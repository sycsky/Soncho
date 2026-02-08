# AiWorkflowRepository

## 1. Class Profile
- **Class Name**: `AiWorkflowRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `AiWorkflow` entities, focusing on retrieval for execution and management.

## 2. Method Deep Dive
### Query Methods
- `findByName(String name)`: Lookup by name.
- `findByEnabledTrueOrderByCreatedAtDesc()`: List all active workflows.
- `findByIsDefaultTrueAndEnabledTrue()`: Find the global fallback workflow.
- `findByTriggerTypeAndEnabledTrue...`: Find workflows that trigger on specific events (e.g., "Keyword").
- `findByCategoryId(String categoryId)`: Specialized search inside the JSON `triggerConfig` to find workflows linked to a specific session category.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.AiWorkflow`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `AiWorkflowService` and `WorkflowEngine`.
- **Matching**: When a new session starts, the engine calls `findByTriggerType...` to see if any workflow matches the session's context. If not, it falls back to `findByIsDefaultTrue...`.

## 5. Source Link
[AiWorkflowRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/AiWorkflowRepository.java)
