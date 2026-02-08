# AgentRepository

## 1. Class Profile
- **Class Name**: `AgentRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `Agent` entities.

## 2. Method Deep Dive
### Query Methods
- `findByEmail(String email)`: Finds an agent by their exact email address.
- `findByEmailIgnoreCase(String email)`: Case-insensitive email lookup.
- `findByStatus(AgentStatus status)`: Finds all agents with a specific status (e.g., all `ONLINE` agents).
- `findByIdWithRole(UUID id)`: Optimized query to fetch an agent and eagerly load their role to prevent N+1 queries.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Agent`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `AgentService` and authentication providers.
- **Login**: `findByEmail` is called during the login process.
- **Routing**: `findByStatus(AgentStatus.ONLINE)` is used to get a pool of candidates for new chat assignment.

## 5. Source Link
[AgentRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/AgentRepository.java)
