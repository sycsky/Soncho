# ExecuteWorkflowRequest

## 1. Class Profile
- **Class Name**: `ExecuteWorkflowRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record`
- **Role**: Data Transfer Object (Request)
- **Purpose**: Triggers a manual execution of a workflow, usually for testing or debugging.

## 2. Method Deep Dive
### Fields
- `sessionId`: (Optional) If provided, the workflow runs in the context of this chat.
- `userMessage`: The input text to feed into the workflow.
- `variables`: Map of initial variables (e.g., `{"orderId": "123"}`).

## 3. Dependency Graph
- **External Dependencies**:
  - `jakarta.validation.constraints.NotBlank`

## 4. Usage Guide
Used by `AiWorkflowController`.
- **Endpoint**: `POST /api/v1/workflows/execute`
- **Scenario**: An admin wants to test if the "Refund" workflow handles the input "I want my money back" correctly without actually sending a message as a user.

## 5. Source Link
[ExecuteWorkflowRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/ExecuteWorkflowRequest.java)
