# WorkflowTestSessionDto

## 1. Class Profile
- **Class Name**: `WorkflowTestSessionDto`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: Represents a transient test session for the Workflow Editor's testing feature. It allows developers to chat with a workflow version without affecting real customer data.

## 2. Method Deep Dive
### Fields
- `testSessionId`: Ephemeral session ID.
- `workflowId`: The ID of the workflow being tested.
- `workflowName`: Name of the workflow.
- `messages`: List of `TestMessage` objects (chat history).
- `createdAt`: Session start time.
- `lastActiveAt`: Last activity time.

### Nested Records
- **TestMessage**: Represents a single message in the test chat.
- **TestMessageMeta**: Detailed metadata for a message, including execution duration, errors, and node trace.
- **NodeDetail**: Granular detail for a single node execution (input, output, duration).

## 3. Dependency Graph
- **Internal Dependencies**: None (Self-contained nested records).

## 4. Usage Guide
Used by `WorkflowTestController`.
- **Workflow Editor**: Populates the "Test Run" panel.
- **Visual Debugging**: The `NodeDetail` list allows the UI to highlight nodes on the canvas as they are executed and show their inputs/outputs.

## 5. Source Link
[WorkflowTestSessionDto.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/WorkflowTestSessionDto.java)
