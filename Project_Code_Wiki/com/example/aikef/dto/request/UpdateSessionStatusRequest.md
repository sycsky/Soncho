# UpdateSessionStatusRequest

## 1. Class Profile
- **Class Name**: `UpdateSessionStatusRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: Request payload to change the state of a chat session.

## 2. Method Deep Dive
### Fields
- `sessionId`: The session to update.
- `action`: The target status/action (e.g., `RESOLVE`, `REOPEN`).

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.enums.SessionStatus`

## 4. Usage Guide
Used by `SessionController.updateStatus`.
- **Workflow**: Closing a ticket when solved, or reopening it if the customer replies.

## 5. Source Link
[UpdateSessionStatusRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/UpdateSessionStatusRequest.java)
