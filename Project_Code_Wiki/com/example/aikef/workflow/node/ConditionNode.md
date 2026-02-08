# ConditionNode

## 1. 类档案 (Class Profile)
- **功能定义**：逻辑判断节点（Switch 类型），支持多路分支条件判断（IF / ELSE IF / ELSE）。
- **注解与配置**：
  - `@LiteflowComponent("condition")`: 注册为 LiteFlow 组件。
- **继承/实现**：继承自 `NodeSwitchComponent`。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `processSwitch` | Out: `String` | 1. 遍历配置的 `conditions` 列表。<br>2. 解析 `sourceValue`（支持变量）和 `inputValue`。<br>3. 执行比较 (`checkCondition`)。<br>4. **匹配成功**：返回 `tag:conditionId`。<br>5. **无匹配**：返回 `tag:else`。 | 支持短路逻辑：找到第一个满足的条件即停止并返回。 |
| `checkCondition` | In: `source`, `type`, `target` | 实现具体比较逻辑：<br>- 字符串：equals, contains, startsWith...<br>- 数值：gt, lt, gte, lte...<br>- 空值：isEmpty, isNotEmpty | 数值比较时自动处理 `NumberFormatException`。 |

## 3. 依赖全景 (Dependency Graph)
- **`TemplateEngine`**: 解析条件中的变量（如 `{{sys.lastOutput}}`）。

## 4. 调用指南 (Usage Guide)
**LiteFlow EL**:
```java
SWITCH(condition_node).TO(branch_a, branch_b, branch_default)
```
LiteFlow 会根据 `processSwitch` 返回的 tag 自动路由到 TO 列表中对应的节点。
