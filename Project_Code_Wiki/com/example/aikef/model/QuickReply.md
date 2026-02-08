# QuickReply

## 1. Class Profile
- **Class Name**: `QuickReply`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Feature Entity
- **Purpose**: Represents a pre-canned response template that agents can use to quickly reply to common customer queries.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `label`: Short title/shortcut for the reply (e.g., "/welcome").
- `text`: The full response content.
- `category`: Grouping tag (e.g., "Greeting", "Refund").
- `system`: Flag indicating if this is a global system-wide reply or a personal one.
- `createdBy`: The agent who owns this reply (if personal).

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Agent`: Creator.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
This entity powers the "Canned Responses" feature in the chat console.
- **Search**: Agents type "/" to trigger a search against `label` and `text`.
- **Scope**: System replies are visible to all agents; personal replies are only visible to the creator.

## 5. Source Link
[QuickReply.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/QuickReply.java)
