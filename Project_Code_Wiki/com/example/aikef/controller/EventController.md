# Class Profile: EventController

**File Path**: `com/example/aikef/controller/EventController.java`
**Type**: Controller (`@RestController`)
**Purpose**: Manages system events and external webhooks. It allows defining custom events, binding them to workflows, and triggering them via external API calls (hooks). This enables the system to react to external triggers (e.g., "Order Created" in Shopify) by executing specific workflows.

# Method Deep Dive

## Event Management (CRUD)
- **`getAllEvents()`**: Lists all configured events.
- **`getEnabledEvents()`**: Lists only active events suitable for UI selection or internal logic.
- **`createEvent(...)`, `updateEvent(...)`, `deleteEvent(...)`**: Standard CRUD operations.

## Event Triggering
- **`receiveEventHook(String event, EventHookRequest request)`**
  - **Endpoint**: `POST /api/v1/events/hook/{event}`
  - **Purpose**: External entry point to trigger an event by name.
  - **Logic**:
    1. Receives the event name and payload (including `sessionId` and `eventData`).
    2. Delegates to `eventService.triggerEvent(...)` which finds the bound workflow and executes it.
    3. Returns an `EventHookResponse` indicating success/failure and the workflow's reply.

# Dependency Graph

**Core Dependencies**:
- `EventService`: Business logic for event configuration and triggering.
- `AiWorkflowService`: Used indirectly via EventService to execute workflows.
- `EventDto`: Response DTO.

**Key Imports**:
```java
import com.example.aikef.service.EventService;
import com.example.aikef.workflow.service.AiWorkflowService;
import org.springframework.web.bind.annotation.*;
```

# Usage Guide

## Triggering an Event
External systems (e.g., Shopify Webhook) can call this endpoint to start a workflow for a user.

`POST /api/v1/events/hook/ORDER_CREATED`
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "eventData": {
    "orderId": "1001",
    "amount": 99.99
  }
}
```

# Source Link
[EventController.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/controller/EventController.java)
