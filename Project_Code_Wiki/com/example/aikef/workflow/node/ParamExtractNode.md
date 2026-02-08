# ParamExtractNode

## Class Profile
- **Package**: `com.example.aikef.workflow.node`
- **Type**: Class (LiteFlow Switch Component)
- **Description**: A "Slot Filling" node that uses LLM to extract required parameters for a Tool from the conversation.
- **Key Features**:
  - **Switch Logic**: Returns `"tag:success"` if all params found, `"tag:incomplete"` if missing.
  - **Schema-driven**: Dynamically reads Tool's `ExtractionSchema` (JSON) to know what to extract.
  - **Interactive**: Generates a prompt for the user if parameters are missing.

## Method Deep Dive

### `processSwitch`
- **Description**: Main execution logic.
- **Logic**:
  1. Identifies target Tool (by ID or Name).
  2. Loads Tool's Parameter Definitions (`ExtractionSchema`).
  3. Loads Chat History.
  4. Constructs System Prompt: "Extract parameters X, Y, Z for tool T...".
  5. Calls LLM (Structured Output).
  6. **Validation**: Checks if all `required` parameters are present in LLM output.
  7. **Success Path**: If complete, sets `ctx.setToolParams(toolName, params)` and returns `"tag:success"`.
  8. **Incomplete Path**: If missing, generates a prompt listing missing fields (e.g., "Please provide your Order ID") and returns `"tag:incomplete"`.

## Dependency Graph
- **Injected Services**:
  - `AiToolRepository`
  - `LangChainChatService`
- **Extends**: `NodeSwitchComponent` (LiteFlow)

## Usage Guide
```json
// Node Config
{
  "type": "parameter_extraction",
  "toolName": "check_order_status",
  "historyCount": 10
}
```
