# VariableNode

## 1. 类档案 (Class Profile)
- **功能定义**：变量管理节点。用于设置、追加或删除工作流上下文中的变量。
- **注解与配置**：
  - `@LiteflowComponent("variable")`: 注册为 LiteFlow 组件。
- **继承/实现**：继承自 `BaseWorkflowNode`。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `process` | - | 支持两种模式：<br>1. **提取模式**：从 JSON 输出中提取字段 (`extractFieldFromJson`) 并存入变量。<br>2. **操作模式**：直接 Set/Append/Delete 变量（支持模板值）。 | |
| `extractFieldFromJson` | In: `json`, `path` | 强大的 JSON 路径解析，支持数组索引（如 `items[0].name`）和嵌套对象。 | 手动实现的路径解析器，未依赖第三方 JsonPath 库。 |

## 3. 依赖全景 (Dependency Graph)
- **`ObjectMapper`**: 解析 JSON。

## 4. 调用指南 (Usage Guide)
用于在不同节点间传递数据，或提取 API 节点的返回结果供后续使用。
