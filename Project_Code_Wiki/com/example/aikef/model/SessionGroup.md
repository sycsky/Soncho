# SessionGroup

## 1. Class Profile
- **Class Name**: `SessionGroup`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Organization Entity
- **Purpose**: Represents a custom folder or group for organizing chat sessions (e.g., "My Priority", "Follow-up Later"). Unlike categories (which are global), groups can be personal to an agent.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `name`: Group name.
- `system`: Flag for immutable system groups (e.g., "Inbox", "Done").
- `agent`: The owner of this group.
- `icon` / `color`: Visual customization.
- `sortOrder`: Display order.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Agent`: Owner.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
This entity supports the "Folder" view in the agent console.
- **Personalization**: Agent A can create a "High Value" group and drag sessions into it. This doesn't affect Agent B's view.

## 5. Source Link
[SessionGroup.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/SessionGroup.java)
