# YesNoNode

## Class Profile
- **Package**: `com.example.aikef.workflow.node`
- **Type**: Class (LiteFlow Switch Component)
- **Description**: A workflow switch node that evaluates a prompt using an LLM and routes execution based on a "YES" or "NO" response.
- **Key Features**:
  - LLM-based boolean decision making.
  - Template support in prompts.
  - Strict "YES" or "NO" output enforcement.

## Method Deep Dive

### `processSwitch`
- **Description**: Executes the evaluation logic and returns the branch tag.
- **Logic**:
  1. Reads config: `modelId`, `systemPrompt`.
  2. Renders `systemPrompt` template (e.g., `{{var.userInput}}`).
  3. Constructs a strict system instruction: "You are a boolean decision maker... strictly answer 'YES' or 'NO'".
  4. Calls `langChainChatService.chatWithModel`.
  5. Normalizes result to "YES" or "NO".
  6. Sets output to result.
  7. Returns `"tag:" + result` to guide LiteFlow switching.

## Dependency Graph
- **Injected Services**:
  - `LangChainChatService`
- **Extends**: `NodeSwitchComponent`

## Usage Guide
```json
// Node Config
{
  "type": "yes_no",
  "modelId": "uuid...",
  "systemPrompt": "Is the user asking about a refund? Context: {{var.userInput}}"
}
```
Result: Routes to edge with handle "YES" or "NO".
