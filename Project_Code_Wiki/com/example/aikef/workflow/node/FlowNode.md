# FlowNode

## Class Profile
`FlowNode` (Component ID: `flow`) is the "Sub-workflow" or "Agent" node. It is responsible for transferring the user into a different workflow. It creates an `AgentSession` which binds the user to the target workflow, ensuring that subsequent messages are routed there. It also immediately executes the target workflow with the current user input.

## Method Deep Dive

### `process()`
- **Description**: Initiates the sub-workflow.
- **Configuration**:
    - `workflowId`: UUID of the target workflow to start.
- **Logic**:
    1.  Validates target workflow exists and is enabled.
    2.  Checks if an `AgentSession` already exists (idempotency).
    3.  Creates a new `AgentSession` with the current node's input as the initial `sysPrompt`.
    4.  **Immediate Execution**: Calls `workflowService.executeWorkflowInternalWithAgentSession` to run the target workflow immediately.
    5.  Sets the output to the result of the target workflow.

## Dependency Graph
- `AiWorkflowRepository`: To load target workflow.
- `AgentSessionRepository`: To persist the session binding.
- `AiWorkflowService`: To execute the target workflow.

## Usage Guide
Use this node to modularize complex logic by breaking it into sub-workflows.

**Liteflow Config:**
```json
{
  "workflowId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Scenario:**
Main router detects intent "Order Status". It routes to a `FlowNode` configured with the "Order Tracking Workflow" ID.

## Source Link
[FlowNode.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/workflow/node/FlowNode.java)
