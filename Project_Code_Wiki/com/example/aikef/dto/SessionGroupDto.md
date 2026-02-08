# SessionGroupDto

## 1. Class Profile
- **Class Name**: `SessionGroupDto`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record`
- **Role**: Data Transfer Object
- **Purpose**: Transfers session group (folder) details, including the list of sessions currently inside it.

## 2. Method Deep Dive
### Fields
- `id` / `name`: Identity.
- `system`: Is it a built-in folder?
- `sessions`: List of `ChatSessionDto` objects.
- `categories`: List of `SessionCategoryDto` bound to this group.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `ChatSessionDto`
  - `SessionCategoryDto`

## 4. Usage Guide
Used by `SessionGroupController`.
- **Kanban/Folder View**: `GET /api/v1/session-groups/with-sessions` returns the full board state.

## 5. Source Link
[SessionGroupDto.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/SessionGroupDto.java)
