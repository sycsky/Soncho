# AiToolService

## Class Profile
- **Package**: `com.example.aikef.tool.service`
- **Type**: Class
- **Description**: Comprehensive service for managing and executing AI Tools (Plugins). Supports API tools, Internal tools, and future MCP tools.
- **Key Features**:
  - Tool CRUD & Schema Management.
  - Tool Execution (API, Internal).
  - Parameter Definition & Validation.
  - OpenAI Function Definition Generation.
  - Execution History Logging.

## Method Deep Dive

### `executeTool`
- **Description**: Executes a tool by ID.
- **Signature**: `public ToolExecutionResult executeTool(UUID toolId, Map<String, Object> params, WorkflowContext ctx, UUID executedBy)`
- **Logic**:
  1. Validates tool enabled status.
  2. Creates `ToolExecution` record (Status: RUNNING).
  3. Dispatches based on `ToolType`:
     - **API**: Calls `executeApiTool`.
     - **INTERNAL**: Calls `executeInternalTool` via `InternalToolRegistry`.
     - **MCP**: Placeholder.
  4. Updates execution record with result (SUCCESS/FAILED) and duration.
  5. Returns result.

### `executeApiTool`
- **Description**: Executes an external API call.
- **Logic**:
  1. Handles Retry logic (Network exceptions).
  2. Renders templates for URL, Headers, and Body using `TemplateEngine` and `WorkflowContext`.
  3. Applies Authentication (Bearer, Basic, API Key).
  4. Sends HTTP request via `RestTemplate`.
  5. Extracts result using JSONPath if configured.

### `createTool`
- **Description**: Creates a new tool definition.
- **Logic**:
  1. Saves tool entity.
  2. Creates associated `ExtractionSchema` for parameters (supports nested objects/arrays).

### `generateOpenAiFunctions`
- **Description**: Converts all enabled tools into OpenAI's JSON schema format for function calling.
- **Logic**: Iterates tools, maps `ExtractionSchema` to JSON schema properties.

## Dependency Graph
- **Injected Services**:
  - `AiToolRepository`, `ToolExecutionRepository`, `ExtractionSchemaRepository`
  - `RestTemplate`: For API calls.
  - `InternalToolRegistry`: For internal tool dispatch.
  - `ChatSessionService`: For context loading.
- **DTOs**:
  - `ToolExecutionResult`, `CreateToolRequest`
  - `ToolDefinition`

## Usage Guide
```java
// Execute a tool programmatically
ToolExecutionResult result = aiToolService.executeTool(
    toolId, 
    Map.of("city", "Beijing"), 
    workflowContext, 
    agentId
);
```
