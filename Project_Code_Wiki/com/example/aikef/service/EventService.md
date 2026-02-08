# EventService

## Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: Class
- **Description**: Service for managing system Events and triggering associated Workflows.
- **Key Features**:
  - Event CRUD.
  - Event Triggering (Synchronous & Async).
  - Workflow Integration.

## Method Deep Dive

### `triggerEvent`
- **Description**: Triggers a specific event, executing the bound workflow.
- **Signature**: `public WorkflowExecutionResult triggerEvent(String eventName, UUID sessionId, Map<String, Object> eventData)`
- **Logic**:
  1. Validates event existence and enabled status.
  2. Loads session.
  3. Prepares Workflow Context variables (injects `eventData`, `eventName`, `sessionId`).
  4. Calls `workflowService.executeWorkflow`.
  5. If workflow returns a reply, sends it as an AI message via `messageGateway`.

### `triggerEventForCustomer`
- **Description**: Triggers an event for a customer by finding their latest active session.
- **Signature**: `public WorkflowExecutionResult triggerEventForCustomer(UUID customerId, String eventName, Map<String, Object> eventData)`
- **Logic**:
  1. Finds most recent session for customer.
  2. Calls `triggerEvent`.

### `createEvent`
- **Description**: Registers a new event.
- **Signature**: `public Event createEvent(CreateEventRequest request)`
- **Logic**: Validates name uniqueness, links Workflow, and saves.

## Dependency Graph
- **Injected Services**:
  - `EventRepository`, `ChatSessionRepository`
  - `AiWorkflowService`: To execute workflows.
  - `SessionMessageGateway`: To send replies.
- **DTOs**:
  - `CreateEventRequest`
  - `WorkflowExecutionResult`

## Usage Guide
```java
eventService.triggerEvent("ORDER_SHIPPED", sessionId, Map.of("trackingNumber", "123456"));
```
