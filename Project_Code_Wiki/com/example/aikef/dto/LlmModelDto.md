# LlmModelDto

## 1. Class Profile
- **Class Name**: `LlmModelDto`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record`
- **Role**: Data Transfer Object
- **Purpose**: Transfers LLM configuration to the frontend (excluding sensitive keys).

## 2. Method Deep Dive
### Fields
- `id` / `name` / `code`: Identity.
- `provider`: `OPENAI`, `OLLAMA`, etc.
- `defaultTemperature` / `defaultMaxTokens`: Settings.
- `inputPricePer1k` / `outputPricePer1k`: Cost info.
- `supportsFunctions` / `supportsVision`: Capability flags.
- `enabled`: Status.

## 3. Dependency Graph
- **Internal Dependencies**: None.

## 4. Usage Guide
Used by `LlmModelController`.
- **List**: `GET /api/v1/llm-models` returns available models for the user to choose from in the workflow editor.

## 5. Source Link
[LlmModelDto.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/LlmModelDto.java)
