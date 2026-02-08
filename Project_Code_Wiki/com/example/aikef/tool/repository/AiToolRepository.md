# AiToolRepository

## 1. Class Profile
- **Class Name**: `AiToolRepository`
- **Package**: `com.example.aikef.tool.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `AiTool` entities, with specialized queries for searching and retrieving tool configurations.

## 2. Method Deep Dive
### Query Methods
- `findByName(String name)`: Exact name lookup.
- `findByEnabledTrueOrderBySortOrderAsc()`: Retrieves all active tools, sorted by priority.
- `findEnabledToolsWithSchema()`: Optimized query to fetch tools and their schemas in a single round-trip (avoiding N+1 problems).
- `searchByKeyword(String keyword)`: Fuzzy search across tool name, display name, and description.
- `findByIdWithSchema(UUID id)`: Fetches a single tool with its schema eagerly loaded.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.tool.model.AiTool`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `AiToolService` and `LlmService`.
- **Initialization**: At system startup or cache refresh, `findEnabledToolsWithSchema()` is called to register all available tools with the LLM provider.
- **Runtime**: When an LLM requests "search_orders", `findByName("search_orders")` retrieves the configuration to execute the call.

## 5. Source Link
[AiToolRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/tool/repository/AiToolRepository.java)
