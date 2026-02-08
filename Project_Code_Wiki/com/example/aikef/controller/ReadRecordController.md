# Class Profile: ReadRecordController

**File Path**: `com/example/aikef/controller/ReadRecordController.java`
**Type**: Controller (`@RestController`)
**Purpose**: Manages the "Read Status" of chat sessions for agents. It tracks when an agent last viewed a session to calculate unread message counts, helping agents prioritize which customers need attention.

# Method Deep Dive

## Status Updates
- **`markAsRead(sessionId)`**: Updates the "last read timestamp" for the current agent on a specific session. Called when the chat window is opened or focused.

## Statistics
- **`getUnreadCount(sessionId)`**: Returns the number of unread messages for a specific session.
- **`getTotalUnreadSessions()`**: Returns the total number of sessions that have unread messages for the current agent.

# Dependency Graph

**Core Dependencies**:
- `ReadRecordService`: Logic for tracking timestamps and counting messages.
- `AgentPrincipal`: Security context.

**Key Imports**:
```java
import com.example.aikef.service.ReadRecordService;
import org.springframework.web.bind.annotation.*;
```

# Usage Guide

## Marking Read
`POST /api/v1/read-records/sessions/{sessionId}/mark-read`
(No body required)

## Checking Global Status
`GET /api/v1/read-records/unread-sessions-count`
Response: `5` (5 conversations have new messages)

# Source Link
[ReadRecordController.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/controller/ReadRecordController.java)
