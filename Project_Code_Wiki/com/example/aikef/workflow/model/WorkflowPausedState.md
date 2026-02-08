# WorkflowPausedState

## 1. Class Profile
- **Class Name**: `WorkflowPausedState`
- **Package**: `com.example.aikef.workflow.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Persistence Entity
- **Purpose**: Persists the state of a paused workflow execution, allowing it to be resumed later. This is crucial for handling long-running or interactive workflows (e.g., waiting for user input during a tool call).

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `sessionId`: The chat session ID associated with this execution.
- `workflowId`: The ID of the workflow being executed.
- `subChainId`: The ID of the sub-chain (if applicable).
- `llmNodeId`: The ID of the LLM node where execution paused.
- `pauseReason`: The reason for pausing (e.g., "WAITING_USER_INPUT").
- `contextJson`: Serialized `WorkflowContext` containing all execution variables.
- `toolCallStateJson`: Serialized `ToolCallState` tracking tool execution progress.
- `collectedParamsJson`: JSON storage for parameters collected so far.
- `currentRound` / `maxRounds`: Tracking for multi-turn loops.
- `pendingToolId` / `pendingToolName`: Details of the tool waiting for execution.
- `nextQuestion`: The question asked to the user to resolve the pause.
- `chatHistoryJson`: Serialized chat history at the moment of pause.
- `status`: Current status (`WAITING_USER_INPUT`, `RESUMED`, `COMPLETED`, `EXPIRED`, `CANCELLED`).
- `createdAt` / `updatedAt` / `expiredAt`: Timestamps for lifecycle management.

### Lifecycle Methods
- `onCreate()`: Sets creation/update timestamps and default expiration (30 minutes).
- `onUpdate()`: Updates the modification timestamp.

## 3. Dependency Graph
- **External Dependencies**:
  - `jakarta.persistence.*`: JPA annotations for ORM mapping.
  - `java.time.Instant`: For timestamp handling.
  - `java.util.UUID`: For unique identifiers.

## 4. Usage Guide
This entity is the core of the "Human-in-the-loop" mechanism for AI tools.
1. **Pause**: When an AI tool needs user clarification, the current memory and context are serialized into this entity.
2. **Wait**: The system waits for a user message in the same session.
3. **Resume**: When a new message arrives, the system checks for a pending `WorkflowPausedState`. If found, it deserializes the context and resumes execution from the paused node, injecting the user's new input.

## 5. Source Link
[WorkflowPausedState.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/workflow/model/WorkflowPausedState.java)
