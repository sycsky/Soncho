# WorkflowGeneratorController

## 1. Class Profile
- **Class Name**: `WorkflowGeneratorController`
- **Package**: `com.example.aikef.workflow.controller`
- **Type**: `Class`
- **Role**: REST Controller
- **Purpose**: Provides endpoints for generating or modifying workflows based on natural language prompts. It serves as an interface for the AI-assisted workflow generation feature.

## 2. Method Deep Dive
### `generateWorkflow(GenerateWorkflowRequest request)`
- **Functionality**: Generates a new workflow or modifies an existing one based on the user's prompt.
- **Parameters**:
  - `request`: `GenerateWorkflowRequest` object containing the prompt, model ID, and optional existing workflow JSON (nodes and edges).
- **Return Value**: `WorkflowGeneratorService.GeneratedWorkflow` object containing the generated nodes and edges JSON.
- **Usage**:
  ```java
  // Example request body
  {
    "prompt": "Create a customer support workflow that handles refunds",
    "modelId": "uuid-...",
    "existingNodesJson": "...",
    "existingEdgesJson": "..."
  }
  ```

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.workflow.service.WorkflowGeneratorService`: Service that handles the logic for generating workflows using LLMs.
- **External Dependencies**:
  - `org.springframework.web.bind.annotation.*`: Spring Web annotations for REST controllers.
  - `java.util.UUID`: For handling unique identifiers.

## 4. Usage Guide
This controller is used by the frontend to provide an AI-powered workflow creation experience.
1. **User Input**: The user describes the desired workflow in natural language (e.g., "Handle product returns").
2. **Request**: The frontend sends this description to the `/api/v1/workflow-generator/generate` endpoint.
3. **Generation**: The `WorkflowGeneratorService` uses an LLM to generate the corresponding ReactFlow nodes and edges.
4. **Response**: The controller returns the generated JSON, which the frontend renders as a visual workflow graph.

## 5. Source Link
[WorkflowGeneratorController.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/workflow/controller/WorkflowGeneratorController.java)
