# Class Profile: SessionGroupController

**File Path**: `com/example/aikef/controller/SessionGroupController.java`
**Type**: Controller (`@RestController`)
**Purpose**: Manages custom "Session Groups" (or folders) for agents. Allows agents to organize their active chats into custom groups (e.g., "VIP Clients", "Follow-up Later"). Also supports binding categories to groups, so sessions of a certain category automatically appear in a specific group.

# Method Deep Dive

## Group Management
- **`getMyGroups()`**: Returns the current agent's folder structure, including sessions within each folder.
- **`createGroup(...)`, `updateGroup(...)`, `deleteGroup(...)`**: CRUD for folders.

## Session Assignment
- **`moveSessionToGroup(groupId, sessionId)`**: Manually drags a session into a folder.
- **`removeSessionFromGroup(sessionId)`**: Moves a session back to the default "Inbox" or "All" list.

## Category Binding (Automation)
- **`bindCategoryToGroup(...)`**: Configures a rule: "All sessions with Category X go to Group Y".
- **`unbindCategoryFromGroup(...)`**: Removes the rule.
- **`getAvailableCategories()`**: Lists categories not yet bound to any of the agent's groups.

# Dependency Graph

**Core Dependencies**:
- `SessionGroupService`: Logic for group management and category binding.
- `ChatSessionService`: Logic for moving sessions.
- `SessionGroupMappingRepository`: Persistence of Session-Group relationships.

**Key Imports**:
```java
import com.example.aikef.service.SessionGroupService;
import com.example.aikef.service.ChatSessionService;
import org.springframework.web.bind.annotation.*;
```

# Usage Guide

## Workflow
1. Agent creates a group "Urgent Refunds".
2. Agent binds category "Refund Request" to "Urgent Refunds".
3. New incoming chat with topic "Refund Request" automatically appears in "Urgent Refunds".
4. Agent can also manually drag a "General Inquiry" chat into "Urgent Refunds" if it turns out to be critical.

# Source Link
[SessionGroupController.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/controller/SessionGroupController.java)
