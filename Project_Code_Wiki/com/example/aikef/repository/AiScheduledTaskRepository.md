# AiScheduledTaskRepository

## 1. Class Profile
- **Class Name**: `AiScheduledTaskRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `AiScheduledTask` entities.

## 2. Method Deep Dive
### Query Methods
- `findByEnabledTrueAndNextRunAtLessThanEqual(Instant time)`: The core polling query used by the scheduler to find due tasks.
- `findByWorkflowId(UUID workflowId)`: Find tasks dependent on a specific workflow (useful for warning users before deleting a workflow).

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.AiScheduledTask`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `AiScheduledTaskService`.
- **Polling**: Every minute, the system runs `findByEnabledTrueAndNextRunAtLessThanEqual(Instant.now())` to pick up jobs.

## 5. Source Link
[AiScheduledTaskRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/AiScheduledTaskRepository.java)
