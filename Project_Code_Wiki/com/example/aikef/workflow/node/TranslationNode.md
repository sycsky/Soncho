# TranslationNode

## Class Profile
- **Package**: `com.example.aikef.workflow.node`
- **Type**: Class (LiteFlow Component)
- **Description**: A workflow node that translates text using an LLM. It considers conversation history to detect the target language (User's language).
- **Key Features**:
  - Context-aware translation (Auto-detects target language from history).
  - Configurable prompts and history depth.

## Method Deep Dive

### `process`
- **Description**: Executes the translation logic.
- **Logic**:
  1. Reads config: `targetText`, `systemPrompt`, `historyCount`.
  2. Renders templates in `targetText` (e.g., `{{var.input}}`).
  3. Loads `historyCount` messages to build context.
  4. Constructs LLM prompt: "Translate 'Target Text' into the identified User's language...".
  5. Calls `langChainChatService`.
  6. Stores result in `ctx.setOutput`.

## Dependency Graph
- **Injected Services**:
  - `LangChainChatService`
  - `HistoryMessageLoader`
- **Extends**: `BaseWorkflowNode`

## Usage Guide
```json
// Node Config
{
  "type": "translation",
  "targetText": "{{var.productDescription}}",
  "historyCount": 5
}
```
