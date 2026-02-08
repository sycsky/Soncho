# AiSummaryRequest

## 1. Class Profile
- **Class Name**: `AiSummaryRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: Request payload to generate an AI summary of a chat session.

## 2. Method Deep Dive
### Fields
- `sessionId`: The ID of the session to summarize.

## 3. Dependency Graph
- **Internal Dependencies**: None.

## 4. Usage Guide
Used by `AiController.summarize`.
- **Session Handover**: When transferring a chat, the system can generate a summary so the new agent catches up quickly.
- **Resolution**: Used to create a final note when closing a ticket.

## 5. Source Link
[AiSummaryRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/AiSummaryRequest.java)
