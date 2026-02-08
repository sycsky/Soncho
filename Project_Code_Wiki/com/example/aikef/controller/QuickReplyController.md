# Class Profile: QuickReplyController

**File Path**: `com/example/aikef/controller/QuickReplyController.java`
**Type**: Controller (`@RestController`)
**Purpose**: Manages "Canned Responses" or "Quick Replies" for agents. Agents can create personal quick replies, and admins can define system-wide replies. These are used to speed up response times for common customer queries.

# Method Deep Dive

## CRUD Operations
- **`getAllReplies(...)`**: Fetches all available replies (System + Personal).
- **`getReply(id)`**: Gets details of a specific reply.
- **`createReply(...)`**: Creates a new quick reply.
- **`updateReply(...)`**: Updates an existing reply (permission restricted).
- **`deleteReply(...)`**: Deletes a reply (permission restricted).

# Dependency Graph

**Core Dependencies**:
- `QuickReplyService`: Business logic.
- `AgentPrincipal`: Identifies the current agent to scope personal replies.

**Key Imports**:
```java
import com.example.aikef.service.QuickReplyService;
import com.example.aikef.dto.QuickReplyDto;
import org.springframework.web.bind.annotation.*;
```

# Usage Guide

## Fetching Replies
`GET /api/v1/quick-replies`
Returns a list like:
```json
[
  {
    "id": "uuid...",
    "label": "Greeting",
    "text": "Hello! How can I help you today?",
    "category": "General",
    "system": true
  },
  {
    "id": "uuid...",
    "label": "My Signoff",
    "text": "Thanks, Agent Smith",
    "category": "Personal",
    "system": false
  }
]
```

# Source Link
[QuickReplyController.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/controller/QuickReplyController.java)
