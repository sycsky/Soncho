# Class Profile: SessionNoteController

**File Path**: `com/example/aikef/controller/SessionNoteController.java`
**Type**: Controller (`@RestController`)
**Purpose**: Manages internal notes attached to a specific chat session. These notes are visible only to agents and are used to record important details, summaries, or hand-off instructions about a conversation.

# Method Deep Dive

## Note Management
- **`getNote(sessionId)`**: Retrieves the current note content.
- **`createNote(sessionId, ...)`**: Adds a note if one doesn't exist. Throws conflict if already present.
- **`updateNote(sessionId, ...)`**: Modifies an existing note (or creates it if missing).
- **`deleteNote(sessionId)`**: Clears the note content.

# Dependency Graph

**Core Dependencies**:
- `ChatSessionService`: Logic for storing and retrieving notes.
- `CreateSessionNoteRequest` / `UpdateSessionNoteRequest`: Request DTOs.

**Key Imports**:
```java
import com.example.aikef.service.ChatSessionService;
import org.springframework.web.bind.annotation.*;
```

# Usage Guide

## Adding a Note
`POST /api/v1/chat/sessions/{sessionId}/note`
```json
{
  "content": "Customer is asking for a refund. Order ID #12345."
}
```

# Source Link
[SessionNoteController.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/controller/SessionNoteController.java)
