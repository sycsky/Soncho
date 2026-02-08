# WorkflowNodeDto

## 1. Class Profile
- **Class Name**: `WorkflowNodeDto`
- **Package**: `com.example.aikef.workflow.dto`
- **Type**: `Record`
- **Role**: Data Transfer Object (DTO)
- **Purpose**: Represents a node in the workflow graph, compatible with the ReactFlow library. It contains the node's type, position, and configuration data.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier for the node.
- `type`: The type of the node (e.g., `start`, `llm`, `condition`, `reply`, `api`, `knowledge`, `intent`, `human_transfer`, `delay`, `end`).
- `data`: `NodeData` record containing the node's label and configuration.
- `position`: `Position` record containing the node's x and y coordinates.

### Inner Records
- `NodeData`:
  - `label`: Display name of the node.
  - `config`: `JsonNode` containing type-specific configuration (e.g., LLM prompt, API URL, condition logic).
- `Position`:
  - `x`: X-coordinate.
  - `y`: Y-coordinate.

### Annotations
- `@JsonIgnoreProperties(ignoreUnknown = true)`: Ignores extra fields from ReactFlow (like `measured`, `selected`, `dragging`) to keep the backend model clean.

## 3. Dependency Graph
- **External Dependencies**:
  - `com.fasterxml.jackson.annotation.JsonIgnoreProperties`: Jackson annotation.
  - `com.fasterxml.jackson.databind.JsonNode`: Jackson class for handling arbitrary JSON structures.

## 4. Usage Guide
This DTO is the core building block of the workflow definition.
- **Workflow Storage**: Workflows are stored as JSON strings in the database, which are deserialized into lists of `WorkflowNodeDto` during execution.
- **Execution Engine**: The workflow engine iterates through these nodes, executing logic based on the `type` and `data.config` of each node.

## 5. Source Link
[WorkflowNodeDto.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/workflow/dto/WorkflowNodeDto.java)
