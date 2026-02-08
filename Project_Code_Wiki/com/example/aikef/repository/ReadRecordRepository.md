# ReadRecordRepository

## 1. Class Profile
- **Class Name**: `ReadRecordRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `ReadRecord` entities.

## 2. Method Deep Dive
### Query Methods
- `findBySessionIdAndAgentId(...)`: Get the specific record for a session/agent pair.
- `findByAgentId(...)`: Get all records for an agent (for calculating badge counts across all sessions).
- `findByAgentIdAndSessionIdIn(...)`: Batch retrieval for list views.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.ReadRecord`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `ReadRecordController`.
- **Badge Count**: `findByAgentId` -> Iterate and sum up `countBySession_IdAndCreatedAtAfter(session.id, record.lastReadTime)`.

## 5. Source Link
[ReadRecordRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/ReadRecordRepository.java)
