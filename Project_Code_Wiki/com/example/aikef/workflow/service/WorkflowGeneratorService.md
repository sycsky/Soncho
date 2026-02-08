# Class Profile: WorkflowGeneratorService

**File Path**: `com/example/aikef/workflow/service/WorkflowGeneratorService.java`
**Type**: Service (`@Service`)
**Purpose**: Uses Generative AI (LLM) to automatically create or modify workflows based on natural language descriptions. It acts as an AI-powered "Workflow Designer" that converts user intent (e.g., "Create a workflow for refund handling") into a valid JSON structure compatible with the workflow engine.

# Method Deep Dive

## `generateWorkflow(String userPrompt, ...)`
- **Description**: Main entry point for workflow generation.
- **Logic**:
  1. Fetches available `AiTool`s and `LlmModel`s to inform the LLM of capabilities.
  2. Parses existing workflow JSON (if modifying).
  3. Constructs a detailed System Prompt (`buildSystemPrompt`) containing:
     - Available tools/models.
     - Node type definitions (from documentation).
     - Design rules (must have start/end, specific ID formats).
     - JSON output schema requirements.
  4. Calls the LLM (`langChainChatService.chat`) with a long timeout (5 min).
  5. Validates and fixes the returned JSON (`fixNode`, `fixEdge`).
  6. Merges with existing workflow if applicable.
  7. Returns the generated nodes and edges JSON.

## `buildSystemPrompt(...)`
- **Description**: Constructs the massive prompt required to guide the LLM.
- **Key Content**: Includes rules for layout (x,y coordinates), edge connections (`sourceHandle`), and switch node logic. It dynamically loads node documentation from `docs/workflow_node.md` or falls back to a hardcoded string.

## `fixNode(...)` & `fixEdge(...)`
- **Description**: Post-processing to ensure the LLM's output is valid.
- **Fixes**: Generates missing IDs, sets default positions, ensures valid types, and fixes edge references.

# Dependency Graph

**Core Dependencies**:
- `LangChainChatService`: For LLM interaction.
- `AiToolRepository` / `LlmModelService`: Context for the AI.
- `ObjectMapper`: JSON processing.

**Key Imports**:
```java
import com.example.aikef.llm.LangChainChatService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
```

# Usage Guide

## Generating a New Workflow
```java
GeneratedWorkflow result = generatorService.generateWorkflow(
    "Create a customer support workflow that asks for order ID and checks status",
    modelId, 
    null, 
    null
);
String nodes = result.nodesJson();
String edges = result.edgesJson();
```

# Source Link
[WorkflowGeneratorService.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/workflow/service/WorkflowGeneratorService.java)
