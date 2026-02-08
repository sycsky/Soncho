# WorkflowCategoryBinding

## 1. Class Profile
- **Class Name**: `WorkflowCategoryBinding`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Persistence Entity
- **Purpose**: Binds an AI Workflow to a Session Category. This allows the system to automatically trigger specific workflows when a session is categorized.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `workflow`: The workflow to execute (Many-to-One).
- `category`: The session category that triggers this workflow (Many-to-One, Unique).
- `priority`: Execution priority (lower number = higher priority).
- `createdAt`: Timestamp of creation.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.AiWorkflow`: The workflow entity.
  - `com.example.aikef.model.SessionCategory`: The category entity.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
Used to configure automated responses or processes for specific topics.
- **Auto-Reply**: When a user selects "Returns" category, the "Return Policy Workflow" is triggered.
- **Routing**: Can be used to route chats to specific agent groups via workflow logic.

## 5. Source Link
[WorkflowCategoryBinding.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/WorkflowCategoryBinding.java)
