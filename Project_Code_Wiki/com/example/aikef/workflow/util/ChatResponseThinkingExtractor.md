# ChatResponseThinkingExtractor

## 1. Class Profile
- **Class Name**: `ChatResponseThinkingExtractor`
- **Package**: `com.example.aikef.workflow.util`
- **Type**: `Utility Class`
- **Role**: Helper / Parser
- **Purpose**: Extracts "thinking" or "reasoning" content (Chain-of-Thought) from LLM responses. This is essential for models that output internal reasoning processes (like DeepSeek-R1) which should be stored but potentially hidden from the final user response.

## 2. Method Deep Dive
### `enrichAiMessage(ChatResponse response, ObjectMapper objectMapper)`
- **Functionality**: Processes a `ChatResponse` to separate the "thinking" content from the actual response text.
- **Logic**:
  1. **Tag Extraction**: Checks for content wrapped in `<think>...</think>` tags in the message text. If found, extracts it and removes it from the main text.
  2. **Metadata Extraction**: If no tags are found, attempts to inspect the raw response metadata (e.g., OpenAI's `reasoning_content` field).
  3. **Reconstruction**: Returns a new `AiMessage` with the `thinking` field populated and the `text` cleaned.

### `extractThinkingFromMetadata(...)`
- **Functionality**: A robust reflection-based method to dig into the raw HTTP response body stored in the metadata to find fields like `reasoning`, `thinking`, `reasoning_content`, or `analysis`.

## 3. Dependency Graph
- **External Dependencies**:
  - `dev.langchain4j.data.message.AiMessage`: LangChain4j message model.
  - `dev.langchain4j.model.chat.response.ChatResponse`: LangChain4j response model.
  - `com.fasterxml.jackson.databind.ObjectMapper`: JSON parsing.
  - `org.springframework.util.StringUtils`: String manipulation utilities.

## 4. Usage Guide
Used by `LlmNode` or `AgentService` immediately after receiving a response from an LLM.
```java
ChatResponse response = model.generate(messages);
AiMessage cleanMessage = ChatResponseThinkingExtractor.enrichAiMessage(response, objectMapper);
// cleanMessage.text() -> "The answer is 42."
// cleanMessage.thinking() -> "Calculating 6 * 7..."
```

## 5. Source Link
[ChatResponseThinkingExtractor.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/workflow/util/ChatResponseThinkingExtractor.java)
