# Class Profile: LlmModelController

**File Path**: `com/example/aikef/controller/LlmModelController.java`
**Type**: Controller (`@RestController`)
**Purpose**: Manages configuration for Large Language Models (LLMs). It allows administrators to add, update, and test connections to various LLM providers (OpenAI, Azure, Anthropic, etc.), and set default models for the system.

# Method Deep Dive

## Model Management
- **`getAllModels()`, `getEnabledModels()`**: Lists configured models.
- **`createModel(...)`, `updateModel(...)`**: Configures connection details (API key, base URL, model name).
- **`toggleModel(...)`**: Enables or disables a model.
- **`setDefaultModel(...)`**: Marks a specific model as the system default.

## Testing & Diagnostics
- **`testModel(modelId)`**: Performs a live connectivity test by sending a simple "Hello" prompt to the LLM provider.
- **`getProviders()`**: Returns a list of supported LLM providers (e.g., OpenAI, Ollama).

## Cache
- **`clearCache()`**: Clears internal model instance caches (LangChain objects).

# Dependency Graph

**Core Dependencies**:
- `LlmModelService`: CRUD logic for model configurations.
- `LangChainChatService`: Used for testing connectivity and clearing caches.
- `LlmModelDto`: Data transfer object.

**Key Imports**:
```java
import com.example.aikef.llm.LlmModelService;
import com.example.aikef.llm.LangChainChatService;
import org.springframework.web.bind.annotation.*;
```

# Usage Guide

## Adding an OpenAI Model
`POST /api/v1/llm-models`
```json
{
  "name": "GPT-4 Turbo",
  "provider": "OPEN_AI",
  "modelName": "gpt-4-turbo",
  "apiKey": "sk-...",
  "enabled": true
}
```

## Testing a Model
`POST /api/v1/llm-models/{id}/test`
Returns:
```json
{
  "success": true,
  "response": "测试成功",
  "durationMs": 1200
}
```

# Source Link
[LlmModelController.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/controller/LlmModelController.java)
