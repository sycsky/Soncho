# LlmModel

## 1. Class Profile
- **Class Name**: `LlmModel`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Configuration Entity
- **Purpose**: Defines the configuration for an external Large Language Model provider (e.g., OpenAI, Azure, Ollama). It abstracts the connection details so the system can switch models dynamically.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `name`: Display name (e.g., "GPT-4 Turbo").
- `code`: Unique system code (e.g., `gpt-4-turbo`).
- `provider`: Service provider (`OPENAI`, `AZURE_OPENAI`, `OLLAMA`, etc.).
- `modelType`: `CHAT` or `EMBEDDING`.
- `modelName`: The actual API model string (e.g., `gpt-4-1106-preview`).
- `baseUrl`: Custom API endpoint (for local LLMs or proxies).
- `apiKey`: Encrypted API key.
- `azureDeploymentName`: Azure-specific config.
- `defaultTemperature` / `defaultMaxTokens`: Hyperparameter defaults.
- `contextWindow`: Maximum token context length.
- `inputPricePer1k` / `outputPricePer1k`: Cost tracking.
- `supportsFunctions` / `supportsVision`: Capability flags.
- `statusExplanation`: Flag indicating if this model is optimized for generating status explanations.

## 3. Dependency Graph
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
This entity allows the admin to manage AI resources.
- **Switching**: If GPT-4 is down, an admin can disable it and set Claude 3 as `isDefault`.
- **Optimization**: Workflows can select specific models (e.g., using a cheap model for categorization and an expensive one for drafting replies).

## 5. Source Link
[LlmModel.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/LlmModel.java)
