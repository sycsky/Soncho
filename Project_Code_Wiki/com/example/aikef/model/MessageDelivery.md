# MessageDelivery

## 1. Class Profile
- **Class Name**: `MessageDelivery`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / State Entity
- **Purpose**: Tracks the "Sent" status of a message to a specific agent via WebSocket. This is crucial for ensuring reliable delivery in real-time communication.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `message`: The message being delivered.
- `agentId`: The recipient agent.
- `customerId`: (Reserved) The recipient customer.
- `isSent`: Boolean flag indicating if the WebSocket acknowledgment was received.
- `sentAt`: Timestamp of delivery.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Message`: The payload.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
Used by the WebSocket infrastructure (`ChatWebSocketHandler`).
- **Mechanism**: When a message is broadcast, `MessageDelivery` records are created for all online agents in the session with `isSent=false`.
- **Ack**: When the client sends a `ACK` frame, the corresponding record is updated to `isSent=true`.
- **Retry**: On reconnection, the system queries for `isSent=false` records and re-sends them.

## 5. Source Link
[MessageDelivery.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/MessageDelivery.java)
