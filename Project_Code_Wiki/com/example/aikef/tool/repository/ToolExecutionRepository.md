# ToolExecutionRepository

## 1. Class Profile
- **Class Name**: `ToolExecutionRepository`
- **Package**: `com.example.aikef.tool.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access for `ToolExecution` logs, enabling analytics and history tracking.

## 2. Method Deep Dive
### Query Methods
- `findByTool_Id(UUID toolId, Pageable pageable)`: Paginates execution history for a specific tool.
- `findBySessionId(UUID sessionId)`: Retrieves all tool calls made within a specific chat session.
- `findRecentByToolId(UUID toolId, Instant since)`: Gets recent executions (e.g., for health checking or rate limiting).
- `countByToolIdAndStatus(...)`: Statistics for success/failure rates.
- `getAverageDurationByToolId(UUID toolId)`: Performance metric calculation.

### Modification Methods
- `deleteByTool_Id(UUID toolId)`: Cleanup method (cascading delete is usually handled by DB, but this exists for explicit cleanup).

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.tool.model.ToolExecution`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `ToolExecutionService` and dashboard/analytics controllers.
- **Dashboard**: "Show me the failure rate of the 'Shipping API' tool over the last 24 hours."
- **Debugging**: "List all tool executions for Session X that failed."

## 5. Source Link
[ToolExecutionRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/tool/repository/ToolExecutionRepository.java)
