# CreateQuickReplyRequest

## 1. Class Profile
- **Class Name**: `CreateQuickReplyRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: Request payload to create a new predefined "canned" response for agents.

## 2. Method Deep Dive
### Fields
- `label`: Short title shown in the list.
- `text`: Full message content inserted into the chat.
- `category`: Grouping tag (e.g., "Greeting", "Refund").
- `system`: If true, it's a global reply visible to all agents.

## 3. Dependency Graph
- **Internal Dependencies**: None.

## 4. Usage Guide
Used by `QuickReplyController.create`.
- **Efficiency**: Agents can create personal quick replies for common questions they handle.

## 5. Source Link
[CreateQuickReplyRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/CreateQuickReplyRequest.java)
