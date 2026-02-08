# AiWorkflow

## 1. Class Profile
- **Class Name**: `AiWorkflow`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Core Logic Entity
- **Purpose**: Stores the definition of an AI workflow, including its visual structure (ReactFlow nodes/edges) and its executable structure (LiteFlow EL).

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `name` / `description`: Metadata.
- `nodesJson` / `edgesJson`: The raw JSON from the ReactFlow frontend editor.
- `liteflowEl`: The compiled Expression Language string used by the LiteFlow engine to execute the logic.
- `subChainsJson`: Metadata about how complex LLM nodes are broken down into sub-chains (crucial for tool call suspension/resumption).
- `isTemplate`: Whether this workflow can be instantiated by others.
- `llmNodeIds`: List of nodes that involve LLM calls.
- `version`: Version control.
- `enabled`: Activation toggle.
- `isDefault`: Fallback workflow flag.
- `triggerType`: How this workflow is activated (`ALL`, `CATEGORY`, `KEYWORD`).
- `triggerConfig`: Specific rules for the trigger type.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Agent`: The creator.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
This is the heart of the system's configurability.
- **Design**: Users drag and drop nodes in the UI. This saves `nodesJson` and `edgesJson`.
- **Compilation**: The backend `WorkflowCompiler` converts the visual graph into `liteflowEl` (e.g., `THEN(start, SWITCH(intent).to(sales, support))`).
- **Execution**: When a message arrives, the `WorkflowEngine` loads this entity and executes the `liteflowEl`.

## 5. Source Link
[AiWorkflow.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/AiWorkflow.java)
