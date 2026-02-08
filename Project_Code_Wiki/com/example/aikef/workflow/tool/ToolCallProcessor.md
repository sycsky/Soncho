# Class Profile: ToolCallProcessor

**File Path**: `com/example/aikef/workflow/tool/ToolCallProcessor.java`
**Type**: Service (`@Service`)
**Purpose**: Central processor for handling LLM tool calls (function calling). It bridges the gap between the LLM's request to call a tool and the actual execution, handling parameter validation, missing parameter collection (slot filling), and multi-turn dialogue management.

# Method Deep Dive

## `processToolCall(ToolCallState state, String userMessage, UUID sessionId)`
- **Description**: Main entry point for processing a pending tool call.
- **Logic**:
  1. Retrieves the tool definition and schema.
  2. Validates provided arguments against the schema's required fields.
  3. Identifies missing parameters.
  4. If parameters are missing:
     - Generates a follow-up question (`buildFollowupQuestion`).
     - Updates state to `WAITING_USER_INPUT`.
     - Returns `NEED_MORE_PARAMS` result.
  5. If all parameters are present:
     - Executes the tool (`executeToolWithParams`).
     - Returns `SUCCESS` result.

## `continueParamCollection(...)`
- **Description**: Handles the user's response to a follow-up question (slot filling).
- **Logic**:
  1. Uses LLM (via `extractParamsFromText`) to extract JSON parameters from the user's natural language response.
  2. Merges extracted parameters with previously collected ones.
  3. Re-evaluates missing parameters.
  4. Decides whether to ask another question or execute the tool.

## `buildToolSpecifications(List<UUID> toolIds)`
- **Description**: Converts internal `AiTool` entities into LangChain4j `ToolSpecification` objects.
- **Details**: Maps the custom JSON schema fields (String, Integer, Array, Object, etc.) to the format required by the LLM provider.

## `executeToolDirectly(...)`
- **Description**: Bypasses the parameter validation/collection loop and executes a tool immediately. Useful for internal tools or when arguments are guaranteed.

## `extractParamsFromText(...)`
- **Description**: Uses a "One-Shot" or "Zero-Shot" prompt to ask the LLM to extract specific fields from the user's text into a JSON object.

## `buildFollowupQuestion(...)`
- **Description**: Constructs a natural language question to ask the user for missing information.
- **Features**: Supports custom configured follow-up questions per field or generates generic ones based on field descriptions.

# Dependency Graph

**Core Dependencies**:
- `AiToolRepository` / `AiToolService`: Tool management and execution.
- `StructuredExtractionService`: Helper for LLM-based extraction.
- `LlmModelService`: Access to LLM models.
- `ToolCallState`: DTO tracking the conversation state.
- `dev.langchain4j.agent.tool.ToolSpecification`: LangChain4j integration.

**Key Imports**:
```java
import com.example.aikef.tool.model.AiTool;
import com.example.aikef.workflow.context.WorkflowContext;
import dev.langchain4j.agent.tool.ToolSpecification;
import org.springframework.stereotype.Service;
import java.util.*;
```

# Usage Guide

This processor is typically used within an `LlmNode` or a specialized `ToolNode` in the workflow engine.

## Basic Flow
```java
@Autowired
private ToolCallProcessor processor;

public void handleLlmResponse(ToolCallRequest request, UUID sessionId) {
    // 1. Initialize state
    ToolCallState state = new ToolCallState();
    state.setCurrentToolCall(request);
    
    // 2. Process
    ToolCallProcessResult result = processor.processToolCall(state, null, sessionId);
    
    if (result.isSuccess()) {
        // Tool executed, get output
        String output = result.getResult().getOutput();
    } else if (result.needsUserInput()) {
        // Ask user the generated question
        sendToUser(result.getQuestion());
        // Save state for next turn...
    }
}
```

# Source Link
[ToolCallProcessor.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/workflow/tool/ToolCallProcessor.java)
