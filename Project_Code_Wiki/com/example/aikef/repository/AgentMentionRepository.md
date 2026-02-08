# AgentMentionRepository

## 1. Class Profile
- **Class Name**: `AgentMentionRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `AgentMention` entities.

## 2. Method Deep Dive
### Query Methods
- `findByAgent_IdAndReadFalse...`: Get unread notifications for an agent.
- `countByAgent_IdAndReadFalse(...)`: Notification badge count.
- `markAllAsReadByAgentId(...)`: "Mark All Read" button.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.AgentMention`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `AgentMentionController`.
- **API**: `GET /api/v1/mentions/unread` calls `findByAgent_IdAndReadFalse...` to populate the notification dropdown.

## 5. Source Link
[AgentMentionRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/AgentMentionRepository.java)
