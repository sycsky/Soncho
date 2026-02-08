# SessionGroupMappingRepository

## 1. Class Profile
- **Class Name**: `SessionGroupMappingRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `SessionGroupMapping` entities, facilitating the management of session-group-agent relationships.

## 2. Method Deep Dive
### Query Methods
- `findBySession(ChatSession session)`: Finds all mappings for a specific session regardless of agent.
- `findBySessionAndAgent(ChatSession session, Agent agent)`: Finds the specific mapping for a session by a specific agent.
- `findBySessionIdAndAgentId(UUID sessionId, UUID agentId)`: Optimized ID-based lookup for session-agent mapping.
- `findBySessionGroup(SessionGroup sessionGroup)`: Finds all mappings within a specific group.
- `findBySessionGroupIdAndAgentId(UUID groupId, UUID agentId)`: Finds mappings for a specific group and agent.
- `findByAgent(Agent agent)`: Finds all mappings created by a specific agent.
- `deleteBySessionAndAgent(ChatSession session, Agent agent)`: Removes a specific mapping.
- `deleteBySession(ChatSession session)`: Removes all mappings for a session (e.g., when session is deleted).

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.SessionGroupMapping`: The entity.
  - `com.example.aikef.model.ChatSession`: Parameter type.
  - `com.example.aikef.model.Agent`: Parameter type.
  - `com.example.aikef.model.SessionGroup`: Parameter type.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `SessionGroupService` to manage personal session lists.
- **Adding to Group**: When an agent moves a session to a group, `save()` is called.
- **Filtering**: Used to fetch "My Sessions" in a specific custom group.
- **Cleanup**: Used to remove mappings when sessions are closed or deleted.

## 5. Source Link
[SessionGroupMappingRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/SessionGroupMappingRepository.java)
