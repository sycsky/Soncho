# AiKnowledgeService

## Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: Class
- **Description**: Service for AI-driven knowledge extraction and processing, such as summarization, rewriting, and tag suggestion.
- **Key Features**:
  - Session Summarization.
  - Message Rewriting.
  - Auto-tagging suggestions.

## Method Deep Dive

### `summarize`
- **Description**: Generates a summary of a chat session.
- **Signature**: `public AiSummaryResponse summarize(String sessionId)`
- **Logic**:
  1. Retrieves all messages for the session.
  2. Concatenates timestamp and text.
  3. (Currently) Returns simple concatenation; likely a placeholder for LLM summarization.

### `rewrite`
- **Description**: Rewrites a message text to be more polite or professional.
- **Signature**: `public AiRewriteResponse rewrite(String text)`
- **Logic**: Prepends "建议回复：" to the text. Placeholder for LLM rewriting.

### `suggestTags`
- **Description**: Suggests tags for a session based on content.
- **Signature**: `public AiSuggestTagsResponse suggestTags(String sessionId)`
- **Logic**:
  1. Aggregates all message text.
  2. Uses `TagHeuristics` to find keywords (e.g., "refund" -> "售后").
  3. Returns set of tags.

## Dependency Graph
- **Injected Services**:
  - `MessageRepository`: Data access for messages.
- **DTOs**:
  - `AiSummaryResponse`, `AiRewriteResponse`, `AiSuggestTagsResponse`

## Usage Guide
```java
AiSummaryResponse summary = aiKnowledgeService.summarize("session-uuid");
```
