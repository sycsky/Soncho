# WorkflowExecutionLog

## 1. Class Profile
- **Class Name**: `WorkflowExecutionLog`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Audit Log / Execution Trace
- **Purpose**: Persists the complete execution trace of a workflow, including every node visit, variable change, and tool output. This is vital for debugging and "Explainability".

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `workflow`: The definition executed.
- `session`: The context of the execution.
- `messageId`: The trigger message.
- `status`: `SUCCESS`, `FAILED`, `TIMEOUT`.
- `userInput`: The prompt that started it.
- `finalOutput`: The result sent back to the user.
- `nodeDetails`: A large JSON blob recording the step-by-step path taken (Start -> LLM -> Condition -> End).
- `toolExecutionChain`: JSON list of tool calls made during this execution.
- `errorMessage`: Stack trace if failed.
- `durationMs`: Performance metric.
- `startedAt` / `finishedAt`: Timestamps.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.AiWorkflow`
  - `com.example.aikef.model.ChatSession`
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
This entity is created by `WorkflowEngine`.
- **Debugging**: An admin clicks "View Logs" on a workflow. The frontend requests `GET /api/v1/workflow-logs?workflowId=...` and renders the visual path using `nodeDetails`.

## 5. Source Link
[WorkflowExecutionLog.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/WorkflowExecutionLog.java)
