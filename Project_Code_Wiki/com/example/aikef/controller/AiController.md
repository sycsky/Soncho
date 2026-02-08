# AiController

## Class Profile
- **Package**: `com.example.aikef.controller`
- **Type**: Class
- **Description**: Controller for AI-powered features such as text summarization, rewriting, and tag suggestion.
- **Key Features**:
  - Summarize session.
  - Rewrite message.
  - Suggest tags.
  - Test endpoints (mock data).

## Method Deep Dive

### `summary`
- **Description**: Generates a summary for a chat session.
- **Signature**: `public AiSummaryResponse summary(AiSummaryRequest request)`
- **Logic**: Delegates to `aiKnowledgeService.summarize`.

### `rewrite`
- **Description**: Rewrites input text (e.g., for politeness or tone).
- **Signature**: `public AiRewriteResponse rewrite(AiRewriteRequest request)`
- **Logic**: Delegates to `aiKnowledgeService.rewrite`.

### `suggestTags`
- **Description**: Suggests categorization tags for a session.
- **Signature**: `public AiSuggestTagsResponse suggestTags(AiSuggestTagsRequest request)`
- **Logic**: Delegates to `aiKnowledgeService.suggestTags`.

## Dependency Graph
- **Injected Services**:
  - `AiKnowledgeService`
- **DTOs**:
  - `AiSummaryRequest`, `AiSummaryResponse`
  - `AiRewriteRequest`, `AiRewriteResponse`
  - `AiSuggestTagsRequest`, `AiSuggestTagsResponse`

## Usage Guide
```bash
POST /api/v1/ai/summary
Content-Type: application/json
{ "sessionId": "..." }
```
