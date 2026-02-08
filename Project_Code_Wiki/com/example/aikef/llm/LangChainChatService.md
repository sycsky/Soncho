# LangChainChatService

## Class Profile
`LangChainChatService` is the central hub for all LLM interactions within the application. It acts as a wrapper around `LangChain4j` components, providing a unified interface to chat with various LLM providers (OpenAI, Azure, Ollama, Zhipu, etc.). It manages model instantiation, caching, and supports advanced features like function calling (`chatWithTools`) and structured JSON output (`chatWithStructuredOutput`).

## Method Deep Dive

### `chat(...)` & `chatWithMessages(...)`
- **Description**: Sends a chat request to a specified LLM.
- **Features**:
    - Supports dynamic model selection via `modelId`.
    - Handles system prompts, user messages, and chat history.
    - Estimates token usage for input and output.
    - Caches `ChatModel` instances to reduce initialization overhead.
    - Supports custom timeouts.

### `chatWithTools(...)`
- **Description**: Executes a chat request with a list of available tools (Function Calling).
- **Logic**: Passes `ToolSpecification`s to the LLM, allowing it to request tool executions.

### `chatWithStructuredOutput(...)`
- **Description**: Enforces a structured JSON response from the LLM.
- **Logic**:
    - Uses `ResponseFormat` (JSON Mode) if supported by the provider (OpenAI, etc.).
    - Fallback: Appends strict instructions to the system prompt for providers that don't support native JSON mode.
    - Validates the output against a provided `JsonObjectSchema`.

### `getOrCreateModel(LlmModel config, ...)`
- **Description**: Internal factory method.
- **Logic**:
    - Checks `modelCache` for an existing instance.
    - Verifies model version (to handle configuration updates).
    - Delegates to provider-specific creation methods (e.g., `createOpenAiCompatibleModel`, `createOllamaModel`).

## Dependency Graph
- `LlmModelService`: To retrieve model configurations.
- `ChatModel` (LangChain4j): The underlying engine.

## Usage Guide
Inject this service to interact with LLMs.

```java
// Basic Chat
LlmChatResponse response = chatService.chat(
    modelId, 
    "You are a helpful assistant.", 
    "Hello!", 
    history, 
    0.7, 
    1000
);

// Structured Output
StructuredOutputResponse jsonParams = chatService.chatWithStructuredOutput(
    modelId,
    "Extract info",
    "My name is Trae",
    jsonSchema,
    "extraction_task",
    0.1
);
```

## Source Link
[LangChainChatService.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/llm/LangChainChatService.java)
