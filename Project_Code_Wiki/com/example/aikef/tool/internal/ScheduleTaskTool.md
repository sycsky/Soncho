# ScheduleTaskTool

## 1. 类档案 (Class Profile)
- **包路径**: `com.example.aikef.tool.internal`
- **类型**: 内部工具组件（`@Component`）
- **核心职责**: 接收用户自然语言的定时要求，结合用户长期记忆做 LLM 决策，自动规划主提醒与中途提醒，并投递为延迟任务。

## 2. 核心逻辑详解 (Logic Deep Dive)
1. 校验 `WorkflowContext` 与目标执行时间，确保任务与客户上下文可用且执行时间晚于当前时间。
2. 读取 `CustomerCoreMemory.summary`，作为用户偏好与行为约束输入。
3. 调用 `LangChainChatService.simpleChat` 生成提醒规划 JSON，包含主提醒内容与可选中途提醒列表。
4. 对提醒计划做过滤与去重，仅保留「当前时间之后且主提醒时间之前」的中途提醒，且最多三条。
5. 为每条提醒构造统一 payload（含 `executeAt`、`reminderType`、`originTaskDescription` 等字段），按 SQS 限制先投递首段延迟。

## 3. 依赖全景 (Dependency Graph)
- **SqsDelayService**: 负责延迟消息投递。
- **CustomerCoreMemoryRepository**: 加载客户长期记忆摘要。
- **LangChainChatService**: 执行提醒规划与文案生成。
- **ObjectMapper**: 负责提醒 payload 与 LLM JSON 解析。

## 4. 调用指南 (Usage Guide)
```java
String result = scheduleTaskTool.scheduleTask(
        "三天后提醒我准备项目复盘材料",
        "2026-03-13T10:00:00",
        workflowContext
);
```

## 5. 架构师备注 (Architect's Notes)
- 该工具负责「提醒策略决策层」，将固定时间提醒升级为“主提醒 + 可选中途提醒”的人性化策略。
- 提醒规划提示词保持场景通用，不绑定会议等单一业务，适用于待办、跟进、回访等任务。
- 长时间任务通过 `executeAt` + 分段延迟投递机制实现跨越 SQS 单次延迟上限。
