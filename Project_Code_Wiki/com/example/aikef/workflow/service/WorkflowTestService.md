# Class Profile: WorkflowTestService

**File Path**: `com/example/aikef/workflow/service/WorkflowTestService.java`
**Type**: Service (`@Service`)
**Purpose**: Manages ephemeral test sessions for workflow validation. It creates isolated environments (Test ChatSession, Test Customer) where workflow logic can be verified without polluting production data. It handles the lifecycle of these sessions (creation, messaging, cleanup).

# Method Deep Dive

## Session Lifecycle
- **`createTestSession(...)`**: Initializes a `TestSession` in memory and a corresponding `ChatSession` in the database. Sets metadata `isTest=true`.
- **`deleteTestSession(...)`**: Removes the session from memory and deletes the associated database records (messages + session).
- **`cleanupExpiredSessions()`**: Scheduled task (every min) to purge sessions inactive for >30 minutes.

## Messaging
- **`sendTestMessage(...)`**:
  1. Saves the user message via `messageGateway`.
  2. Executes the workflow using `workflowService.executeForSession`.
  3. Captures execution details (success, duration, node logs).
  4. Saves the AI reply.
  5. Returns the updated session state with message history.

## Helpers
- **`createTestChatSession(...)`**: Creates a dummy customer and session record. Binds to the workflow's category if applicable.
- **`parseNodeDetails(...)`**: Deserializes execution logs for frontend visualization.

# Dependency Graph

**Core Dependencies**:
- `AiWorkflowService`: Executes the actual workflow logic.
- `ChatSessionRepository`, `MessageRepository`: Database persistence.
- `SessionMessageGateway`: Message handling.

**Key Imports**:
```java
import com.example.aikef.workflow.service.AiWorkflowService;
import com.example.aikef.repository.ChatSessionRepository;
import org.springframework.stereotype.Service;
```

# Usage Guide

Used by `WorkflowTestController`.

```java
// Start test
WorkflowTestSessionDto session = testService.createTestSession(workflowId, variables);

// Send message
session = testService.sendTestMessage(session.id(), "Hello");

// Check result
System.out.println(session.messages().get(1).content()); // AI Reply
```

# Source Link
[WorkflowTestService.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/workflow/service/WorkflowTestService.java)
