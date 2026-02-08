# ToolNode

## 1. 类档案 (Class Profile)
- **功能定义**：工具执行节点（Switch 类型）。根据上下文中的参数调用后端工具，并根据执行结果（成功/失败）路由到不同的后续分支。
- **注解与配置**：
  - `@LiteflowComponent("tool")`: 注册为 LiteFlow 组件。
- **继承/实现**：继承自 `NodeSwitchComponent` (LiteFlow 分支节点)。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `processSwitch` | Out: `String` (Tag) | 1. 解析配置获取 `toolId` 或 `toolName`。<br>2. 从上下文 `toolsParams` 中获取参数。<br>3. **参数校验**：检查所有必填参数是否存在。<br>4. **执行工具**：调用 `toolService.executeTool`。<br>5. **路由决策**：<br>   - 成功：返回 `tag:executed` 对应的节点 ID。<br>   - 失败/参数缺失：返回 `tag:not_executed` 对应的节点 ID。 | 返回值格式 `tag:nodeId` 用于 LiteFlow 的 `SWITCH...TO` 路由选择。 |
| `getToolParameters` | In: `AiTool` | 解析工具的 JSON Schema 定义，获取参数列表。 | 用于运行时校验必填参数。 |

## 3. 依赖全景 (Dependency Graph)
- **`AiToolService`**: 执行具体的工具逻辑（反射调用或脚本执行）。
- **`AiToolRepository`**: 查询工具定义。
- **`WorkflowContext`**: 获取 `toolParams`（通常由上一个 Parameter Extraction 节点或 LLM 节点产生）。

## 4. 调用指南 (Usage Guide)
**场景**：在 LLM 提取参数后，显式调用工具。
1.  前置节点（如 ParamExtractNode）提取参数存入上下文。
2.  ToolNode 配置指定工具 ID。
3.  根据执行结果连接两条分支：Success -> 下一步，Fail -> 错误处理。

**LiteFlow EL**:
```java
SWITCH(tool_node).TO(success_node, fail_node)
```
