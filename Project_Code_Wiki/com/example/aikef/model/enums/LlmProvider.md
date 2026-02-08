# LlmProvider

## 1. Class Profile
- **Class Name**: `LlmProvider`
- **Package**: `com.example.aikef.model.enums`
- **Type**: `Enum`
- **Role**: Enumeration
- **Purpose**: Lists supported AI model providers and their default API endpoints.

## 2. Method Deep Dive
### Values
- `OPENAI`: Standard OpenAI API.
- `AZURE_OPENAI`: Microsoft Azure.
- `OLLAMA`: Local inference.
- `ZHIPU` / `DASHSCOPE` / `MOONSHOT` / `DEEPSEEK`: Chinese providers.
- `CUSTOM`: User-defined endpoint.

### Methods
- `getDefaultBaseUrl()`: Returns the standard API URL for that provider.

## 3. Usage Guide
Used in `LlmModel` entity configuration.

## 4. Source Link
[LlmProvider.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/enums/LlmProvider.java)
