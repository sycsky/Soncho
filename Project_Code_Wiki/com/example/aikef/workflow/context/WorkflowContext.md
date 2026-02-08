# WorkflowContext

## Class Profile
- **Package**: `com.example.aikef.workflow.context`
- **Type**: Class
- **Description**: The runtime state container for a workflow execution. Holds all variables, node outputs, execution history, and session metadata.
- **Key Features**:
  - Variable Management (Set/Get).
  - Node Output Tracking (Last Output, Specific Node Output).
  - Tool Execution Chain Logging.
  - Workflow Pause/Resume State.
  - Streaming Status Management.

## Method Deep Dive

### `getLastOutput`
- **Description**: Retrieves the output of the most recently executed node.
- **Signature**: `public String getLastOutput()`
- **Logic**:
  1. Checks `nodeExecutionDetails` to find the last executed node ID.
  2. Returns that node's output.
  3. If no nodes executed, returns the initial `query`.

### `addToolExecution`
- **Description**: Logs a tool execution (API or internal) for debugging and history.
- **Signature**: `public void addToolExecution(String nodeId, String nodeType, String toolName, String args, String result, String error, long durationMs, boolean success)`
- **Logic**: Adds entry to `toolExecutionChain` and `nodeToolExecutions`.

### `pauseWorkflow`
- **Description**: Marks the workflow as paused, usually waiting for user input (e.g., Human Transfer or Missing Params).
- **Signature**: `public void pauseWorkflow(String reason, String message)`
- **Logic**: Sets `paused = true` and stores the reason/message.

## Dependency Graph
- **Used By**:
  - All Workflow Nodes (`BaseWorkflowNode` subclasses).
  - `AiWorkflowService` (to initialize and pass to engine).
  - `TemplateEngine` (for variable rendering).
- **DTOs**:
  - `NodeExecutionDetail`, `ChatHistoryItem`, `ToolCallState`

## Usage Guide
```java
// Accessing context in a node
WorkflowContext ctx = this.getContextBean(WorkflowContext.class);
String userInput = ctx.getQuery();
ctx.setVariable("orderId", "12345");
```
