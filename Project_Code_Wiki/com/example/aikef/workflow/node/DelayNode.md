# DelayNode

## 1. 类档案 (Class Profile)
- **功能定义**：异步延迟节点。将任务挂起指定时间后，触发另一个工作流。
- **注解与配置**：
  - `@Component("delay")`: 注册为 Spring 组件（注意：这里不是 LiteflowComponent，可能通过特殊方式调用，或者代码中有误，通常应为 `@LiteflowComponent`）。
- **继承/实现**：继承自 `BaseWorkflowNode`。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `process` | - | 1. 读取 `delayMinutes` 和 `targetWorkflowId`。<br>2. 渲染 `inputData` 模板。<br>3. 构建任务数据（包含当前 session 信息）。<br>4. 调用 `scheduler.scheduleDelayTask` 将任务推送到延迟队列（SQS）。 | 限制最大延迟时间为 24 小时。 |

## 3. 依赖全景 (Dependency Graph)
- **`WorkflowExecutionScheduler`**: 负责与消息队列（SQS）交互，投递延迟消息。

## 4. 调用指南 (Usage Guide)
用于实现 "24小时后发送跟进邮件" 等场景。
**注意**：此节点执行后，当前工作流继续执行，但会产生一个未来的副作用（触发另一个工作流）。
