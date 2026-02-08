# MessageRepository

## 1. Class Profile
- **Class Name**: `MessageRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `Message` entities, with extensive support for pagination, filtering, and statistics.

## 2. Method Deep Dive
### Query Methods
- `findBySession_IdOrderByCreatedAtAsc(...)`: Standard chat history retrieval.
- `findBySession_IdOrderByCreatedAtDesc(...)`: Retrieval for "Load Previous" functionality.
- `findBySessionAndReadByCustomerFalse...`: Finding unread messages for read receipts.
- `countBySession...`: Unread badge counters.
- `findFirstBySession_IdOrderByCreatedAtDesc(...)`: Getting the "last message" for the session list preview.
- `findBySession_IdAndInternalFalse...`: Fetching only public messages (hiding internal notes/thoughts) for the customer view.
- `findBySession_IdAndInternalFalseAndCreatedAtLessThanEqualOrderByCreatedAtDesc(...)`: Complex query for loading history context for the AI, respecting the "trigger message" timestamp to avoid causal loops.

### Statistics
- `countByCreatedAtBetweenAndTenantId(...)`: Volume analytics.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Message`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `MessageService`, `ChatController`, and `HistoryMessageLoader`.
- **AI Context**: `HistoryMessageLoader` uses the complex `findBySession_Id...LessThanEqual...` query to reconstruct the exact state of the conversation at the moment a tool was called, ensuring the LLM doesn't see "future" messages.

## 5. Source Link
[MessageRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/MessageRepository.java)
