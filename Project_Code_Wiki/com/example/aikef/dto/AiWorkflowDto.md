# AiWorkflowDto

## 1. Class Profile
- **Class Name**: `AiWorkflowDto`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record`
- **Role**: Data Transfer Object
- **Purpose**: Transfers full workflow definitions, including the visual graph (ReactFlow) and execution logic (LiteFlow), between the backend and the workflow editor.

## 2. Method Deep Dive
### Fields
- `id` / `name` / `description`: Basic metadata.
- `nodesJson` / `edgesJson`: Raw JSON for the visual editor.
- `liteflowEl`: Compiled logic string.
- `version`: Version number.
- `enabled` / `isDefault`: Status flags.
- `triggerType` / `triggerConfig`: Activation rules.
- `categoryIds` / `categories`: The session categories this workflow is bound to.

### Inner Records
- `CategoryInfo`: Lightweight representation of a category (ID, name, color, icon).

## 3. Dependency Graph
- **Internal Dependencies**: None directly (uses primitives and UUID).

## 4. Usage Guide
Used by `AiWorkflowController`.
- **Editor Load**: `GET /api/v1/workflows/{id}` returns this DTO so the frontend can reconstruct the graph.

## 5. Source Link
[AiWorkflowDto.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/AiWorkflowDto.java)
