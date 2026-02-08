# WorkflowPausedStateRepository

## 1. Class Profile
- **Class Name**: `WorkflowPausedStateRepository`
- **Package**: `com.example.aikef.workflow.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `WorkflowPausedState` entities, extending JpaRepository.

## 2. Method Deep Dive
### Query Methods
- `findPendingBySessionId(UUID sessionId, Instant now)`: Finds all active paused states for a session that are waiting for user input and haven't expired.
- `findLatestPendingBySessionId(UUID sessionId)`: Default method that returns the most recent pending state for a session.
- `findByWorkflowIdOrderByCreatedAtDesc(UUID workflowId)`: Retrieves execution history for a specific workflow.

### Modification Methods
- `cancelAllPendingBySessionId(UUID sessionId, Instant now)`: Cancels all pending states for a session (e.g., when a user starts a new topic).
- `markExpiredStates(Instant now)`: Batch update to mark states as `EXPIRED` if they have passed their expiration time.
- `deleteOldStates(Instant before)`: Cleanup method to permanently delete old, completed, or cancelled states.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.workflow.model.WorkflowPausedState`: The entity being managed.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.*`: Spring Data JPA interfaces and annotations.

## 4. Usage Guide
This repository is used by the `WorkflowExecutionService` and scheduled tasks.
- **Session Resume**: When a user sends a message, `findLatestPendingBySessionId` is called to check if the message is a response to a pending AI question.
- **Cleanup**: A background task periodically calls `markExpiredStates` and `deleteOldStates` to maintain database hygiene.

## 5. Source Link
[WorkflowPausedStateRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/workflow/repository/WorkflowPausedStateRepository.java)
