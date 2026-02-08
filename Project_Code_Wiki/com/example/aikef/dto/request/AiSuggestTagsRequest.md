# AiSuggestTagsRequest

## 1. Class Profile
- **Class Name**: `AiSuggestTagsRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: Request payload to trigger AI analysis of a session for tag suggestions.

## 2. Method Deep Dive
### Fields
- `sessionId`: The ID of the session to analyze.

## 3. Dependency Graph
- **Internal Dependencies**: None.

## 4. Usage Guide
Used by `AiController.suggestTags`.
- **Auto-Tagging**: An agent can click "Suggest Tags" in the sidebar. The backend retrieves the session history, sends it to the LLM, and returns relevant tags.

## 5. Source Link
[AiSuggestTagsRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/AiSuggestTagsRequest.java)
