# ToolExecution

## 1. Class Profile
- **Class Name**: `ToolExecution`
- **Package**: `com.example.aikef.tool.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Audit Log / Execution Record
- **Purpose**: Records every execution of an AI tool, including inputs, outputs, duration, and status. This is critical for debugging, auditing, and usage analytics.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `tool`: Reference to the `AiTool` definition.
- `sessionId`: The chat session context (optional).
- `status`: Execution status (`PENDING`, `RUNNING`, `SUCCESS`, `FAILED`, `TIMEOUT`, `CANCELLED`).
- `inputParams`: JSON string of arguments provided by the AI.
- `outputResult`: JSON string of the raw result returned by the tool.
- `errorMessage`: Captured exception message or error response.
- `durationMs`: Execution time in milliseconds.
- `httpStatus`: HTTP status code (for API tools).
- `retryCount`: Number of retries attempted.
- `triggerSource`: Where the call originated (e.g., "workflow", "chat").
- `executedBy`: User or Agent ID who triggered the action.
- `startedAt` / `finishedAt`: Precise timestamps.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.tool.model.AiTool`: The tool definition.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.
  - `java.time.Instant`: Timestamp handling.

## 4. Usage Guide
This entity serves as the "black box" recorder for agent actions.
- **Auditing**: "Why did the agent refund order #123?" -> Check `ToolExecution` logs for the specific call and parameters.
- **Performance**: Calculate average `durationMs` to identify slow external APIs.
- **Debugging**: Inspect `errorMessage` and `httpStatus` for failed integrations.

## 5. Source Link
[ToolExecution.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/tool/model/ToolExecution.java)
