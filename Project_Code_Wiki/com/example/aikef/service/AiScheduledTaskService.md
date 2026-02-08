# AiScheduledTaskService

## 1. Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: AI 定时任务服务。负责管理和执行基于 Cron 表达式的定时任务，这些任务通常用于触发 AI 工作流以主动联系客户（如定期回访、节日问候）。

## 2. Method Deep Dive

### `createTask`
- **Signature**: `public ScheduledTaskDto createTask(SaveScheduledTaskRequest request)`
- **Description**: 创建一个新的定时任务。计算首次运行时间并保存。

### `scheduleTask`
- **Signature**: `public void scheduleTask()`
- **Annotations**: `@Scheduled(fixedDelay = 60000)`
- **Description**: 定时调度器，每分钟执行一次。检查并执行所有到期的任务。
- **Logic**:
  1. 查询所有 `enabled=true` 且 `nextRunAt <= now` 的任务。
  2. 遍历任务调用 `executeTask`。
  3. 更新任务的 `lastRunAt` 和 `nextRunAt`。

### `executeTask`
- **Signature**: `public void executeTask(AiScheduledTask task)`
- **Description**: 执行单个定时任务的核心逻辑。
- **Logic**:
  1. 解析目标客户集合（特定客户或特定角色）。
  2. 遍历每个目标客户，异步执行以下步骤：
     - 查找客户最新的活跃会话。
     - 准备工作流变量（customerId, taskName 等）。
     - 调用 `workflowService.executeWorkflow` 启动关联的 AI 工作流。
     - 如果工作流返回了回复内容，通过 `sessionMessageGateway` 发送 AI 消息。

### `generateCronExpression`
- **Signature**: `private String generateCronExpression(SaveScheduledTaskRequest.ScheduleConfig config)`
- **Description**: 将前端友好的调度配置转换为标准的 Cron 表达式。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `AiScheduledTaskRepository`: 任务存储。
  - `AiWorkflowService`: 执行 AI 工作流。
  - `SessionMessageGateway`: 发送消息。
  - `CustomerRepository`, `SpecialCustomerRepository`: 解析目标客户。

## 4. Usage Guide
### 场景：每日早报
管理员创建一个 "每日早报" 任务，配置为每天早上 8:00 执行，目标群体为 "VIP 客户"。关联一个 "新闻摘要" 工作流。
系统会在每天 8:00 自动触发该任务，查找所有 VIP 客户，并为每个客户运行工作流，最后将生成的早报内容发送到客户的聊天窗口。
