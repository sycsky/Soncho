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
- **Description**: Classifies user input into one of several defined intents using an LLM.
- **Configuration (`data.config`)**:
    - `modelId` (String, Required): ID of the LLM model to use for classification.
    - `customPrompt` (String, Optional): Custom system prompt to guide the classification.
    - `historyCount` (Number, Default: 0): Number of historical conversation turns to include in the context.
    - `intents` (Array, Required): List of possible intents.
        - `id` (String): Unique ID for the intent.
        - `label` (String): Description of the intent (e.g., "Check Order Status").

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

### 8. Agent (`agent`)
- **Description**: Executes another workflow (sub-workflow).
- **Configuration (`data.config`)**:
    - `workflowId` (String, Required): ID of the sub-workflow to execute.
    - `workflowName` (String, Read-only): Name of the selected workflow.

### 9. Agent End (`agent_end`)
- **Description**: Marks the end of an agent execution path.
- **Parameters**: None.

### 10. Agent Update (`agent_update`)
- **Description**: Updates the state or context of the agent.
- **Parameters**: None.

### 11. Tool Execution (`tool`)
- **Description**: Executes a specific tool or function.
- **Configuration (`data.config`)**:
    - `toolId` (String, Required): ID of the tool to execute.
    - `toolName` (String, Read-only): Name of the selected tool.

### 12. Image-Text Split (`imageTextSplit`)
- **Description**: Uses an AI model to split and analyze image and text content.
- **Configuration (`data.config`)**:
    - `modelId` (String, Required): ID of the Multimodal model.
    - `systemPrompt` (String, Optional): Instructions for the analysis.

### 13. Set Metadata (`setSessionMetadata`)
- **Description**: Extracts information from the conversation context and updates session metadata.
- **Configuration (`data.config`)**:
    - `modelId` (String, Required): ID of the LLM model used for extraction.
    - `systemPrompt` (String, Optional): Instructions for extraction.
    - `mappings` (Object): Key-value pairs mapping extracted JSON fields to session metadata keys.

---

## Generated Workflow Demo

Below is an example of a workflow configuration JSON. This example represents a customer service flow for an online restaurant, including intent recognition ("Chat", "Check Order", "Buy Items"), product search via tool, LLM response, and agent delegation.

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
    "type": "agent",
    "position": { "x": 1246.09, "y": 234.61 },
    "data": {
      "label": "Agent",
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
