# Event

## 1. Class Profile
- **Class Name**: `Event`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Integration Entity
- **Purpose**: Defines an external event hook that can trigger an AI workflow. For example, "Order Created" or "Payment Failed".

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `name`: The event key (e.g., `order.created`).
- `displayName`: Human-readable name.
- `description`: Documentation.
- `workflow`: The `AiWorkflow` to execute when this event fires.
- `enabled`: Toggle.
- `sortOrder`: Display order.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.AiWorkflow`: The logic to run.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
Used by `EventController` and `EventService`.
- **Webhook**: An external system POSTs to `/api/v1/events/hook/{name}` with a payload. The system looks up the `Event`, finds the `workflow`, and executes it with the payload as variables.

## 5. Source Link
[Event.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/Event.java)
