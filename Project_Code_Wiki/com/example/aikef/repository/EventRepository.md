# EventRepository

## 1. Class Profile
- **Class Name**: `EventRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `Event` entities.

## 2. Method Deep Dive
### Query Methods
- `findByName(String name)`: Lookup by event key.
- `findByEnabledTrueOrderBySortOrder()`: List active events.
- `findByWorkflow_Id(UUID workflowId)`: Find all events bound to a specific workflow.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Event`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `EventService`.
- **Execution**: `findByName("order.created")` retrieves the configuration to launch the "Order Processing" workflow.

## 5. Source Link
[EventRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/EventRepository.java)
