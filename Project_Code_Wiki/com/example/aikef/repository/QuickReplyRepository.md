# QuickReplyRepository

## 1. Class Profile
- **Class Name**: `QuickReplyRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `QuickReply` entities.

## 2. Method Deep Dive
### Query Methods
- `findBySystemTrue()`: Retrieves all global system replies.
- (Standard JpaRepository methods inherited).

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.QuickReply`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `QuickReplyService`.
- **Listing**: When an agent loads the chat interface, the service fetches `findBySystemTrue()` combined with `findByCreatedBy(currentAgent)` to show the full list of available shortcuts.

## 5. Source Link
[QuickReplyRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/QuickReplyRepository.java)
