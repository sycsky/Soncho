# ApiNode

## 1. 类档案 (Class Profile)
- **功能定义**：通用 API 调用节点，支持发送 HTTP 请求（GET/POST 等）并将响应数据存入上下文变量。
- **注解与配置**：
  - `@LiteflowComponent("api")`: 注册为 LiteFlow 组件。
- **继承/实现**：继承自 `BaseWorkflowNode`。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `process` | - | 1. 读取 URL、Method、Headers、Body 配置。<br>2. **变量替换**：处理配置中的 `{{var}}` 模板变量。<br>3. 发送 HTTP 请求 (`RestTemplate`)。<br>4. **响应处理**：<br>   - 可选：使用 JSONPath (`responseMapping`) 提取特定字段。<br>   - 可选：将结果保存到指定变量 (`saveToVariable`)。<br>5. 设置节点输出。 | 支持复杂的 JSON Body 构造和 Header 设置。 |
| `extractJsonPath` | In: `json`, `path` | 简单的 JSON 路径提取逻辑（支持点号分隔，如 `data.items`）。 | 轻量级实现，不依赖复杂库。 |

## 3. 依赖全景 (Dependency Graph)
- **`RestTemplate`**: Spring 的 HTTP 客户端。
- **`ObjectMapper`**: JSON 序列化/反序列化。

## 4. 调用指南 (Usage Guide)
**配置示例**：
```json
{
  "url": "https://api.example.com/orders/{{query}}",
  "method": "GET",
  "saveToVariable": "orderInfo",
  "responseMapping": "data.order_details"
}
```
