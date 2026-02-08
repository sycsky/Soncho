# AiScheduledTaskController

## Class Profile
- **Package**: `com.example.aikef.controller`
- **Type**: Class
- **Description**: Controller for managing AI scheduled tasks (e.g., periodic reports, cleanups).
- **Key Features**:
  - Task CRUD (Create, Update, Delete, Get).
  - List all tasks.

## Method Deep Dive

### `createTask`
- **Description**: Creates a new scheduled task.
- **Signature**: `public Result<ScheduledTaskDto> createTask(SaveScheduledTaskRequest request)`
- **Logic**: Calls `scheduledTaskService.createTask`.

### `updateTask`
- **Description**: Updates an existing task.
- **Signature**: `public Result<ScheduledTaskDto> updateTask(UUID id, SaveScheduledTaskRequest request)`
- **Logic**: Calls `scheduledTaskService.updateTask`.

### `deleteTask`
- **Description**: Deletes a task by ID.
- **Signature**: `public Result<Void> deleteTask(UUID id)`
- **Logic**: Calls `scheduledTaskService.deleteTask`.

## Dependency Graph
- **Injected Services**:
  - `AiScheduledTaskService`
- **DTOs**:
  - `ScheduledTaskDto`, `SaveScheduledTaskRequest`, `Result`

## Usage Guide
```bash
POST /api/v1/scheduled-tasks
```
