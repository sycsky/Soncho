# AiToolController

## Class Profile
- **Package**: `com.example.aikef.controller`
- **Type**: Class
- **Description**: Controller for managing AI Tools (Plugins). Provides APIs for CRUD operations on tools, tool execution, statistics, and retrieval of tool definitions for LLM consumption.
- **Key Features**:
  - Tool CRUD (List, Get, Create, Update, Delete)
  - Tool Execution (Execute by ID or Name, Test)
  - Execution History & Stats
  - Tool Definition Generation (OpenAI format, Standard format)
  - Support for complex parameter definitions (Nested objects, Arrays)

## Method Deep Dive

### `getTools`
- **Description**: Retrieves all enabled tools, optionally filtered by keyword.
- **Signature**: `public List<ToolDto> getTools(String keyword)`
- **Logic**: Delegates search or retrieval to `toolService`.

### `createTool`
- **Description**: Creates a new AI tool definition.
- **Signature**: `public ToolDto createTool(CreateToolDto request, Authentication authentication)`
- **Logic**:
  1. Extracts current agent ID from authentication.
  2. Converts nested `ParameterDto` structures to `ParameterDefinition`.
  3. Calls `toolService.createTool` with a `CreateToolRequest`.
  4. Returns the created tool as DTO.

### `createOrReplaceToolByName`
- **Description**: Idempotent creation/replacement of a tool by name. Useful for syncing tools from code/config.
- **Signature**: `public ToolDto createOrReplaceToolByName(CreateToolDto request, Authentication authentication)`
- **Logic**:
  1. Calls `toolService.createOrReplaceToolByName`.
  2. If tool exists, it is replaced; otherwise created.

### `executeTool`
- **Description**: Executes a specific tool by ID.
- **Signature**: `public ToolExecutionResult executeTool(UUID toolId, ExecuteToolDto request, Authentication authentication)`
- **Logic**:
  1. Captures `executedBy` agent ID.
  2. Delegates to `toolService.executeTool`.

### `getOpenAiFunctions`
- **Description**: Returns tool definitions formatted for OpenAI Function Calling.
- **Signature**: `public String getOpenAiFunctions()`
- **Logic**: Delegates to `toolService.generateOpenAiFunctions()`.

### `testTool`
- **Description**: Tests a tool execution without persisting the execution log (usually).
- **Signature**: `public ToolExecutionResult testTool(UUID toolId, ExecuteToolDto request)`
- **Logic**: Calls `toolService.executeTool` with `null` sessionId and agentId, implying a test context.

## Dependency Graph
- **Injected Services**:
  - `AiToolService`: Core service for tool management and execution.
- **DTOs**:
  - `ToolDto`, `CreateToolDto`, `UpdateToolDto`
  - `ExecuteToolDto`, `ExecutionDto`, `ToolStats`
  - `ParameterDto` (Recursive definition)

## Usage Guide
```java
// Example: Executing a tool via API
POST /api/v1/tools/{toolId}/execute
Content-Type: application/json

{
  "params": {
    "orderId": "12345"
  },
  "sessionId": "..."
}
```
