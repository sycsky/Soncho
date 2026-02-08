# FlowEndNode

## Class Profile
`FlowEndNode` (Component ID: `flow_end`) is a specialized workflow node designed to terminate an active `AgentSession`. When a user enters a specific sub-workflow (via `FlowNode`), they are "bound" to it via an `AgentSession`. This node marks that session as ended, effectively releasing the user from the sub-workflow and allowing them to return to the global routing logic (e.g., Intent classification).

## Method Deep Dive

### `process()`
- **Description**: Executes the session termination logic.
- **Logic**:
    1.  Validates that `sessionId` and `workflowId` exist in the context.
    2.  Finds the active `AgentSession` for the current workflow.
    3.  Sets `ended = true` and `endedAt = now`.
    4.  Saves to the database.
    5.  Outputs `agent_session_ended`.

## Dependency Graph
- `AgentSessionRepository`: To update the session status.

## Usage Guide
Use this node at the logical end of a sub-workflow (Agent Workflow).

**Liteflow Config:**
```xml
<node id="end_agent" type="flow_end"/>
```

**Scenario:**
User finishes a "Password Reset" workflow. The `FlowEndNode` ensures their next message isn't treated as part of the password reset process anymore.

## Source Link
[FlowEndNode.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/workflow/node/FlowEndNode.java)
