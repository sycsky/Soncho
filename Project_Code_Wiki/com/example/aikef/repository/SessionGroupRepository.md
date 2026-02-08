# SessionGroupRepository

## 1. Class Profile
- **Class Name**: `SessionGroupRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `SessionGroup` entities.

## 2. Method Deep Dive
### Query Methods
- `findByAgentOrderBySortOrderAsc(Agent agent)`: Fetch all groups for a specific agent.
- `findByAgentAndSystemTrue(Agent agent)`: Fetch system default groups.
- `findByAgentAndName(...)`: Lookup specific group.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.SessionGroup`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `SessionGroupService`.
- **Initialization**: When a new agent is created, the service calls `save()` to create default groups like "Starred" or "Archive" for them.

## 5. Source Link
[SessionGroupRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/SessionGroupRepository.java)
