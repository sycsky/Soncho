# LlmModelService

## Class Profile
`LlmModelService` manages the lifecycle and configuration of Large Language Models (LLMs) in the system. It provides CRUD operations for `LlmModel` entities, handles validation of provider configurations, and manages the "Default Model" selection. It ensures that critical constraints (like unique model codes) are respected.

## Method Deep Dive

### `createModel(SaveLlmModelRequest request)` / `updateModel(...)`
- **Description**: Creates or updates an LLM configuration.
- **Logic**:
    - Validates uniqueness of the model `code`.
    - Maps request DTO to `LlmModel` entity.
    - Handles `isDefault` logic: if a new model is set as default, it unsets the previous default.

### `setDefaultModel(UUID modelId)`
- **Description**: Explicitly sets a model as the system default.
- **Constraints**: Only models of type `CHAT` can be set as default (`EMBEDDING` models cannot).

### `getEnabledModels()` / `getModelsByProvider(...)`
- **Description**: Retrieval methods for listing available models for UI or internal logic.

### `toggleModel(UUID modelId, boolean enabled)`
- **Description**: Quickly enable or disable a model.

## Dependency Graph
- `LlmModelRepository`: DB access.

## Usage Guide
Used primarily by `LlmModelController` for admin management and by `LangChainChatService` to load configurations.

```java
// Get the default model config
LlmModel defaultModel = llmModelService.getDefaultModel()
    .orElseThrow(() -> new IllegalStateException("No default model"));

// Create a new OpenAI model
llmModelService.createModel(new SaveLlmModelRequest(
    "GPT-4", "gpt-4-turbo", "OPENAI", ...
));
```

## Source Link
[LlmModelService.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/llm/LlmModelService.java)
