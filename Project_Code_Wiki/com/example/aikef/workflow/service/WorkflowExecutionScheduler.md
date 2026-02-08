# WorkflowExecutionScheduler

## Class Profile
- **Package**: `com.example.aikef.workflow.service`
- **Type**: Class
- **Description**: Manages the execution queue of workflows, implementing **debouncing** and **concurrency control** for chat sessions.
- **Key Features**:
  - **Debouncing**: Delays execution to handle rapid-fire user messages (only processes the latest message after a quiet period).
  - **Serialized Execution**: Ensures only one workflow runs per session at a time using `ReentrantLock`.
  - **Queue Management**: Queues messages arriving during an active execution.

## Method Deep Dive

### `submitMessage`
- **Description**: Submits a user message for workflow processing.
- **Signature**: `public void submitMessage(UUID sessionId, String userMessage, UUID messageId)`
- **Logic**:
  1. Acquires session lock.
  2. If workflow `isExecuting`: adds message to `messageQueue`.
  3. If idle: adds to `debounceBuffer` and schedules `scheduleDebouncedExecution`.

### `scheduleDebouncedExecution`
- **Description**: Schedules the actual execution after a delay (debounce window).
- **Logic**:
  1. Cancels any pending task.
  2. Schedules a new task via `ScheduledExecutorService`.
  3. Task logic:
     - Checks buffer.
     - Takes **last** message (latest).
     - Sets `isExecuting = true`.
     - Calls `executeWorkflowAsync`.

### `executeWorkflowAsync`
- **Description**: Runs the workflow service asynchronously.
- **Logic**:
  1. Calls `workflowService.executeForSession`.
  2. Sends AI reply via `messageGateway` if successful.
  3. Finally calls `handleQueuedMessages` to process backlog.

### `handleQueuedMessages`
- **Description**: Processes queued messages after a workflow finishes.
- **Logic**:
  1. Applies "Post-Execution Debounce": checks timestamps of queued messages.
  2. Skips messages that are "too close" to the previous one (based on debounce window).
  3. Picks the next valid message and triggers `executeWorkflowAsync`.

## Dependency Graph
- **Injected Services**:
  - `AiWorkflowService`: The workflow engine.
  - `SessionMessageGateway`: For sending replies.
  - `SqsDelayService`: For long-running scheduled tasks.

## Usage Guide
```java
// Called by WebSocket/Controller when user sends a message
scheduler.submitMessage(sessionId, "Hello", messageId);
```
