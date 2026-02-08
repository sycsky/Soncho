# AiScheduledTask

## 1. Class Profile
- **Class Name**: `AiScheduledTask`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Automation Entity
- **Purpose**: Defines a recurring task that triggers an AI workflow. Examples include "Daily Sentiment Analysis" or "Weekly Customer Check-in".

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `name` / `description`: Metadata.
- `enabled`: Toggle.
- `cronExpression`: Unix-style cron string (e.g., `0 0 9 * * ?`).
- `scheduleConfig`: JSON storing user-friendly schedule parameters (used to reconstruct the UI).
- `workflow`: The `AiWorkflow` to execute.
- `customerMode`: Target audience (`ALL_CUSTOMERS`, `BY_TAG`, `BY_SEGMENT`).
- `targetConfig`: JSON defining the specific tags or segments to target.
- `initialInput`: Optional starting prompt variables for the workflow.
- `lastRunAt` / `nextRunAt`: Execution timestamps.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.AiWorkflow`: The logic to run.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
This entity is processed by the `AiScheduledTaskService`.
- **Scheduler**: A background thread polls for tasks where `nextRunAt <= now`.
- **Execution**: When triggered, it fetches the target customers (e.g., all users with tag "VIP"), and for each one, starts a new workflow execution instance.

## 5. Source Link
[AiScheduledTask.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/AiScheduledTask.java)
