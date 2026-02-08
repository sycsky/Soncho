# SaveScheduledTaskRequest

## 1. Class Profile
- **Class Name**: `SaveScheduledTaskRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: Payload for defining a recurring AI task (e.g., "Send a weekly newsletter").

## 2. Method Deep Dive
### Fields
- `name`, `description`: Metadata.
- `workflowId`: The AI workflow to execute.
- `scheduleConfig`: Cron-like scheduling rules (Daily/Weekly/Monthly).
- `customerMode`: Who to target (All, Tagged, etc.).
- `targetIds`: IDs of specific customers or tags.

### Nested Records
- **ScheduleConfig**: Defines the timing rules.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.enums.TaskCustomerMode`

## 4. Usage Guide
Used by `ScheduledTaskController`.
- **Automation**: Marketing or support automation (e.g., "Follow up with all new users every Monday").

## 5. Source Link
[SaveScheduledTaskRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/SaveScheduledTaskRequest.java)
