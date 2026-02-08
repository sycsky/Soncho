# LlmModelRepository

## 1. Class Profile
- **Class Name**: `LlmModelRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `LlmModel` entities.

## 2. Method Deep Dive
### Query Methods
- `findByCode(String code)`: Lookup by unique code.
- `findByEnabledTrueOrderBySortOrderAsc()`: Get all available models for the UI dropdown.
- `findByProviderAndEnabledTrue...`: Filter by provider (e.g., "Show only OpenAI models").
- `findByIsDefaultTrueAndEnabledTrue()`: Get the system default model.
- `findFirstByStatusExplanationTrue...`: Specialized lookup for status explanation tasks.
- `findByModelTypeAndIsDefaultTrue...`: Get default Chat vs. Embedding model.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.LlmModel`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `LlmFactory` to instantiate the correct LangChain4j model object.
- **Factory Pattern**: `LlmFactory.createChatModel(modelCode)` calls `findByCode(modelCode)` to get the config, then builds the `ChatLanguageModel` instance.

## 5. Source Link
[LlmModelRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/LlmModelRepository.java)
