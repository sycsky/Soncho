# AgentSessionRepository

## 1. Class Profile
- **Class Name**: `AgentSessionRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `AgentSession` entities.

## 2. Method Deep Dive
### Query Methods
- `findBySessionIdAndWorkflowIdAndNotEnded(...)`: Checks if a specific workflow is currently "active" and handling this chat.
- `findBySessionIdAndNotEnded(...)`: Checks if *any* agent workflow is handling this chat.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.AgentSession`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `AgentService` and `WorkflowEngine`.
- **Routing**: When a message arrives, the engine first calls `findBySessionIdAndNotEnded`. If found, it bypasses the normal "Trigger" logic and routes the message directly to the active Agent Workflow.

## 5. Source Link
[AgentSessionRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/AgentSessionRepository.java)
