# WorkflowPausedException

## 1. Class Profile
- **Class Name**: `WorkflowPausedException`
- **Package**: `com.example.aikef.workflow.exception`
- **Type**: `Class`
- **Role**: Custom Runtime Exception
- **Purpose**: Signals that the workflow execution needs to be paused, typically to wait for external input (e.g., user clarification during a tool call).

## 2. Method Deep Dive
### Constructors
- `WorkflowPausedException(String pauseReason, String pauseMessage)`: Creates a new exception with a reason code and a user-facing message.

### Methods
- `getPauseReason()`: Returns the reason for the pause.
- `getPauseMessage()`: Returns the message to be displayed to the user or logged.

## 3. Dependency Graph
- **Internal Dependencies**: None.
- **External Dependencies**:
  - `java.lang.RuntimeException`: Base class for unchecked exceptions.

## 4. Usage Guide
This exception is thrown by the workflow engine when it encounters a state that requires suspension.
- **Scenario**: When an LLM tool call requires missing parameters, the system throws this exception to halt execution.
- **Handling**: The exception is caught by the workflow execution service, which then persists the current state (using `WorkflowPausedState`) and returns a response to the user asking for the missing information.
- **Resumption**: Once the user provides the input, the workflow is resumed from the saved state.

## 5. Source Link
[WorkflowPausedException.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/workflow/exception/WorkflowPausedException.java)
