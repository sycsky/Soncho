# SqsDelayService

## 1. Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `Slf4j`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 基于 AWS SQS 的延迟任务服务。用于实现 AI 工作流中的 "Delay"（延迟等待）节点功能，支持在指定时间后唤醒工作流继续执行。

## 2. Method Deep Dive

### `sendDelayMessage`
- **Signature**: `public void sendDelayMessage(Map<String, Object> taskData, int delayMinutes)`
- **Description**: 发送延迟消息到 SQS 队列。
- **Parameters**:
  - `taskData`: 包含工作流恢复执行所需的上下文（sessionId, workflowId, inputData 等）。
  - `delayMinutes`: 延迟时间（分钟）。
- **Logic**: 设置 SQS 消息的 `DelaySeconds` 属性。

### `pollMessages`
- **Signature**: `private void pollMessages()`
- **Description**: 后台线程轮询 SQS 队列。
- **Logic**:
  1. 使用 Long Polling 拉取消息。
  2. 调用 `processMessage` 处理。

### `processMessage`
- **Signature**: `private void processMessage(Message message)`
- **Description**: 处理到期的延迟消息。
- **Logic**:
  1. 解析消息体中的任务数据。
  2. 验证工作流有效性。
  3. 调用 `workflowService.executeWorkflow` 恢复工作流执行。
  4. 如果工作流产生回复，通过 `messageGateway` 发送。
  5. 删除 SQS 消息。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `SqsClient`: AWS SDK 客户端。
  - `AiWorkflowService`: 工作流执行。
  - `SessionMessageGateway`: 消息发送。

## 4. Usage Guide
### 场景：24小时跟进
在 AI 工作流中配置一个 "Delay 24h" 节点。
1. 工作流执行到该节点时，调用 `sendDelayMessage`，挂起当前执行。
2. 24小时后，SQS 消息可见，`pollMessages` 拉取到消息。
3. 服务自动唤醒工作流，执行后续节点（如发送 "您昨天的问题解决了吗？" 的跟进消息）。
