# WorkflowEdgeDto

## 1. Class Profile
- **Class Name**: `WorkflowEdgeDto`
- **Package**: `com.example.aikef.workflow.dto`
- **Type**: `Record`
- **Role**: Data Transfer Object (DTO)
- **Purpose**: Represents a connection (edge) between two nodes in the workflow graph, compatible with the ReactFlow library.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier for the edge.
- `source`: ID of the source node.
- `target`: ID of the target node.
- `sourceHandle`: The specific output handle of the source node (e.g., "true", "false" for conditions, or an intent ID).
- `targetHandle`: The specific input handle of the target node.
- `label`: Optional label displayed on the edge.

### Annotations
- `@JsonIgnoreProperties(ignoreUnknown = true)`: Ignores extra fields from ReactFlow (like `selected`, `animated`, `style`) during deserialization, focusing only on the structural data.

## 3. Dependency Graph
- **External Dependencies**:
  - `com.fasterxml.jackson.annotation.JsonIgnoreProperties`: Jackson annotation for JSON processing.

## 4. Usage Guide
This DTO is used to serialize and deserialize the connections between workflow nodes.
- **Workflow Execution**: The backend uses the `source`, `target`, and `sourceHandle` fields to determine the next step in the workflow execution logic.
- **Frontend Interaction**: The frontend sends the full graph structure (nodes and edges) as JSON, which is mapped to this DTO for processing.

## 5. Source Link
[WorkflowEdgeDto.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/workflow/dto/WorkflowEdgeDto.java)
