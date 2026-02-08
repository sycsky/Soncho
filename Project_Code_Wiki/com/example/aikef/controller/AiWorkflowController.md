# AiWorkflowController

## 1. 类档案 (Class Profile)
- **功能定义**：AI 工作流的 RESTful API 入口，提供工作流的管理（CRUD）、执行、测试、验证以及分类绑定等 HTTP 接口。
- **注解与配置**：
  - `@RestController`: 标记为 REST 控制器，返回 JSON 响应。
  - `@RequestMapping("/api/v1/ai-workflows")`: 定义基础路由路径。
- **继承/实现**：无。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `createWorkflow` | In: `SaveWorkflowRequest`, `Authentication`<br>Out: `AiWorkflowDto` | 1. 从 Authentication 获取当前操作 Agent ID。<br>2. 调用 Service 创建工作流。<br>3. 转换为 DTO 返回。 | 响应状态码 201 Created。 |
| `executeForSession` | In: `sessionId`, `userMessage`, `messageId`<br>Out: `WorkflowExecutionResultDto` | 1. 解析参数。<br>2. 调用 `workflowService.executeForSession` 进行智能路由执行。<br>3. 封装执行结果（包含回复、状态、人工转接标记）。 | 核心业务接口，用于聊天窗口自动触发。 |
| `validateWorkflow` | In: `ValidateWorkflowRequest`<br>Out: `ValidationResponse` | 1. 调用 `converter.validate` 校验 ReactFlow JSON 结构。<br>2. 返回验证结果和错误列表。 | 纯计算逻辑，不涉及数据库写操作。 |
| `previewEl` | In: `ValidateWorkflowRequest`<br>Out: `PreviewElResponse` | 1. 尝试将前端 JSON 转换为 LiteFlow EL。<br>2. 成功返回 EL 字符串，失败返回错误信息。 | 用于前端调试 EL 生成。 |
| `testWorkflow` | In: `workflowId`, `ExecuteWorkflowRequest`<br>Out: `WorkflowExecutionResultDto` | 1. 如果无 SessionId，创建临时测试会话。<br>2. 调用 `workflowService.testWorkflow`（不记录正式日志）。 | 支持临时会话测试。 |
| `setDebounceSeconds` | In: `seconds`<br>Out: `Map` | 1. 校验时间范围 (0-300秒)。<br>2. 更新调度器配置。 | 动态调整系统参数。 |

## 3. 依赖全景 (Dependency Graph)

### 依赖的服务/组件 (Injected Beans)
- **`AiWorkflowService`**: 核心业务逻辑委托对象。
- **`ReactFlowToLiteflowConverter`**: 用于 API 层的结构验证和 EL 预览。
- **`WorkflowTestService`**: 用于创建测试会话。
- **`WorkflowExecutionScheduler`**: 管理工作流执行的调度配置（如防抖）。

### 使用的工具
- **`ObjectMapper`**: 用于 DTO 序列化处理（尽管 Spring MVC 自带，但此处显式定义了用于特定转换）。

### 配置依赖
- 无直接配置注入。

## 4. 调用指南 (Usage Guide)

### 实例化方式
Spring 容器自动管理，作为 Web 组件。

### 代码示例
**前端调用示例 (JavaScript/Fetch)**

```javascript
// 执行工作流
const response = await fetch('/api/v1/ai-workflows/execute-for-session?sessionId=123&userMessage=hello', {
    method: 'POST'
});
const result = await response.json();

if (result.success) {
    console.log("AI Reply:", result.reply);
}
```

**后端内部调用**
通常不直接调用 Controller，而是注入 `AiWorkflowService`。
