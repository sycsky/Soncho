# ScheduledTaskDto

## 1. Class Profile
- **Class Name**: `ScheduledTaskDto`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record`
- **Role**: Data Transfer Object
- **Purpose**: Transfers scheduled task configuration.

## 2. Method Deep Dive
### Fields
- `id` / `name` / `description`: Metadata.
- `workflowId` / `workflowName`: The task to run.
- `cronExpression`: The schedule.
- `scheduleConfig`: UI-friendly schedule object.
- `customerMode` / `targetIds`: Target audience.
- `lastRunAt` / `nextRunAt`: Status.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.enums.TaskCustomerMode`

## 4. Usage Guide
Used by `AiScheduledTaskController`.
- **List**: `GET /api/v1/scheduled-tasks`.

## 5. Source Link
[ScheduledTaskDto.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/ScheduledTaskDto.java)
