# ImageTextSplitNode

## Class Profile
`ImageTextSplitNode` (Component ID: `imageTextSplit`) is an advanced AI processing node that analyzes unstructured text (usually from a previous LLM node) to identify and extract image-text pairs (e.g., product recommendations with images). It uses an LLM with structured output to parse the content into a standard JSON format (`struct#{...}`) that the frontend can render as rich UI (carousels, cards).

## Method Deep Dive

### `process()`
- **Description**: Analyzes input for image-text content.
- **Configuration**:
    - `modelId`: LLM to use (optional).
    - `systemPrompt`: Custom extraction instructions (optional).
- **Logic**:
    1.  Constructs a prompt asking the LLM to extract `struct` (array of img/content) and `overview` (summary text).
    2.  Calls `LangChainChatService.chatWithStructuredOutput` with a strict JSON Schema.
    3.  If successful and data exists:
        - Formats output as `struct#{"struct":[...], "overview":"..."}`.
    4.  If no data found: returns original text.

### `extractImageTextData(...)`
- **Description**: The core extraction logic using LangChain4j's structured output.
- **Schema**:
    - `struct`: Array of objects `{img: string, content: string}`.
    - `overview`: String.

## Dependency Graph
- `LangChainChatService`: For LLM interaction.
- `LlmModelService`: For model configuration.
- `ObjectMapper`: JSON processing.

## Usage Guide
Place this node after an LLM node that might return product lists or rich content.

**Scenario:**
1.  LLM Node: "Recommend 3 red dresses." -> Returns text with image URLs.
2.  ImageTextSplitNode: Parses text -> `struct#{"struct":[{"img":"url1","content":"Dress A"}...], "overview":"Here are 3 red dresses"}`.
3.  Frontend: Renders a carousel of dresses.

## Source Link
[ImageTextSplitNode.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/workflow/node/ImageTextSplitNode.java)
