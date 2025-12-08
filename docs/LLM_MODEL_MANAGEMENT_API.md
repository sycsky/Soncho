# LLM 模型管理 API 文档

## 概述

本文档描述 LLM（大语言模型）配置管理的 CRUD API 接口。系统支持多种模型提供商，包括 OpenAI、Azure OpenAI、Ollama、智谱 AI 等。

## 模型类型

| 类型 | 说明 |
|------|------|
| CHAT | 聊天/对话模型，用于与用户进行对话交互 |
| EMBEDDING | 嵌入/向量化模型，用于知识库的文本向量化 |

**重要规则**：
- 默认模型只能是 `CHAT` 类型
- `EMBEDDING` 类型的模型不能设为默认模型
- 系统中只能有一个默认模型

## 支持的提供商

| 提供商代码 | 显示名称 | 默认 Base URL |
|-----------|---------|---------------|
| OPENAI | OpenAI | https://api.openai.com/v1 |
| AZURE_OPENAI | Azure OpenAI | - |
| OLLAMA | Ollama (本地) | http://localhost:11434 |
| ZHIPU | 智谱 AI | https://open.bigmodel.cn/api/paas/v4 |
| DASHSCOPE | 阿里云 DashScope | https://dashscope.aliyuncs.com/compatible-mode/v1 |
| MOONSHOT | Moonshot (月之暗面) | https://api.moonshot.cn/v1 |
| DEEPSEEK | DeepSeek | https://api.deepseek.com/v1 |
| CUSTOM | 自定义 (OpenAI 兼容) | - |

---

## API 接口

### 1. 获取所有模型

获取系统中配置的所有模型列表。

```http
GET /api/v1/llm-models
```

**响应示例：**

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "GPT-4",
    "code": "gpt-4",
    "provider": "OPENAI",
    "modelName": "gpt-4",
    "modelType": "CHAT",
    "baseUrl": "https://api.openai.com/v1",
    "azureDeploymentName": null,
    "defaultTemperature": 0.7,
    "defaultMaxTokens": 2000,
    "contextWindow": 8192,
    "inputPricePer1k": 0.03,
    "outputPricePer1k": 0.06,
    "supportsFunctions": true,
    "supportsVision": false,
    "enabled": true,
    "isDefault": true,
    "sortOrder": 1,
    "description": "OpenAI GPT-4 模型",
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
]
```

---

### 2. 获取启用的模型

获取所有已启用的模型列表。

```http
GET /api/v1/llm-models/enabled
```

---

### 3. 获取模型详情

根据模型 ID 获取详细信息。

```http
GET /api/v1/llm-models/{modelId}
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| modelId | UUID | 模型 ID |

---

### 4. 创建模型

创建新的 LLM 模型配置。

```http
POST /api/v1/llm-models
Content-Type: application/json
```

**请求体：**

```json
{
  "name": "GPT-4",
  "code": "gpt-4",
  "provider": "OPENAI",
  "modelName": "gpt-4",
  "modelType": "CHAT",
  "baseUrl": "https://api.openai.com/v1",
  "apiKey": "sk-xxx",
  "azureDeploymentName": null,
  "defaultTemperature": 0.7,
  "defaultMaxTokens": 2000,
  "contextWindow": 8192,
  "inputPricePer1k": 0.03,
  "outputPricePer1k": 0.06,
  "supportsFunctions": true,
  "supportsVision": false,
  "enabled": true,
  "isDefault": true,
  "sortOrder": 1,
  "description": "OpenAI GPT-4 模型",
  "extraConfig": null
}
```

**关于 isDefault 字段：**
- 设置 `isDefault: true` 时，会自动取消之前的默认模型
- EMBEDDING 类型的模型不能设为默认（会返回 400 错误）
- 设置为默认模型时会同时将该模型启用

**请求字段说明：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | ✅ | 模型显示名称 |
| code | string | ✅ | 模型唯一编码（不可重复） |
| provider | string | ✅ | 提供商代码 |
| modelName | string | ✅ | API 模型标识（如 gpt-4, gpt-3.5-turbo） |
| modelType | string | ❌ | 模型类型：CHAT 或 EMBEDDING，默认 CHAT |
| baseUrl | string | ❌ | API Base URL |
| apiKey | string | ❌ | API Key（敏感信息，不会在响应中返回） |
| azureDeploymentName | string | ❌ | Azure 部署名称（Azure OpenAI 专用） |
| defaultTemperature | number | ❌ | 默认温度参数（0-2），默认 0.7 |
| defaultMaxTokens | integer | ❌ | 默认最大 Token 数，默认 2000 |
| contextWindow | integer | ❌ | 上下文窗口大小，默认 4096 |
| inputPricePer1k | number | ❌ | 每千 Token 输入价格（美元） |
| outputPricePer1k | number | ❌ | 每千 Token 输出价格（美元） |
| supportsFunctions | boolean | ❌ | 是否支持函数调用，默认 false |
| supportsVision | boolean | ❌ | 是否支持视觉（图片输入），默认 false |
| enabled | boolean | ❌ | 是否启用，默认 true |
| isDefault | boolean | ❌ | 是否设为默认模型，设为 true 会自动取消原默认模型 |
| sortOrder | integer | ❌ | 排序顺序，默认 0 |
| description | string | ❌ | 模型描述 |
| extraConfig | string | ❌ | 额外配置（JSON 格式） |

**响应：** 返回创建的模型对象（HTTP 201）

---

### 5. 更新模型

更新现有的模型配置。

```http
PUT /api/v1/llm-models/{modelId}
Content-Type: application/json
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| modelId | UUID | 模型 ID |

**请求体：** 同创建模型

---

### 6. 删除模型

删除指定的模型配置。

```http
DELETE /api/v1/llm-models/{modelId}
```

**响应：** HTTP 204 No Content

---

### 7. 启用/禁用模型

切换模型的启用状态。

```http
PATCH /api/v1/llm-models/{modelId}/toggle?enabled={true|false}
```

**参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| modelId | UUID | 模型 ID |
| enabled | boolean | true=启用, false=禁用 |

**响应示例：**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "GPT-4",
  "enabled": false,
  ...
}
```

---

### 8. 设置默认模型

将指定模型设置为默认模型。

```http
POST /api/v1/llm-models/{modelId}/set-default
```

**重要规则：**
- 系统中只能有一个默认模型
- 设置新的默认模型会自动取消原有默认模型
- **EMBEDDING 类型的模型不能设为默认模型**（会返回 400 错误）
- 设置默认模型会同时将该模型启用

**成功响应：** 返回更新后的模型对象

**错误响应（EMBEDDING 类型）：**

```json
{
  "error": "EMBEDDING 类型的模型不能设为默认模型，只有 CHAT 类型可以"
}
```

---

### 9. 获取提供商列表

获取系统支持的所有模型提供商。

```http
GET /api/v1/llm-models/providers
```

**响应示例：**

```json
[
  {
    "code": "OPENAI",
    "name": "OpenAI",
    "defaultBaseUrl": "https://api.openai.com/v1"
  },
  {
    "code": "AZURE_OPENAI",
    "name": "Azure OpenAI",
    "defaultBaseUrl": null
  },
  {
    "code": "OLLAMA",
    "name": "Ollama (本地)",
    "defaultBaseUrl": "http://localhost:11434"
  }
]
```

---

### 10. 测试模型连接

测试模型配置是否正确，能否正常调用。

```http
POST /api/v1/llm-models/{modelId}/test
```

**响应示例（成功）：**

```json
{
  "success": true,
  "response": "测试成功",
  "durationMs": 1234,
  "error": null
}
```

**响应示例（失败）：**

```json
{
  "success": false,
  "response": null,
  "durationMs": 5000,
  "error": "Connection timeout"
}
```

---

### 11. 清除模型缓存

清除所有模型的运行时缓存。

```http
POST /api/v1/llm-models/clear-cache
```

**响应：** HTTP 204 No Content

---

## 使用示例

### 创建 OpenAI GPT-4 模型并设为默认

```bash
curl -X POST http://localhost:8080/api/v1/llm-models \
  -H "Content-Type: application/json" \
  -d '{
    "name": "GPT-4",
    "code": "gpt-4",
    "provider": "OPENAI",
    "modelName": "gpt-4",
    "modelType": "CHAT",
    "baseUrl": "https://api.openai.com/v1",
    "apiKey": "sk-xxx",
    "defaultTemperature": 0.7,
    "defaultMaxTokens": 4000,
    "contextWindow": 8192,
    "supportsFunctions": true,
    "isDefault": true
  }'
```

> **说明**：当 `isDefault: true` 时，会自动取消之前的默认模型。

### 创建 Embedding 模型

```bash
curl -X POST http://localhost:8080/api/v1/llm-models \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Text Embedding 3 Small",
    "code": "text-embedding-3-small",
    "provider": "OPENAI",
    "modelName": "text-embedding-3-small",
    "modelType": "EMBEDDING",
    "baseUrl": "https://api.openai.com/v1",
    "apiKey": "sk-xxx",
    "contextWindow": 8191
  }'
```

### 设置默认模型（方式一：单独接口）

```bash
curl -X POST http://localhost:8080/api/v1/llm-models/{modelId}/set-default
```

### 设置默认模型（方式二：更新时设置）

```bash
curl -X PUT http://localhost:8080/api/v1/llm-models/{modelId} \
  -H "Content-Type: application/json" \
  -d '{
    "name": "GPT-4",
    "code": "gpt-4",
    "provider": "OPENAI",
    "modelName": "gpt-4",
    "modelType": "CHAT",
    "isDefault": true
  }'
```

> **说明**：更新模型时设置 `isDefault: true`，会自动取消之前的默认模型。

### 测试模型

```bash
curl -X POST http://localhost:8080/api/v1/llm-models/{modelId}/test
```

---

## 注意事项

1. **API Key 安全**：API Key 不会在响应中返回，只在创建/更新时设置
2. **默认模型限制**：只有 CHAT 类型的模型可以设为默认模型
3. **默认模型自动切换**：创建或更新模型时设置 `isDefault: true`，会自动取消之前的默认模型
4. **唯一编码**：模型编码（code）必须全局唯一
5. **缓存管理**：更新或删除模型后会自动清除相关缓存
6. **模型类型**：创建后可以更新模型类型，但如果是默认模型且要改为 EMBEDDING，需要先取消默认状态

## 相关文件

- 控制器：`com.example.aikef.controller.LlmModelController`
- 服务：`com.example.aikef.llm.LlmModelService`
- 模型实体：`com.example.aikef.model.LlmModel`
- 请求 DTO：`com.example.aikef.dto.request.SaveLlmModelRequest`
- 响应 DTO：`com.example.aikef.dto.LlmModelDto`

