# SaveWorkflowRequest

## 1. Class Profile
- **Class Name**: `SaveWorkflowRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record`
- **Role**: Data Transfer Object (Request)
- **Purpose**: Payload for creating or updating an AI workflow definition.

## 2. Method Deep Dive
### Fields
- `name`: Required.
- `description`: Optional.
- `nodesJson` / `edgesJson`: ReactFlow graph data (JSON strings).
- `triggerType`: `ALL`, `CATEGORY`, `KEYWORD`.
- `triggerConfig`: JSON config for the trigger.
- `categoryIds`: List of session categories to bind this workflow to (Mutual exclusion logic is handled by the service).

## 3. Dependency Graph
- **External Dependencies**:
  - `jakarta.validation.constraints.NotBlank`

## 4. Usage Guide
Used by `AiWorkflowController`.
- **Endpoint**: `POST /api/v1/workflows` or `PUT /api/v1/workflows/{id}`.

## 5. Source Link
[SaveWorkflowRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/SaveWorkflowRequest.java)
