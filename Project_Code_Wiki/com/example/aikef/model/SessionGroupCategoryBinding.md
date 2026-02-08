# SessionGroupCategoryBinding

## 1. Class Profile
- **Class Name**: `SessionGroupCategoryBinding`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Association Entity
- **Purpose**: Links a `SessionCategory` (e.g., "Billing") to a `SessionGroup` (e.g., "My Billing Queue") for a specific agent. This allows agents to organize categories into their personal folders.

## 2. Method Deep Dive
### Fields
- `sessionGroup`: The folder.
- `category`: The category being filed.
- `agent`: The owner (redundant but useful for constraints).

### Constraints
- `uk_agent_category`: An agent can only bind a category to one group at a time.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.SessionGroup`
  - `com.example.aikef.model.SessionCategory`
  - `com.example.aikef.model.Agent`
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
Used by `SessionGroupService`.
- **UI**: When an agent drags "Billing" into "Priority", a new binding record is created.

## 5. Source Link
[SessionGroupCategoryBinding.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/SessionGroupCategoryBinding.java)
