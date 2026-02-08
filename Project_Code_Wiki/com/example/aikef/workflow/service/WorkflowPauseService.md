# Class Profile: WorkflowPauseService

**File Path**: `com/example/aikef/workflow/service/WorkflowPauseService.java`
**Type**: Service (`@Service`)
**Purpose**: Manages the suspension and resumption of workflows. It handles the persistence of workflow state (`WorkflowPausedState`) when execution needs to stop (e.g., waiting for user input during a tool call) and restores context when execution resumes.

# Method Deep Dive

## `pauseWorkflow(...)`
- **Description**: Pauses the workflow and saves the current state to the database.
- **Parameters**: `sessionId`, `workflowId`, `subChainId`, `llmNodeId`, `context`, `collectedParams`, etc.
- **Logic**:
  1. Cancels any existing pending states for the session to ensure only one active pause state.
  2. Creates a `WorkflowPausedState` entity.
  3. Serializes the `WorkflowContext` and `collectedParams` to JSON.
  4. Optionally saves `chatHistoryJson` for LLM context restoration.
  5. Sets an expiration time (default 30 minutes).
  6. Saves to `WorkflowPausedStateRepository`.

## `resumeWorkflow(UUID pausedStateId)`
- **Description**: Marks a paused state as `RESUMED`.
- **Usage**: Called when the user provides the missing information and the workflow is about to continue.

## `findPendingState(UUID sessionId)`
- **Description**: Retrieves the latest active (non-expired, non-completed) paused state for a session.
- **Logic**: First triggers a cleanup/marking of expired states, then queries the repository.

## `updatePausedState(...)`
- **Description**: Updates an existing paused state with newly collected information without resuming yet (e.g., multi-round parameter collection).
- **Updates**: Collected parameters, current round count, next question.

## `serializeContext(WorkflowContext context)` & `deserializeContext(...)`
- **Description**: Helper methods to convert the runtime `WorkflowContext` object to/from a JSON string.
- **Details**: Persists critical data like `variables`, `nodeOutputs`, `query`, and `finalReply`.

## `cleanupOldStates()`
- **Description**: Maintenance method to delete paused states older than 7 days.

# Dependency Graph

**Core Dependencies**:
- `WorkflowPausedStateRepository`: Data access for pause states.
- `WorkflowContext`: The runtime context object being preserved.
- `ObjectMapper`: JSON serialization.

**Key Imports**:
```java
import com.example.aikef.workflow.context.WorkflowContext;
import com.example.aikef.workflow.model.WorkflowPausedState;
import com.example.aikef.workflow.repository.WorkflowPausedStateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
```

# Usage Guide

This service is primarily used by `LlmNode` or `ToolCallProcessor` when they detect a need to wait for user interaction.

## Pausing a Workflow
```java
@Autowired
private WorkflowPauseService pauseService;

// Inside a workflow node
if (toolExecutionResult.needsUserInput()) {
    pauseService.pauseWorkflow(
        sessionId,
        workflowId,
        "subchain_123",
        "llm_node_456",
        "Waiting for order ID",
        currentContext,
        toolId,
        "OrderLookupTool",
        collectedParams,
        1,
        3,
        "Please provide your Order ID."
    );
    throw new WorkflowPausedException("Workflow paused for input");
}
```

## Resuming a Workflow
```java
// When user sends a message
Optional<WorkflowPausedState> stateOpt = pauseService.findPendingState(sessionId);
if (stateOpt.isPresent()) {
    WorkflowPausedState state = stateOpt.get();
    // Restore context
    WorkflowContext context = pauseService.deserializeContext(
        state.getContextJson(), 
        state.getWorkflowId(), 
        sessionId, 
        userMessage
    );
    // Resume execution logic...
    pauseService.resumeWorkflow(state.getId());
}
```

# Source Link
[WorkflowPauseService.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/workflow/service/WorkflowPauseService.java)
