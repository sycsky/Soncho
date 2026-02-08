# Class Profile: ReactFlowToLiteflowConverter

**File Path**: `com/example/aikef/workflow/converter/ReactFlowToLiteflowConverter.java`
**Type**: Component (`@Component`)
**Purpose**: Converts ReactFlow workflow JSON (nodes and edges) into LiteFlow EL (Expression Language) strings. It supports complex workflow structures including parallel execution (`WHEN`), conditional routing (`SWITCH`), and sub-chain splitting for LLM nodes to handle pause/resume mechanisms.

# Method Deep Dive

## `convert(String nodesJson, String edgesJson)`
- **Description**: Entry point for converting raw JSON strings of nodes and edges into a LiteFlow EL expression.
- **Logic**: Parses JSON into `WorkflowNodeDto` and `WorkflowEdgeDto` lists, then calls the overloaded `convert` method. Handles JSON parsing exceptions.

## `convert(List<WorkflowNodeDto> nodes, List<WorkflowEdgeDto> edges)`
- **Description**: Core conversion logic.
- **Key Steps**:
  1. Builds adjacency lists (in/out edges) and node maps.
  2. Identifies the start node (node with type "start" or no incoming edges).
  3. Recursively generates EL expressions using `generateEl`.
  4. Handles branching logic (parallel vs. sequential).
  5. Detects "converge nodes" (where parallel branches merge back) to correctly structure `WHEN(...)` blocks followed by the merged path.

## `convertWithSubChains(String nodesJson, String edgesJson, String workflowId)`
- **Description**: Advanced conversion that splits the workflow into a main chain and multiple sub-chains based on LLM nodes.
- **Purpose**: Essential for the agent's ability to pause execution at LLM nodes (e.g., for tool calls requiring user input) and resume later.
- **Logic**:
  1. Identifies all LLM nodes.
  2. Generates independent sub-chains for each LLM node and its downstream nodes (`generateSubChain`).
  3. Generates the main chain where LLM nodes are replaced by `CHAIN(subchain_id)` calls.
  4. Returns a `ConversionResult` containing the main EL and a map of sub-chains.

## `generateEl(...)` & `generateElUntilConverge(...)`
- **Description**: Recursive helper methods to build the EL string.
- **Features**:
  - **Sequential**: Uses `THEN(a, b)`.
  - **Parallel**: Uses `WHEN(a, b)` for diverging paths.
  - **Switch**: Uses `SWITCH(node).TO(a, b)` for conditional nodes (`condition`, `intent`, `tool`).
  - **Recursion Control**: Uses a `visited` set to prevent infinite loops in cyclic graphs (though workflows are typically DAGs).
  - **Formatting**: Adds indentation for readability.

## `findConvergeNode(...)`
- **Description**: Detects a node where multiple parallel branches merge.
- **Logic**: Checks if a node has multiple incoming edges that all originate from the current set of parallel branch paths. This allows the converter to close a `WHEN` block and continue sequentially.

# Dependency Graph

**Core Dependencies**:
- `com.example.aikef.workflow.dto.*`: Data transfer objects for nodes and edges.
- `com.fasterxml.jackson.databind.ObjectMapper`: JSON parsing.
- `org.slf4j.Logger`: Logging.

**Key Imports**:
```java
import com.example.aikef.workflow.dto.WorkflowEdgeDto;
import com.example.aikef.workflow.dto.WorkflowNodeDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;
```

# Usage Guide

This converter is typically used by the `AiWorkflowService` or `WorkflowGeneratorService` when saving or executing a workflow designed in the frontend ReactFlow editor.

## Basic Conversion
```java
@Autowired
private ReactFlowToLiteflowConverter converter;

public void saveWorkflow(String nodesJson, String edgesJson) {
    String el = converter.convert(nodesJson, edgesJson);
    System.out.println("Generated EL: " + el);
    // Output example: THEN(node("start").tag("1"), node("process").tag("2"))
}
```

## Sub-Chain Conversion (For Resumable Workflows)
```java
public void compileWorkflow(String nodes, String edges, String workflowId) {
    var result = converter.convertWithSubChains(nodes, edges, workflowId);
    
    // Register main chain
    FlowBus.addChain("main_" + workflowId, result.mainChainEl());
    
    // Register sub-chains
    result.subChains().forEach((id, info) -> {
        FlowBus.addChain(info.chainId(), info.chainEl());
    });
}
```

# Source Link
[ReactFlowToLiteflowConverter.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/workflow/converter/ReactFlowToLiteflowConverter.java)
