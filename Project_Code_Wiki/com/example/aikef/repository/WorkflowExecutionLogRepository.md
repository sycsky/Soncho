# WorkflowExecutionLogRepository

## 1. Class Profile
- **Class Name**: `WorkflowExecutionLogRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `WorkflowExecutionLog` entities.

## 2. Method Deep Dive
### Query Methods
- `findByWorkflow_IdOrderByCreatedAtDesc(...)`: List logs for a specific workflow (Admin UI).
- `findBySession_IdOrderByCreatedAtDesc(...)`: List logs for a specific chat (Customer Support UI).
- `countByWorkflowIdAndTimeRange(...)`: Analytics (e.g., "How many times did this run yesterday?").
- `getAverageDuration(...)`: Performance monitoring.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.WorkflowExecutionLog`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `WorkflowExecutionLogService`.
- **Analytics**: `countByWorkflowIdAndTimeRange` helps determine which workflows are most popular or resource-intensive.

## 5. Source Link
[WorkflowExecutionLogRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/WorkflowExecutionLogRepository.java)
