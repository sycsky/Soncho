# AiWorkflowService

## 1. 类档案 (Class Profile)
- **功能定义**：AI 工作流核心服务类，负责工作流的创建、更新、执行、暂停恢复以及与 LiteFlow 引擎的集成。
- **注解与配置**：
  - `@Service`: 标记为 Spring 服务组件。
  - `@Transactional(readOnly = true)`: 类级别默认为只读事务，确保数据安全。
  - `@Transactional`: 关键写操作方法上覆盖为读写事务。
- **继承/实现**：无显式继承，作为核心业务 Service 独立存在。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `createWorkflow` | In: `SaveWorkflowRequest`, `agentId`<br>Out: `AiWorkflow` | 1. 验证名称唯一性。<br>2. 验证 ReactFlow 结构有效性。<br>3. 调用 Converter 将 JSON 转换为 LiteFlow EL 表达式。<br>4. 保存工作流实体。<br>5. 绑定分类（如果有）。 | 校验空模板；异常抛出 `IllegalArgumentException`。 |
| `updateWorkflow` | In: `workflowId`, `SaveWorkflowRequest`<br>Out: `AiWorkflow` | 1. 检查工作流是否存在。<br>2. 检查名称冲突。<br>3. 重新验证并转换 EL 表达式。<br>4. 更新版本号并保存。<br>5. 更新分类绑定。 | 自动增加版本号。 |
| `executeWorkflow` | In: `workflowId`, `sessionId`, `userMessage`, `variables`<br>Out: `WorkflowExecutionResult` | 1. 获取工作流实体。<br>2. 调用 `executeWorkflowInternal` 执行。 | 事务性执行。 |
| `executeForSession` | In: `sessionId`, `userMessage`, `messageId`<br>Out: `WorkflowExecutionResult` | **优先级策略执行**：<br>1. **检查暂停状态**：如果有未完成的工具调用/追问，调用 `resumeFromPausedState` 恢复。<br>2. **检查 AgentSession**：如果是 Agent 模式，执行 Agent 绑定的工作流。<br>3. **检查分类绑定**：根据会话分类查找工作流。<br>4. **默认工作流**：兜底执行默认工作流。 | 包含复杂的优先级路由逻辑；处理未找到工作流的情况。 |
| `resumeFromPausedState` | In: `pausedState`, `userMessage`, `session`, `messageId`<br>Out: `WorkflowExecutionResult` | 1. 反序列化上下文 (`WorkflowContext`)。<br>2. 恢复变量、节点配置、路由表。<br>3. 注入 `_resumeFromPause` 等系统变量。<br>4. 调用 LiteFlow 执行子链 (`subChainId`)。<br>5. 处理执行结果（完成或再次暂停）。 | 深度恢复上下文状态；处理 `WorkflowPausedException`。 |
| `registerSubChains` | In: `AiWorkflow` | 1. 解析 `subChainsJson`。<br>2. 根据依赖关系排序（拓扑排序）。<br>3. 动态注册到 LiteFlow 引擎 (`LiteFlowChainELBuilder`)。 | 解决子链依赖顺序问题；避免循环依赖死锁。 |
| `executeSubChain` | In: `chainId`, `WorkflowContext`<br>Out: `WorkflowExecutionResult` | 1. 直接执行指定的 LiteFlow Chain。<br>2. 捕获暂停异常并封装返回。 | 用于子链独立测试或恢复执行。 |
| `bindWorkflowToCategories` | In: `workflowId`, `categoryIds` | 1. 检查分类是否已被其他工作流占用（排他性）。<br>2. 删除旧绑定。<br>3. 创建新绑定记录。 | 强制校验分类绑定的唯一性。 |

## 3. 依赖全景 (Dependency Graph)

### 依赖的服务/组件 (Injected Beans)
- **`FlowExecutor`** (`LiteFlow`): 核心依赖，用于执行 EL 表达式定义的工作流。
- **`ReactFlowToLiteflowConverter`**: 将前端 ReactFlow JSON 转换为后端 LiteFlow EL 表达式。
- **`WorkflowPauseService`**: 处理工作流的暂停、状态存储和上下文序列化。
- **`WorkflowStatusService`**: 推送 WebSocket 状态更新（如 "Thinking...", "Completed"）。
- **`AgentService` / `AgentSessionRepository`**: 处理 Agent 模式下的工作流执行。
- **Repositories**:
  - `AiWorkflowRepository`: 工作流元数据存储。
  - `WorkflowExecutionLogRepository`: 执行日志记录。
  - `ChatSessionRepository`: 会话上下文获取。
  - `WorkflowCategoryBindingRepository` / `SessionCategoryRepository`: 分类绑定管理。

### 使用的工具
- **`ObjectMapper`** (`Jackson`): 这里的配置为 `FAIL_ON_EMPTY_BEANS=false`，用于 JSON 序列化/反序列化（节点配置、上下文、日志）。
- **`LiteFlowChainELBuilder`**: 动态构建和注册 LiteFlow 链。

### 配置依赖
- 无直接 `@Value` 配置注入，但依赖 `LiteFlow` 的全局配置。

## 4. 调用指南 (Usage Guide)

### 实例化方式
通过 Spring 依赖注入获取：
```java
@Autowired
private AiWorkflowService aiWorkflowService;
```

### 代码示例
**场景：根据用户会话自动执行合适的工作流**

```java
@Autowired
private AiWorkflowService aiWorkflowService;

public void handleUserMessage(UUID sessionId, String message) {
    try {
        // 自动路由并执行工作流
        WorkflowExecutionResult result = aiWorkflowService.executeForSession(
            sessionId, 
            message, 
            UUID.randomUUID() // messageId
        );

        if (result.success()) {
            System.out.println("AI 回复: " + result.reply());
            
            if (result.isNeedHumanTransfer()) {
                // 处理转人工逻辑
            }
        } else if (result.isPaused()) {
            // 处理暂停（如等待工具输入）
            System.out.println("工作流暂停: " + result.errorMessage());
        } else {
            System.err.println("执行失败: " + result.errorMessage());
        }
    } catch (Exception e) {
        log.error("系统异常", e);
    }
}
```
