# AgentMention

## 1. Class Profile
- **Class Name**: `AgentMention`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Notification Entity
- **Purpose**: Records when an agent is mentioned (e.g., "@Support") in a chat or internal note. This drives the "Mentions" notification system.

## 2. Method Deep Dive
### Fields
- `agent`: The mentioned agent.
- `session`: The session where it happened.
- `message`: The specific message containing the mention.
- `read`: Status flag (Has the agent seen this mention?).

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Agent`
  - `com.example.aikef.model.Message`
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
Used by `AgentMentionService`.
- **Creation**: When a message is saved, regex parses for `@UUID`. If found, `AgentMention` records are created.
- **Notification**: This triggers a real-time WebSocket push to the mentioned agent.

## 5. Source Link
[AgentMention.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/AgentMention.java)
