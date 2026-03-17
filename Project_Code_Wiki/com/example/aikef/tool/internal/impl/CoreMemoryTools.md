# CoreMemoryTools

## 1. 类档案 (Class Profile)
- **功能定义**：提供高级 Agent 的长期记忆实时管理能力，支持把用户自然语言要求转成记忆列表更新。
- **注解与配置**：
  - `@Component`：注册为 Spring Bean。
  - `@Tool`：将方法暴露为可被 LLM 调用的内部工具。
- **继承/实现**：无继承。

## 2. 核心逻辑详解 (Logic Deep Dive)
1. 解析输入参数 `customerId` 与 `userRequirement`，优先使用显式入参，缺失时从 `WorkflowContext` 回退。
2. 从 `CustomerCoreMemoryRepository` 加载当前客户记忆，不存在则初始化新记录。
3. 读取历史 `memories` 列表并调用 LLM，让模型基于新要求输出更新后的完整列表；调用时明确传入非空 `userMessage`，避免底层消息构造报 `text cannot be null or blank`。
4. 解析模型返回 JSON 的 `updated_memories`，失败则降级为本地规则兜底：先清洗空白/重复记忆，再按“事实前缀”（如“助手的名字是”）替换冲突旧值，避免仅追加导致脏记忆残留。
5. 重新生成 `summary` 并保存，供 `AdvancedAgentNode` 在 System Prompt 注入；摘要调用同样使用非空 `userMessage`，避免二次异常。
6. 输出关键可观测日志：入口请求、更新前后记忆条数、摘要生成结果与最终落库信息。

## 3. 依赖全景 (Dependency Graph)
- **`CustomerCoreMemoryRepository`**：读取与持久化长期记忆实体。
- **`LangChainChatService`**：执行“记忆编辑”与“摘要生成”两类 LLM 调用。
- **`ObjectMapper`**：处理记忆列表与模型 JSON 结果序列化/反序列化。
- **`WorkflowContext`**：在工具调用时提供上下文 customerId。

## 4. 调用指南 (Usage Guide)
```java
String result = coreMemoryTools.manageLongTermMemory(
        "8d704650-f7c8-4d7b-8ea7-8c6c8a6f5e00",
        "我叫Tom，不要频繁提醒我工作进度，叫我Tom",
        workflowContext
);
```

## 5. 架构师备注 (Architect's Notes)
- 该工具以“完整记忆列表重写”方式工作，避免外部调用方处理字段键值对。
- 该工具既可存储用户画像事实，也可存储用户对 Agent 行为的约束指令。
- 当 LLM 返回结构异常时采用降级策略，保证记忆更新不中断，并优先避免冲突事实并存（如 Alice/Tom 同时存在）。
