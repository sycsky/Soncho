# ReplyNode

## 1. 类档案 (Class Profile)
- **功能定义**：简单回复节点。直接设置固定的回复内容（支持模板变量），通常作为工作流的结束输出。
- **注解与配置**：
  - `@LiteflowComponent("reply")`: 注册为 LiteFlow 组件。
- **继承/实现**：继承自 `BaseWorkflowNode`。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `process` | - | 1. 获取配置的 `text`。<br>2. **模板渲染**：替换 `{{var}}` 变量。<br>3. 如果未配置 text，默认使用上一个节点的输出。<br>4. 调用 `messageGateway` 发送消息。 | 最基础的输出节点。 |

## 3. 依赖全景 (Dependency Graph)
- **`SessionMessageGateway`**: 发送消息。

## 4. 调用指南 (Usage Guide)
用于固定话术回复，如 "欢迎咨询"、"再见" 等。
