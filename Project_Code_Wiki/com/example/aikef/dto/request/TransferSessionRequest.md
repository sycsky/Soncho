# TransferSessionRequest

## 1. Class Profile
- **Class Name**: `TransferSessionRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: Request payload to transfer ownership of a chat session to another agent.

## 2. Method Deep Dive
### Fields
- `targetAgentId`: The recipient agent.
- `keepAsSupport`: If true, the current agent remains in the chat as a secondary participant (watcher).

## 3. Dependency Graph
- **Internal Dependencies**: None.

## 4. Usage Guide
Used by `ChatSessionService.transfer`.
- **Escalation**: Tier 1 support transfers a difficult ticket to a Tier 2 specialist.

## 5. Source Link
[TransferSessionRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/TransferSessionRequest.java)
