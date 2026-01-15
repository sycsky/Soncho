# AI Agent Workflow Nodes Documentation

This document details the configuration and parameters for the available workflow nodes in the AI Agent system.

## Node Types

### 1. Start Node (`start`)
- **Description**: The entry point of the workflow. Every workflow must have exactly one Start node.
- **Parameters**: None.

### 2. End Node (`end`)
- **Description**: The exit point of the workflow.
- **Parameters**: None.

### 3. Intent Recognition (`intent`)
- **Description**: Classifies user input into one of several defined intents using an LLM. This is a **Switch** node.
- **Configuration (`data.config`)**:
    - `modelId` (String, Required): ID of the LLM model to use for classification.
    - `customPrompt` (String, Optional): Custom system prompt to guide the classification.
    - `historyCount` (Number, Default: 0): Number of historical conversation turns to include in the context.
    - `intents` (Array, Required): List of possible intents.
        - `id` (String): Unique ID for the intent.
        - `label` (String): Description of the intent (e.g., "Check Order Status").
- **Outputs**:
    - Each defined intent `id` is a valid output handle.

### 4. LLM Generation (`llm`)
- **Description**: Generates a response using a Large Language Model.
- **Configuration (`data.config`)**:
    - `modelId` (String, Required): ID of the LLM model.
    - `systemPrompt` (String, Optional): The system instructions for the LLM.
    - `temperature` (Number, Default: 0.7): Controls randomness (0.0 to 2.0).
    - `useHistory` (Boolean, Default: true): Whether to include chat history.
    - `readCount` (Number, Default: 5): Number of recent messages to include if `useHistory` is true.
    - `tools` (Array<String>, Optional): List of Tool IDs that the LLM can call during generation.

### 5. Knowledge Retrieval (`knowledge`)
- **Description**: Searches for relevant information in the knowledge base.
- **Configuration (`data.config`)**:
    - `knowledgeBaseIds` (Array<String>, Required): IDs of the knowledge bases to search.
    - `topK` (Number, Default: 3): Number of results to retrieve.
    - `scoreThreshold` (Number, Default: 0.5): Minimum similarity score for results.

### 6. Direct Reply (`reply`)
- **Description**: Sends a fixed message or the output from a previous node (like LLM) to the user.
- **Configuration (`data.config`)**:
    - `text` (String, Optional): The fixed text content to send (if not using dynamic output).
    - `source` (String, Optional): Source of the reply (e.g., 'LLM Output', 'Fixed Text').

### 7. Transfer to Human (`human_transfer`)
- **Description**: Transfers the conversation to a human agent.
- **Parameters**: None.

### 8. Sub-Workflow (`flow`)
- **Description**: Executes another existing workflow.
- **Configuration (`data.config`)**:
    - `workflowId` (String, Required): ID of the sub-workflow to execute.

### 9. Sub-Workflow End (`flow_end`)
- **Description**: Marks the end of a sub-workflow execution path. Used within the sub-workflow to return control.
- **Parameters**: None.

### 10. Sub-Workflow Update (`flow_update`)
- **Description**: Updates the context (System Prompt) of the current sub-workflow session.
- **Configuration (`data.config`)**:
    - `updateMode` (String, Default: "replace"): "replace" or "append".

### 11. Autonomous Agent (`agent`)
- **Description**: An advanced autonomous agent capable of multi-step reasoning and tool usage (ReAct pattern).
- **Configuration (`data.config`)**:
    - `goal` (String, Required): The goal or instruction for the agent.
    - `modelId` (String, Required): ID of the LLM model.
    - `tools` (Array<String>, Optional): List of Tool IDs the agent can use.
    - `maxIterations` (Number, Default: 10): Maximum reasoning steps.
    - `useHistory` (Boolean, Default: true): Whether to use conversation history.

### 12. Tool Execution (`tool`)
- **Description**: Executes a specific tool or function directly. This is a **Switch** node.
- **Configuration (`data.config`)**:
    - `toolId` (String, Required): ID of the tool to execute.
    - `toolName` (String, Read-only): Name of the selected tool.
- **Outputs**:
    - `executed`: Tool execution successful.
    - `not_executed`: Tool execution failed or not run.

### 13. Image-Text Split (`imageTextSplit`)
- **Description**: Uses an AI model to split and analyze image and text content.
- **Configuration (`data.config`)**:
    - `modelId` (String, Required): ID of the Multimodal model.
    - `systemPrompt` (String, Optional): Instructions for the analysis.

### 14. Set Metadata (`setSessionMetadata`)
- **Description**: Extracts information from the conversation context and updates session metadata.
- **Configuration (`data.config`)**:
    - `modelId` (String, Required): ID of the LLM model used for extraction.
    - `systemPrompt` (String, Optional): Instructions for extraction.
    - `mappings` (Object): Key-value pairs mapping extracted JSON fields to session metadata keys.

### 15. Condition Switch (`condition`)
- **Description**: Routes the flow based on conditional logic. This is a **Switch** node.
- **Configuration (`data.config`)**:
    - `conditions` (Array, Required): List of conditions.
        - `id` (String): Unique ID for the condition (used as output handle).
        - `sourceValue` (String): The value to check (supports templates like `{{sys.lastoutput}}`).
        - `conditionType` (String): Operator (`contains`, `notContains`, `startsWith`, `endsWith`, `equals`, `notEquals`, `isEmpty`, `isNotEmpty`).
        - `inputValue` (String): The value to compare against.
- **Outputs**:
    - Matches condition `id` or fallback to `else` (if no match).

### 16. Parameter Extraction (`parameter_extraction`)
- **Description**: Extracts parameters from conversation for a specific tool. This is a **Switch** node.
- **Configuration (`data.config`)**:
    - `toolId` (String, Optional): ID of the tool.
    - `toolName` (String, Optional): Name of the tool.
    - `modelId` (String, Optional): Model used for extraction.
    - `historyCount` (Number, Default: 0): History turns to use.
    - `extractParams` (Array<String>, Optional): Specific parameters to extract.
- **Outputs**:
    - `success`: All parameters extracted.
    - `incomplete`: Missing parameters (usually connects to a reply node to ask user).

### 17. Variable Operation (`variable`)
- **Description**: Sets or modifies workflow variables.
- **Configuration (`data.config`)**:
    - `variableName` (String): Name of the variable to set.
    - `sourceField` (String): JSON path to extract from last output (or `#lastResponse`).
    - `sourceNodeId` (String, Optional): Specific node to read output from.
    - OR
    - `operation` (String): `set`, `append`, `delete`.
    - `variables` (Object): Key-value pairs to set.

### 18. Translation (`translation`)
- **Description**: Translates target text to the user's language (inferred from history).
- **Configuration (`data.config`)**:
    - `modelId` (String, Required): Model ID.
    - `targetText` (String, Required): Text to translate (supports templates).
    - `systemPrompt` (String, Optional): Additional style instructions.
    - `historyCount` (Number, Default: 10): Context for language inference.
    - `outputVar` (String, Default: "translationResult"): Variable to store result.

### 19. API Call (`api`)
- **Description**: Makes an HTTP request to an external API.
- **Configuration (`data.config`)**:
    - `url` (String, Required): Target URL.
    - `method` (String, Default: "GET"): HTTP method.
    - `headers` (Object): HTTP headers.
    - `body` (Object): Request body (for POST/PUT).
    - `responseMapping` (String, Optional): JSONPath to extract specific data from response.
    - `saveToVariable` (String, Optional): Variable to save the output to.

---

## Generated Workflow Demo

### `edgesJson`
```json
[
  {"type":"custom","animated":true,"style":{"stroke":"#94a3b8"},"deletable":true,"source":"c0w6g","target":"fkuxd5","id":"xy-edge__c0w6g-fkuxd5"},
  {"type":"custom","animated":true,"style":{"stroke":"#94a3b8"},"deletable":true,"source":"fkuxd5","sourceHandle":"i1765355853730","target":"qda9gq","id":"xy-edge__fkuxd5i1765355853730-qda9gq","selected":false},
  {"type":"custom","animated":true,"style":{"stroke":"#94a3b8"},"deletable":true,"source":"fkuxd5","sourceHandle":"i1765355844905","target":"x81z87","id":"xy-edge__fkuxd5i1765355844905-x81z87"},
  {"type":"custom","animated":true,"style":{"stroke":"#94a3b8"},"deletable":true,"source":"x81z87","target":"qda9gq","id":"xy-edge__x81z87-qda9gq"},
  {"type":"custom","animated":true,"style":{"stroke":"#94a3b8"},"deletable":true,"source":"90bm6n","target":"qda9gq","id":"xy-edge__90bm6n-qda9gq"},
  {"type":"custom","animated":true,"style":{"stroke":"#94a3b8"},"deletable":true,"source":"fkuxd5","sourceHandle":"i1765355858385","target":"0hu2v5","id":"xy-edge__fkuxd5i1765355858385-0hu2v5"},
  {"type":"custom","animated":true,"style":{"stroke":"#94a3b8"},"deletable":true,"source":"0hu2v5","sourceHandle":"executed","target":"90bm6n","id":"xy-edge__0hu2v5executed-90bm6n"},
  {"type":"custom","animated":true,"style":{"stroke":"#94a3b8"},"deletable":true,"source":"0hu2v5","sourceHandle":"not_executed","target":"qda9gq","id":"xy-edge__0hu2v5not_executed-qda9gq"},
  {"type":"custom","animated":true,"style":{"stroke":"#94a3b8"},"deletable":true,"source":"x81z87","target":"3600p9","id":"xy-edge__x81z87-3600p9"}
]
```

### `nodesJson`
```json
[
  {
    "id": "c0w6g",
    "type": "start",
    "position": { "x": -73.99, "y": -189.88 },
    "data": { "label": "Start" },
    "measured": { "width": 200, "height": 89 },
    "selected": false,
    "dragging": false
  },
  {
    "id": "fkuxd5",
    "type": "intent",
    "position": { "x": 651.96, "y": -252.64 },
    "data": {
      "label": "Intent Recognition",
      "config": {
        "intents": [
          { "id": "i1765355844905", "label": "单纯聊天" },
          { "id": "i1765355853730", "label": "查询订单" },
          { "id": "i1765355858385", "label": "询问售卖的商品，想购买商品" }
        ],
        "modelId": "77e70e47-cc73-11f0-83b5-345a60a971df",
        "model": "gpt-4o-mini",
        "modelDisplayName": "gpt-4o-mini",
        "provider": "OPENAI",
        "historyTurns": 10,
        "historyCount": 5,
        "customPrompt": "你是一个线上餐厅客服，如果关于食物的想法，或者商品都理解成购买商品，请理解客户的想法"
      }
    },
    "measured": { "width": 280, "height": 255 },
    "selected": false,
    "dragging": false
  },
  {
    "id": "qda9gq",
    "type": "end",
    "position": { "x": 2138.68, "y": 152.98 },
    "data": { "label": "End" },
    "measured": { "width": 200, "height": 89 },
    "selected": false,
    "dragging": false
  },
  {
    "id": "x81z87",
    "type": "llm",
    "position": { "x": 1477.39, "y": -198.26 },
    "data": {
      "label": "LLM Generation",
      "config": {
        "modelId": "07c694e8-cc55-11f0-83b5-345a60a971df",
        "model": "gpt-4o",
        "modelDisplayName": "GPT-4o",
        "provider": "OPENAI",
        "systemPrompt": "## 你是一个火锅点餐平台客服根据用户提问回答问题\n\n## ",
        "messages": [],
        "useHistory": true,
        "readCount": 5,
        "temperature": 0.3
      }
    },
    "measured": { "width": 240, "height": 140 },
    "selected": false,
    "dragging": false
  },
  {
    "id": "90bm6n",
    "type": "flow",
    "position": { "x": 1246.09, "y": 234.61 },
    "data": {
      "label": "Sub-Workflow",
      "config": {
        "workflowId": "22576f74-1047-420c-8655-0526c9f3859a",
        "workflowName": "推销员"
      }
    },
    "measured": { "width": 240, "height": 140 },
    "selected": false,
    "dragging": false
  },
  {
    "id": "0hu2v5",
    "type": "tool",
    "position": { "x": 1019.88, "y": 661.50 },
    "data": {
      "label": "Tool Execution",
      "config": {
        "toolId": "52e289e7-d6d9-4dc9-92be-a2751a72ddae",
        "toolName": "getProducts"
      }
    },
    "measured": { "width": 240, "height": 198 },
    "selected": false,
    "dragging": false
  },
  {
    "id": "3600p9",
    "type": "reply",
    "position": { "x": 1850.27, "y": -249.46 },
    "data": {
      "label": "Direct Reply",
      "text": "测试输出"
    },
    "measured": { "width": 240, "height": 159 },
    "selected": true,
    "dragging": false
  }
]
```
