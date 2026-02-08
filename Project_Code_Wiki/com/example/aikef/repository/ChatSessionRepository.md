# ChatSessionRepository

## 1. Class Profile
- **Class Name**: `ChatSessionRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `ChatSession` entities.

## 2. Method Deep Dive
### Query Methods
- `findByPrimaryAgent_Id(UUID agentId)`: Finds sessions assigned to a specific agent.
- `findByStatus(SessionStatus status)`: Finds sessions by status (e.g., waiting in queue).
- `findByCustomer_Id(UUID customerId)`: Finds history for a customer.
- `findByPrimaryAgentIdOrSupportAgentIdsContaining(...)`: Finds all sessions relevant to an agent (whether primary or supporting).
- `findFirstByCustomer_IdOrderByLastActiveAtDesc(...)`: Finds the most recent session for a customer (used for threading new messages).

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.ChatSession`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `ChatSessionService` and `MessageService`.
- **Threading**: When a webhook receives a message, `findFirstByCustomer_IdOrderByLastActiveAtDesc` determines if it belongs to an existing active conversation or starts a new one.

## 5. Source Link
[ChatSessionRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/ChatSessionRepository.java)
