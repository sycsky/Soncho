# Class Profile: WorkflowStatusService

**File Path**: `com/example/aikef/workflow/service/WorkflowStatusService.java`
**Type**: Service (`@Service`)
**Purpose**: Manages the real-time broadcasting of workflow execution status to the frontend. It translates technical execution steps (e.g., "TOOL_CALLING OrderLookup") into user-friendly, localized messages (e.g., "🔍 Checking your order details...") often using a small, fast LLM for natural language generation.

# Method Deep Dive

## `updateStatus(...)`
- **Description**: Asynchronously broadcasts a status update.
- **Parameters**: `sessionId`, `StatusType` (ANALYZING, TOOL_CALLING, etc.), `data`.
- **Logic**:
  1. Checks if status streaming is enabled in `WorkflowContext`.
  2. Calls `interpretStatus` to generate a user-friendly message.
  3. Uses `WebSocketEventService` to push the message to the client.

## `interpretStatus(...)`
- **Description**: Converts technical status to natural language.
- **Logic**:
  1. Tries to find a configured "Small Model" (fast, cheap LLM).
  2. If found, prompts it to translate/explain the action (e.g., "Explain 'TOOL_CALLING' in 15 words").
  3. If not found or error, falls back to `getDefaultDescription`.

## `getDefaultDescription(...)`
- **Description**: Hardcoded fallback messages (English/Chinese) based on status type.

# Dependency Graph

**Core Dependencies**:
- `WebSocketEventService`: Transport layer.
- `LangChainChatService`: AI translation.
- `LlmModelRepository`: Finding the small model.

**Key Imports**:
```java
import com.example.aikef.service.WebSocketEventService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
```

# Usage Guide

Used inside `LlmNode` or `ToolCallProcessor`.

```java
statusService.updateStatus(
    sessionId, 
    StatusType.TOOL_CALLING, 
    "OrderLookupTool", 
    context
);
```

# Source Link
[WorkflowStatusService.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/workflow/service/WorkflowStatusService.java)
