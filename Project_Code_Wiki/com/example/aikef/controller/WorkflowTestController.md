# Class Profile: WorkflowTestController

**File Path**: `com/example/aikef/controller/WorkflowTestController.java`
**Type**: Controller (`@RestController`)
**Purpose**: Facilitates the testing of workflows directly from the frontend editor. It allows designers to start temporary test sessions, send messages to a running workflow instance, and inspect the execution state (variables, outputs) without affecting production data.

# Method Deep Dive

## Session Management
- **`createSession(...)`**: Starts a new ephemeral test session for a specific `workflowId` with initial variables.
- **`getSession(id)`**: Retrieves the current state of a test session.
- **`clearSession(id)`**: Resets the conversation history but keeps the session alive.
- **`deleteSession(id)`**: Terminates the test session.

## Interaction
- **`sendMessage(id, request)`**: Sends a user message to the workflow. Returns the workflow's response and any state updates.
- **`setVariables(id, variables)`**: Manually injects or updates variables in the running session (debugging).

# Dependency Graph

**Core Dependencies**:
- `WorkflowTestService`: Logic for managing temporary test sessions.
- `WorkflowTestSessionDto`: Response DTO containing chat history and variable state.

**Key Imports**:
```java
import com.example.aikef.workflow.service.WorkflowTestService;
import com.example.aikef.dto.WorkflowTestSessionDto;
import org.springframework.web.bind.annotation.*;
```

# Usage Guide

## Testing a Workflow
1. **Start**: `POST /api/v1/workflow-test/sessions` -> returns `testSessionId`.
2. **Chat**: `POST /api/v1/workflow-test/sessions/{id}/messages` with `{"message": "hi"}`.
3. **Inspect**: Response includes `reply: "Hello!"` and `variables: { "intent": "greeting" }`.

# Source Link
[WorkflowTestController.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/controller/WorkflowTestController.java)
