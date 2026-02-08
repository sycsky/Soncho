# SessionCategory

## 1. Class Profile
- **Class Name**: `SessionCategory`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Classification Entity
- **Purpose**: Defines categories for tagging chat sessions (e.g., "Technical Support", "Billing", "Sales"). This helps in organizing queues and generating reports.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `name`: Category name (unique).
- `description`: Usage guidelines.
- `icon` / `color`: Visual indicators for the UI.
- `sortOrder`: Display priority.
- `enabled`: Toggle.
- `createdByAgent`: Creator audit.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Agent`: Creator.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
Used to label `ChatSession` objects.
- **Workflow Automation**: An AI workflow can analyze a message, detect intent "Refund", and automatically update the session's category to "Billing".
- **Reporting**: "Show me ticket volume by Category".

## 5. Source Link
[SessionCategory.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/SessionCategory.java)
