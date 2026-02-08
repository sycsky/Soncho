# SessionGroupMapping

## 1. Class Profile
- **Class Name**: `SessionGroupMapping`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Persistence Entity
- **Purpose**: Represents the mapping of a chat session to a session group by a specific agent. It allows different agents to categorize the same session into different groups.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID, inherited from AuditableEntity).
- `session`: The chat session being categorized (Many-to-One).
- `sessionGroup`: The group the session is assigned to (Many-to-One).
- `agent`: The agent who made this assignment (Many-to-One).

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.ChatSession`: The session entity.
  - `com.example.aikef.model.SessionGroup`: The group entity.
  - `com.example.aikef.model.Agent`: The agent entity.
  - `com.example.aikef.model.base.AuditableEntity`: Base class.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
This entity supports personalized session organization for agents.
- **Personalized Grouping**: Agent A can put Session X in "Urgent" group, while Agent B puts the same Session X in "Follow-up" group.
- **Inbox Organization**: Used to filter and display sessions in the agent's inbox based on their custom groups.

## 5. Source Link
[SessionGroupMapping.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/SessionGroupMapping.java)
