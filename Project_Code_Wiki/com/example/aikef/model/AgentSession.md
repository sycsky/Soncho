# AgentSession

## 1. Class Profile
- **Class Name**: `AgentSession`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / State Entity
- **Purpose**: Represents a persistent context for a special type of "Agent Workflow". Unlike standard stateless workflows, an Agent Session maintains memory (system prompt, variable state) across multiple user messages until it is explicitly ended.

## 2. Method Deep Dive
### Fields
- `sessionId`: The chat session this agent is attached to.
- `workflow`: The "Agent Workflow" definition.
- `sysPrompt`: The dynamic system prompt, which can be updated by the workflow itself (e.g., "User is now authenticated").
- `ended`: Status flag.
- `endedAt`: Timestamp.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.AiWorkflow`
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
This entity powers the "Agent" node type in the workflow editor.
- **Scenario**: A user enters a "Booking Agent" flow. The workflow creates an `AgentSession` with a system prompt "You are a booking assistant".
- **Persistence**: Subsequent user messages are routed to this existing session, maintaining context, until the agent decides to "End Session" (e.g., after booking is complete).

## 5. Source Link
[AgentSession.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/AgentSession.java)
