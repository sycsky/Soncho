# MessageDeliveryRepository

## 1. Class Profile
- **Class Name**: `MessageDeliveryRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `MessageDelivery` entities.

## 2. Method Deep Dive
### Query Methods
- `findUnsentForAgent(...)`: Retrieves pending messages for a specific agent (for sync on reconnect).
- `markAsSent(...)`: Bulk updates status to true (optimization for batch ACKs).

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.MessageDelivery`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `OfflineMessageService`.
- **Sync**: When an agent logs in, `findUnsentForAgent` is called to push everything they missed while offline or disconnected.

## 5. Source Link
[MessageDeliveryRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/MessageDeliveryRepository.java)
