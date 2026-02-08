# Class Profile: SystemPromptController

**File Path**: `com/example/aikef/controller/SystemPromptController.java`
**Type**: Controller (`@RestController`)
**Purpose**: Provides an endpoint to "enhance" or "beautify" system prompts using AI. It helps administrators or workflow designers generate better, more effective system prompts based on the node type, available tools, and user input context.

# Method Deep Dive

## Enhancement
- **`enhanceSystemPrompt(EnhanceSystemPromptRequest request)`**
  - **Endpoint**: `POST /api/system-prompt/enhance`
  - **Input**: `nodeType` (e.g., "llm"), `toolIds` (list of tools), `userInput` (sample user query).
  - **Logic**: Calls `SystemPromptEnhancementService` which likely uses an LLM to rewrite the prompt instructions, making them clearer and more robust for the target AI model.

# Dependency Graph

**Core Dependencies**:
- `SystemPromptEnhancementService`: The core AI service that performs the prompt engineering.

**Key Imports**:
```java
import com.example.aikef.service.SystemPromptEnhancementService;
import org.springframework.web.bind.annotation.*;
```

# Usage Guide

## Improving a Prompt
`POST /api/system-prompt/enhance`
```json
{
  "nodeType": "llm",
  "toolIds": ["uuid-tool-1", "uuid-tool-2"],
  "userInput": "Check my order status"
}
```
**Response**:
```json
{
  "systemPrompt": "You are a helpful assistant. You have access to OrderLookupTool. When a user asks about order status..."
}
```

# Source Link
[SystemPromptController.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/controller/SystemPromptController.java)
