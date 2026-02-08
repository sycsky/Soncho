# ReadRecord

## 1. Class Profile
- **Class Name**: `ReadRecord`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / State Entity
- **Purpose**: Tracks the "Last Read" timestamp for an agent in a specific chat session. Used to calculate unread message counts.

## 2. Method Deep Dive
### Fields
- `session`: The chat session.
- `agent`: The reader.
- `lastReadTime`: The timestamp of the last message they saw.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.ChatSession`
  - `com.example.aikef.model.Agent`
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
Used by `ReadRecordService`.
- **Logic**: Unread Count = Total Messages in Session (after `lastReadTime`).
- **Update**: When an agent opens a chat window, `lastReadTime` is updated to `now()`.

## 5. Source Link
[ReadRecord.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/ReadRecord.java)
