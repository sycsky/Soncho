# AiRewriteRequest

## 1. Class Profile
- **Class Name**: `AiRewriteRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: Request payload for the AI "Magic Rewrite" feature, which improves the tone or grammar of agent drafts.

## 2. Method Deep Dive
### Fields
- `text`: The original text draft that needs rewriting.

## 3. Dependency Graph
- **Internal Dependencies**: None.

## 4. Usage Guide
Used by `AiController.rewrite`.
- **Agent Tool**: In the chat input box, an agent types a rough draft and clicks "Rewrite". The frontend sends this request, and the AI returns a polished version.

## 5. Source Link
[AiRewriteRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/AiRewriteRequest.java)
