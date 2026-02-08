# FlowUpdateNode

## Class Profile
`FlowUpdateNode` (Component ID: `flow_update`) allows for dynamic modification of the active `AgentSession`'s system prompt (`sysPrompt`). This is useful for "stateful" agents that need to change their instructions or persona as the conversation progresses without leaving the workflow.

## Method Deep Dive

### `process()`
- **Description**: Updates the system prompt of the current agent session.
- **Configuration**:
    - `updateMode`: `replace` (default) or `append`.
- **Logic**:
    1.  Finds the active `AgentSession`.
    2.  Takes the node's input (from previous node) as the new prompt content.
    3.  Updates `sysPrompt` based on the mode.
    4.  Saves to DB.

## Dependency Graph
- `AgentSessionRepository`: To update the session.

## Usage Guide
**Liteflow Config:**
```json
{
  "updateMode": "append"
}
```

**Scenario:**
1.  Agent starts with generic prompt.
2.  User confirms identity.
3.  `FlowUpdateNode` appends "User is verified. You can now discuss account details." to the system prompt.

## Source Link
[FlowUpdateNode.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/workflow/node/FlowUpdateNode.java)
