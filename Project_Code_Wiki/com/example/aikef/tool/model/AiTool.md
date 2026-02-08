# AiTool

## 1. Class Profile
- **Class Name**: `AiTool`
- **Package**: `com.example.aikef.tool.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Configuration Entity
- **Purpose**: Defines an AI tool available for use by the system. It supports both standard HTTP APIs and MCP (Model Context Protocol) services.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `tenantId`: Support for multi-tenancy (system tools have null tenantId).
- `name`: Unique machine-readable name for AI invocation.
- `displayName`: Human-readable name for UI.
- `description`: Detailed description used by the LLM to understand when to use this tool.
- `toolType`: Enum (`API`, `MCP`).
- `schema`: One-to-one relationship with `ExtractionSchema`, defining the parameters this tool accepts.
- `enabled`: Toggle for availability.

### API Configuration
- `apiMethod`: HTTP method (GET, POST, etc.).
- `apiUrl`: Target endpoint URL.
- `apiHeaders`: JSON string of required headers.
- `apiBodyTemplate`: Template string for the request body.
- `apiResponsePath`: JSONPath to extract the relevant result from the response.

### MCP Configuration
- `mcpEndpoint`: Connection point for the MCP server.
- `mcpToolName`: Name of the specific tool within the MCP server.
- `mcpServerType`: Transport type (`stdio`, `sse`, `websocket`).

### Auth & Metadata
- `authType`: Authentication method (`NONE`, `API_KEY`, `BEARER`, etc.).
- `authConfig`: Encrypted credentials or config.
- `inputExample` / `outputExample`: Few-shot examples to improve LLM performance.
- `requireConfirmation`: Human-in-the-loop safety toggle.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.extraction.model.ExtractionSchema`: Defines the parameter structure.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.
  - `org.hibernate.annotations.Filter`: Multi-tenancy support.

## 4. Usage Guide
This entity is the central configuration point for extending the agent's capabilities.
- **Registration**: Admins create `AiTool` records to add new capabilities (e.g., "Check Order Status").
- **Discovery**: The `ToolRegistry` loads enabled `AiTool`s and converts them into LangChain4j `ToolSpecification` objects for the LLM.
- **Execution**: When the LLM calls a tool, the `AiToolService` looks up this record to determine how to execute the request (call an API or forward to an MCP server).

## 5. Source Link
[AiTool.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/tool/model/AiTool.java)
