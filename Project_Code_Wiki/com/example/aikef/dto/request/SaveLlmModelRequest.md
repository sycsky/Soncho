# SaveLlmModelRequest

## 1. Class Profile
- **Class Name**: `SaveLlmModelRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: Comprehensive payload for creating or updating an LLM provider configuration.

## 2. Method Deep Dive
### Fields
- `name`: Display name.
- `code`: System identifier.
- `provider`: Provider type (OpenAI, Azure, etc.).
- `modelName`: The actual model string (e.g., "gpt-4").
- `modelType`: CHAT or EMBEDDING.
- `baseUrl`, `apiKey`: Connection details.
- `isDefault`: Marks this as the system-wide default.
- `supportsFunctions`, `supportsVision`: Capabilities.
- `inputPricePer1k`, `outputPricePer1k`: Cost tracking.

## 3. Dependency Graph
- **Internal Dependencies**: None.

## 4. Usage Guide
Used by `LlmModelController`.
- **Admin**: Allows admins to configure which AI models are available to the system without restarting the server.

## 5. Source Link
[SaveLlmModelRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/SaveLlmModelRequest.java)
