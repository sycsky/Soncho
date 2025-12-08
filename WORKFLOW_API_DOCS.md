# AI 工作流模块接口文档

## 目录

- [概述](#概述)
- [工作流管理 API](#工作流管理-api)
- [工作流执行 API](#工作流执行-api)
- [LLM 模型管理 API](#llm-模型管理-api)
- [数据结构说明](#数据结构说明)
- [节点类型配置](#节点类型配置)
- [前端集成指南](#前端集成指南)
- [错误码说明](#错误码说明)

---

## 概述

工作流模块基于 LiteFlow 引擎，支持可视化编排 AI 客服对话流程。前端使用 ReactFlow 进行工作流编辑，后端负责存储、转换和执行工作流。

### 基础信息

- **Base URL**: `/api/v1`
- **认证方式**: Bearer Token
- **Content-Type**: `application/json`

### 请求头

```
Authorization: Bearer {agent_token}
Content-Type: application/json
```

---

## 工作流管理 API

### 1. 获取所有工作流

获取系统中所有工作流列表。

**请求**
```
GET /api/v1/ai-workflows
```

**响应**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "智能客服工作流",
    "description": "处理客户咨询的基础工作流",
    "nodesJson": "[...]",
    "edgesJson": "[...]",
    "liteflowEl": "start_1, llm_1, reply_1, end_1",
    "version": 1,
    "enabled": true,
    "isDefault": true,
    "createdByAgentId": "agent-uuid",
    "createdByAgentName": "管理员",
    "triggerType": "ALL",
    "triggerConfig": null,
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
]
```

---

### 2. 获取工作流详情

**请求**
```
GET /api/v1/ai-workflows/{workflowId}
```

**路径参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| workflowId | UUID | 是 | 工作流ID |

**响应**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "智能客服工作流",
  "description": "处理客户咨询的基础工作流",
  "nodesJson": "[{\"id\":\"start_1\",\"type\":\"start\",...}]",
  "edgesJson": "[{\"id\":\"e1\",\"source\":\"start_1\",\"target\":\"llm_1\"}]",
  "liteflowEl": "start_1, llm_1, reply_1, end_1",
  "version": 1,
  "enabled": true,
  "isDefault": true,
  "createdByAgentId": "agent-uuid",
  "createdByAgentName": "管理员",
  "triggerType": "ALL",
  "triggerConfig": null,
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

---

### 3. 创建工作流

**请求**
```
POST /api/v1/ai-workflows
```

**请求体**
```json
{
  "name": "智能客服工作流",
  "description": "处理客户咨询的基础工作流",
  "nodesJson": "[{\"id\":\"start_1\",\"type\":\"start\",\"data\":{\"label\":\"开始\"},\"position\":{\"x\":250,\"y\":0}}]",
  "edgesJson": "[{\"id\":\"e1\",\"source\":\"start_1\",\"target\":\"llm_1\"}]",
  "triggerType": "ALL",
  "triggerConfig": null
}
```

**请求体参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| name | string | 是 | 工作流名称（唯一） |
| description | string | 否 | 工作流描述 |
| nodesJson | string | 是 | ReactFlow 节点数据 JSON 字符串 |
| edgesJson | string | 是 | ReactFlow 边数据 JSON 字符串 |
| triggerType | string | 否 | 触发类型：ALL/CATEGORY/KEYWORD |
| triggerConfig | string | 否 | 触发配置 JSON |

**响应**: 201 Created
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "智能客服工作流",
  ...
}
```

---

### 4. 更新工作流

**请求**
```
PUT /api/v1/ai-workflows/{workflowId}
```

**请求体**: 同创建工作流

**响应**: 200 OK

---

### 5. 删除工作流

**请求**
```
DELETE /api/v1/ai-workflows/{workflowId}
```

**响应**: 204 No Content

---

### 6. 启用/禁用工作流

**请求**
```
PATCH /api/v1/ai-workflows/{workflowId}/toggle?enabled=true
```

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| enabled | boolean | 是 | true=启用, false=禁用 |

**响应**: 200 OK

---

### 7. 设置默认工作流

**请求**
```
POST /api/v1/ai-workflows/{workflowId}/set-default
```

**响应**: 200 OK

---

### 8. 验证工作流结构

验证 ReactFlow 数据是否符合规范。

**请求**
```
POST /api/v1/ai-workflows/validate
```

**请求体**
```json
{
  "nodesJson": "[...]",
  "edgesJson": "[...]"
}
```

**响应**
```json
{
  "valid": true,
  "errors": []
}
```

或
```json
{
  "valid": false,
  "errors": [
    "工作流必须有一个起始节点",
    "条件节点 condition_1 至少需要一个分支"
  ]
}
```

---

### 9. 预览 LiteFlow EL 表达式

将 ReactFlow 数据转换为 LiteFlow EL 表达式预览。

**请求**
```
POST /api/v1/ai-workflows/preview-el
```

**请求体**
```json
{
  "nodesJson": "[...]",
  "edgesJson": "[...]"
}
```

**响应**
```json
{
  "success": true,
  "el": "start_1, llm_1, IF(condition_1, THEN(reply_1), THEN(human_1)), end_1",
  "error": null
}
```

---

## 工作流执行 API

### 1. 执行指定工作流

**请求**
```
POST /api/v1/ai-workflows/{workflowId}/execute
```

**请求体**
```json
{
  "sessionId": "会话UUID（可选）",
  "userMessage": "我想退款",
  "variables": {
    "customerName": "张三",
    "orderId": "ORDER123"
  }
}
```

**请求体参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| sessionId | UUID | 否 | 关联的会话ID |
| userMessage | string | 是 | 用户输入消息 |
| variables | object | 否 | 额外变量 |

**响应**
```json
{
  "success": true,
  "reply": "好的，我来帮您处理退款申请。请提供您的订单号。",
  "errorMessage": null,
  "needHumanTransfer": false,
  "nodeDetails": [
    {
      "nodeId": "start_1",
      "nodeType": "start",
      "nodeName": null,
      "input": null,
      "output": "workflow_started",
      "startTime": 1704067200000,
      "endTime": 1704067200001,
      "durationMs": 1,
      "success": true,
      "errorMessage": null
    },
    {
      "nodeId": "llm_1",
      "nodeType": "llm",
      "nodeName": null,
      "input": "我想退款",
      "output": "好的，我来帮您处理退款申请...",
      "startTime": 1704067200001,
      "endTime": 1704067201500,
      "durationMs": 1499,
      "success": true,
      "errorMessage": null
    }
  ]
}
```

---

### 2. 测试执行工作流

与执行接口相同，但**不保存执行日志**，用于调试。

**请求**
```
POST /api/v1/ai-workflows/{workflowId}/test
```

**请求体**: 同执行接口

---

### 3. 为会话自动匹配执行工作流

根据会话的分类等信息自动选择合适的工作流执行。

**请求**
```
POST /api/v1/ai-workflows/execute-for-session?sessionId={sessionId}&userMessage={message}
```

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| sessionId | UUID | 是 | 会话ID |
| userMessage | string | 是 | 用户消息 |

**响应**: 同执行接口

---

## LLM 模型管理 API

### 1. 获取所有模型

**请求**
```
GET /api/v1/llm-models
```

**响应**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "GPT-4o",
    "code": "gpt-4o",
    "provider": "OPENAI",
    "modelName": "gpt-4o",
    "baseUrl": "https://api.openai.com/v1",
    "azureDeploymentName": null,
    "defaultTemperature": 0.7,
    "defaultMaxTokens": 4096,
    "contextWindow": 128000,
    "inputPricePer1k": 0.005,
    "outputPricePer1k": 0.015,
    "supportsFunctions": true,
    "supportsVision": true,
    "enabled": true,
    "isDefault": true,
    "sortOrder": 1,
    "description": "OpenAI 最新的多模态模型",
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
]
```

---

### 2. 获取启用的模型

**请求**
```
GET /api/v1/llm-models/enabled
```

**响应**: 同上（仅返回 enabled=true 的模型）

---

### 3. 获取模型详情

**请求**
```
GET /api/v1/llm-models/{modelId}
```

---

### 4. 创建模型

**请求**
```
POST /api/v1/llm-models
```

**请求体**
```json
{
  "name": "GPT-4o",
  "code": "gpt-4o",
  "provider": "OPENAI",
  "modelName": "gpt-4o",
  "baseUrl": "https://api.openai.com/v1",
  "apiKey": "sk-xxx",
  "azureDeploymentName": null,
  "defaultTemperature": 0.7,
  "defaultMaxTokens": 4096,
  "contextWindow": 128000,
  "inputPricePer1k": 0.005,
  "outputPricePer1k": 0.015,
  "supportsFunctions": true,
  "supportsVision": true,
  "enabled": true,
  "sortOrder": 1,
  "description": "OpenAI 最新的多模态模型"
}
```

**请求体参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| name | string | 是 | 模型显示名称 |
| code | string | 是 | 模型唯一编码 |
| provider | string | 是 | 提供商代码 |
| modelName | string | 是 | API 模型名 |
| baseUrl | string | 否 | API 地址 |
| apiKey | string | 否 | API Key |
| defaultTemperature | double | 否 | 默认温度 |
| defaultMaxTokens | int | 否 | 默认最大 Token |
| enabled | boolean | 否 | 是否启用 |

**响应**: 201 Created

---

### 5. 更新模型

**请求**
```
PUT /api/v1/llm-models/{modelId}
```

---

### 6. 删除模型

**请求**
```
DELETE /api/v1/llm-models/{modelId}
```

---

### 7. 启用/禁用模型

**请求**
```
PATCH /api/v1/llm-models/{modelId}/toggle?enabled=true
```

---

### 8. 设置默认模型

**请求**
```
POST /api/v1/llm-models/{modelId}/set-default
```

---

### 9. 获取提供商列表

**请求**
```
GET /api/v1/llm-models/providers
```

**响应**
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
    "name": "Ollama",
    "defaultBaseUrl": "http://localhost:11434"
  },
  {
    "code": "ZHIPU",
    "name": "智谱AI",
    "defaultBaseUrl": "https://open.bigmodel.cn/api/paas/v4"
  },
  {
    "code": "DASHSCOPE",
    "name": "通义千问",
    "defaultBaseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1"
  },
  {
    "code": "MOONSHOT",
    "name": "月之暗面",
    "defaultBaseUrl": "https://api.moonshot.cn/v1"
  },
  {
    "code": "DEEPSEEK",
    "name": "DeepSeek",
    "defaultBaseUrl": "https://api.deepseek.com/v1"
  },
  {
    "code": "CUSTOM",
    "name": "自定义",
    "defaultBaseUrl": null
  }
]
```

---

### 10. 测试模型连接

**请求**
```
POST /api/v1/llm-models/{modelId}/test
```

**响应**
```json
{
  "success": true,
  "response": "测试成功",
  "durationMs": 1234,
  "error": null
}
```

---

### 11. 清除模型缓存

**请求**
```
POST /api/v1/llm-models/clear-cache
```

**响应**: 204 No Content

---

## 数据结构说明

### ReactFlow 节点结构

```typescript
interface WorkflowNode {
  id: string;           // 唯一标识，如 "llm_1"
  type: string;         // 节点类型
  data: {
    label: string;      // 显示标签
    config: object;     // 节点配置
  };
  position: {
    x: number;
    y: number;
  };
}
```

### ReactFlow 边结构

```typescript
interface WorkflowEdge {
  id: string;           // 唯一标识
  source: string;       // 源节点ID
  target: string;       // 目标节点ID
  sourceHandle?: string; // 源节点句柄（条件分支用："true"/"false"）
  targetHandle?: string;
  label?: string;
}
```

### 工作流执行结果

```typescript
interface WorkflowExecutionResult {
  success: boolean;           // 是否成功
  reply: string | null;       // AI 回复
  errorMessage: string | null; // 错误信息
  needHumanTransfer: boolean; // 是否需要转人工
  nodeDetails: NodeExecutionDetail[]; // 节点执行详情
}

interface NodeExecutionDetail {
  nodeId: string;
  nodeType: string;
  nodeName: string | null;
  input: any;
  output: any;
  startTime: number;
  endTime: number;
  durationMs: number;
  success: boolean;
  errorMessage: string | null;
}
```

---

## 节点类型配置

### 1. 开始节点 (start)

**配置**: 无需配置

```json
{
  "id": "start_1",
  "type": "start",
  "data": { "label": "开始" },
  "position": { "x": 250, "y": 0 }
}
```

---

### 2. 结束节点 (end)

**配置**: 无需配置

```json
{
  "id": "end_1",
  "type": "end",
  "data": { "label": "结束" },
  "position": { "x": 250, "y": 400 }
}
```

---

### 3. LLM 节点 (llm)

调用大语言模型进行对话。

**配置参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| modelId | UUID | 否 | 模型ID（优先） |
| modelCode | string | 否 | 模型编码（备选） |
| systemPrompt | string | 否 | 系统提示词 |
| temperature | double | 否 | 温度（0-2） |
| maxTokens | int | 否 | 最大输出 Token |
| useHistory | boolean | 否 | 是否使用聊天历史 |

**示例**
```json
{
  "id": "llm_1",
  "type": "llm",
  "data": {
    "label": "AI 对话",
    "config": {
      "modelId": "550e8400-e29b-41d4-a716-446655440000",
      "systemPrompt": "你是一个友好的智能客服助手，负责解答用户的问题。",
      "temperature": 0.7,
      "maxTokens": 2000,
      "useHistory": true
    }
  },
  "position": { "x": 250, "y": 100 }
}
```

---

### 4. 条件节点 (condition)

根据条件进行分支判断。

**配置参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| conditionType | string | 是 | 条件类型 |
| value | string | 是 | 目标值 |
| sourceType | string | 否 | 数据来源（默认 lastOutput） |
| variableName | string | 否 | 变量名（sourceType=variable 时） |

**条件类型**

| 类型 | 说明 |
|-----|------|
| contains | 包含 |
| notContains | 不包含 |
| equals | 等于 |
| notEquals | 不等于 |
| startsWith | 以...开头 |
| endsWith | 以...结尾 |
| regex | 正则匹配 |
| isEmpty | 为空 |
| isNotEmpty | 不为空 |
| intentEquals | 意图等于 |
| confidenceGreaterThan | 意图置信度大于 |

**数据来源**

| 类型 | 说明 |
|-----|------|
| lastOutput | 上一个节点的输出 |
| userMessage | 用户原始消息 |
| intent | 识别的意图 |
| variable | 指定变量 |
| entity | 提取的实体 |

**示例**
```json
{
  "id": "condition_1",
  "type": "condition",
  "data": {
    "label": "是否退款意图",
    "config": {
      "conditionType": "intentEquals",
      "value": "refund",
      "sourceType": "intent"
    }
  },
  "position": { "x": 250, "y": 200 }
}
```

**边配置**（条件节点需要两条边）
```json
[
  { "id": "e1", "source": "condition_1", "target": "refund_flow", "sourceHandle": "true" },
  { "id": "e2", "source": "condition_1", "target": "normal_flow", "sourceHandle": "false" }
]
```

---

### 5. 回复节点 (reply)

设置最终回复内容。

**配置参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| replyType | string | 是 | 回复类型 |
| template | string | 否 | 模板内容（replyType=template 时） |
| sourceNodeId | string | 否 | 源节点ID（replyType=nodeOutput 时） |
| variableName | string | 否 | 变量名（replyType=variable 时） |

**回复类型**

| 类型 | 说明 |
|-----|------|
| template | 模板回复，支持 `{{变量}}` 替换 |
| lastOutput | 使用上一个节点的输出 |
| nodeOutput | 使用指定节点的输出 |
| variable | 使用变量值 |

**模板变量**

| 变量 | 说明 |
|-----|------|
| `{{userMessage}}` | 用户原始消息 |
| `{{lastOutput}}` | 上一节点输出 |
| `{{intent}}` | 识别的意图 |
| `{{customerName}}` | 自定义变量 |

**示例**
```json
{
  "id": "reply_1",
  "type": "reply",
  "data": {
    "label": "回复用户",
    "config": {
      "replyType": "template",
      "template": "您好！{{lastOutput}}"
    }
  },
  "position": { "x": 250, "y": 300 }
}
```

---

### 6. 意图识别节点 (intent)

识别用户输入的意图。

**配置参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| recognitionType | string | 是 | 识别方式：keyword/llm/rule |
| modelId | UUID | 否 | LLM 模型ID（recognitionType=llm 时） |
| intents | array | 是 | 意图列表 |

**意图定义**

```json
{
  "name": "refund",
  "description": "用户想要退款退货",
  "keywords": ["退款", "退货", "退钱"]
}
```

**示例**
```json
{
  "id": "intent_1",
  "type": "intent",
  "data": {
    "label": "意图识别",
    "config": {
      "recognitionType": "keyword",
      "intents": [
        {
          "name": "refund",
          "description": "退款退货",
          "keywords": ["退款", "退货", "退钱"]
        },
        {
          "name": "consult",
          "description": "产品咨询",
          "keywords": ["咨询", "了解", "介绍"]
        },
        {
          "name": "complaint",
          "description": "投诉建议",
          "keywords": ["投诉", "建议", "不满意"]
        }
      ]
    }
  },
  "position": { "x": 250, "y": 100 }
}
```

---

### 7. API 节点 (api)

调用外部 API。

**配置参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| url | string | 是 | API 地址，支持 `{{变量}}` |
| method | string | 是 | HTTP 方法：GET/POST/PUT/DELETE |
| headers | object | 否 | 请求头 |
| body | object | 否 | 请求体 |
| responseMapping | string | 否 | 响应提取路径（如 `$.data.result`） |
| saveToVariable | string | 否 | 保存到变量名 |

**示例**
```json
{
  "id": "api_1",
  "type": "api",
  "data": {
    "label": "查询订单",
    "config": {
      "url": "https://api.example.com/orders/{{orderId}}",
      "method": "GET",
      "headers": {
        "Authorization": "Bearer xxx"
      },
      "responseMapping": "$.data",
      "saveToVariable": "orderInfo"
    }
  },
  "position": { "x": 250, "y": 200 }
}
```

---

### 8. 知识库节点 (knowledge)

查询知识库。

**配置参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| querySource | string | 否 | 查询来源：userMessage/lastOutput/custom |
| customQuery | string | 否 | 自定义查询（querySource=custom 时） |
| maxResults | int | 否 | 最大结果数（默认 3） |
| outputFormat | string | 否 | 输出格式：combined/list/first |
| noResultMessage | string | 否 | 无结果时的提示 |

**示例**
```json
{
  "id": "knowledge_1",
  "type": "knowledge",
  "data": {
    "label": "知识库查询",
    "config": {
      "querySource": "userMessage",
      "maxResults": 3,
      "outputFormat": "combined",
      "noResultMessage": "抱歉，没有找到相关信息。"
    }
  },
  "position": { "x": 250, "y": 150 }
}
```

---

### 9. 转人工节点 (human_transfer)

标记需要转接人工客服。

**配置参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| reason | string | 否 | 转人工原因 |
| message | string | 否 | 提示消息 |

**示例**
```json
{
  "id": "human_1",
  "type": "human_transfer",
  "data": {
    "label": "转人工",
    "config": {
      "reason": "用户要求转人工",
      "message": "正在为您转接人工客服，请稍候..."
    }
  },
  "position": { "x": 100, "y": 300 }
}
```

---

### 10. 变量节点 (variable)

操作变量。

**配置参数**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| operation | string | 是 | 操作类型：set/append/delete |
| variables | object | 是 | 变量键值对 |

**示例**
```json
{
  "id": "variable_1",
  "type": "variable",
  "data": {
    "label": "设置变量",
    "config": {
      "operation": "set",
      "variables": {
        "greeting": "您好，欢迎咨询！",
        "currentIntent": "{{intent}}"
      }
    }
  },
  "position": { "x": 250, "y": 50 }
}
```

---

## 前端集成指南

### 1. 安装依赖

```bash
npm install reactflow
# 或
yarn add reactflow
```

### 2. TypeScript 类型定义

```typescript
// 工作流类型
interface AiWorkflow {
  id: string;
  name: string;
  description: string | null;
  nodesJson: string;
  edgesJson: string;
  liteflowEl: string;
  version: number;
  enabled: boolean;
  isDefault: boolean;
  createdByAgentId: string | null;
  createdByAgentName: string | null;
  triggerType: string;
  triggerConfig: string | null;
  createdAt: string;
  updatedAt: string;
}

// LLM 模型类型
interface LlmModel {
  id: string;
  name: string;
  code: string;
  provider: string;
  modelName: string;
  baseUrl: string | null;
  defaultTemperature: number;
  defaultMaxTokens: number;
  contextWindow: number;
  inputPricePer1k: number | null;
  outputPricePer1k: number | null;
  supportsFunctions: boolean;
  supportsVision: boolean;
  enabled: boolean;
  isDefault: boolean;
  sortOrder: number;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

// 执行结果类型
interface WorkflowExecutionResult {
  success: boolean;
  reply: string | null;
  errorMessage: string | null;
  needHumanTransfer: boolean;
  nodeDetails: NodeExecutionDetail[];
}
```

### 3. API 调用封装

```typescript
const API_BASE = '/api/v1';

// 获取所有工作流
export async function getWorkflows(): Promise<AiWorkflow[]> {
  const res = await fetch(`${API_BASE}/ai-workflows`, {
    headers: { Authorization: `Bearer ${token}` }
  });
  return res.json();
}

// 保存工作流
export async function saveWorkflow(
  id: string | null,
  data: {
    name: string;
    description?: string;
    nodesJson: string;
    edgesJson: string;
    triggerType?: string;
    triggerConfig?: string;
  }
): Promise<AiWorkflow> {
  const url = id 
    ? `${API_BASE}/ai-workflows/${id}` 
    : `${API_BASE}/ai-workflows`;
  const method = id ? 'PUT' : 'POST';
  
  const res = await fetch(url, {
    method,
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(data)
  });
  return res.json();
}

// 执行工作流
export async function executeWorkflow(
  workflowId: string,
  userMessage: string,
  sessionId?: string,
  variables?: Record<string, any>
): Promise<WorkflowExecutionResult> {
  const res = await fetch(`${API_BASE}/ai-workflows/${workflowId}/execute`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ sessionId, userMessage, variables })
  });
  return res.json();
}

// 获取启用的模型列表
export async function getEnabledModels(): Promise<LlmModel[]> {
  const res = await fetch(`${API_BASE}/llm-models/enabled`, {
    headers: { Authorization: `Bearer ${token}` }
  });
  return res.json();
}
```

### 4. ReactFlow 工作流编辑器示例

```tsx
import React, { useState, useCallback } from 'react';
import ReactFlow, {
  Node,
  Edge,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  addEdge,
  Connection
} from 'reactflow';
import 'reactflow/dist/style.css';

const WorkflowEditor: React.FC = () => {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [workflowName, setWorkflowName] = useState('');

  const onConnect = useCallback(
    (connection: Connection) => setEdges((eds) => addEdge(connection, eds)),
    [setEdges]
  );

  // 添加节点
  const addNode = (type: string) => {
    const newNode: Node = {
      id: `${type}_${Date.now()}`,
      type,
      data: { label: getNodeLabel(type), config: {} },
      position: { x: 250, y: nodes.length * 100 }
    };
    setNodes((nds) => [...nds, newNode]);
  };

  // 保存工作流
  const handleSave = async () => {
    try {
      await saveWorkflow(null, {
        name: workflowName,
        nodesJson: JSON.stringify(nodes),
        edgesJson: JSON.stringify(edges)
      });
      alert('保存成功！');
    } catch (error) {
      alert('保存失败：' + error);
    }
  };

  return (
    <div style={{ height: '100vh', display: 'flex' }}>
      {/* 工具栏 */}
      <div style={{ width: 200, padding: 16, borderRight: '1px solid #ccc' }}>
        <h3>节点类型</h3>
        <button onClick={() => addNode('start')}>开始</button>
        <button onClick={() => addNode('llm')}>LLM</button>
        <button onClick={() => addNode('condition')}>条件</button>
        <button onClick={() => addNode('reply')}>回复</button>
        <button onClick={() => addNode('intent')}>意图识别</button>
        <button onClick={() => addNode('api')}>API</button>
        <button onClick={() => addNode('knowledge')}>知识库</button>
        <button onClick={() => addNode('human_transfer')}>转人工</button>
        <button onClick={() => addNode('end')}>结束</button>
        
        <hr />
        
        <input
          placeholder="工作流名称"
          value={workflowName}
          onChange={(e) => setWorkflowName(e.target.value)}
        />
        <button onClick={handleSave}>保存</button>
      </div>
      
      {/* 画布 */}
      <div style={{ flex: 1 }}>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          fitView
        >
          <Controls />
          <Background />
        </ReactFlow>
      </div>
    </div>
  );
};

function getNodeLabel(type: string): string {
  const labels: Record<string, string> = {
    start: '开始',
    end: '结束',
    llm: 'LLM 对话',
    condition: '条件判断',
    reply: '回复',
    intent: '意图识别',
    api: 'API 调用',
    knowledge: '知识库',
    human_transfer: '转人工',
    variable: '变量'
  };
  return labels[type] || type;
}

export default WorkflowEditor;
```

---

## 错误码说明

| HTTP 状态码 | 说明 |
|------------|------|
| 200 | 请求成功 |
| 201 | 创建成功 |
| 204 | 操作成功（无返回内容） |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如名称重复） |
| 500 | 服务器内部错误 |

### 常见错误响应

```json
{
  "timestamp": "2024-01-01T00:00:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "工作流名称已存在",
  "path": "/api/v1/ai-workflows"
}
```

```json
{
  "timestamp": "2024-01-01T00:00:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "工作流不存在",
  "path": "/api/v1/ai-workflows/xxx"
}
```

---

## 完整工作流示例

### 智能客服基础工作流

```json
{
  "name": "智能客服基础工作流",
  "description": "包含意图识别、条件分支的完整工作流",
  "nodesJson": [
    {
      "id": "start_1",
      "type": "start",
      "data": { "label": "开始" },
      "position": { "x": 250, "y": 0 }
    },
    {
      "id": "intent_1",
      "type": "intent",
      "data": {
        "label": "意图识别",
        "config": {
          "recognitionType": "keyword",
          "intents": [
            { "name": "refund", "keywords": ["退款", "退货"] },
            { "name": "consult", "keywords": ["咨询", "了解"] }
          ]
        }
      },
      "position": { "x": 250, "y": 100 }
    },
    {
      "id": "condition_1",
      "type": "condition",
      "data": {
        "label": "是否退款",
        "config": {
          "conditionType": "intentEquals",
          "value": "refund"
        }
      },
      "position": { "x": 250, "y": 200 }
    },
    {
      "id": "human_1",
      "type": "human_transfer",
      "data": {
        "label": "转人工处理",
        "config": {
          "reason": "退款需求",
          "message": "正在为您转接退款专员..."
        }
      },
      "position": { "x": 100, "y": 300 }
    },
    {
      "id": "llm_1",
      "type": "llm",
      "data": {
        "label": "AI 回答",
        "config": {
          "modelCode": "gpt-4o-mini",
          "systemPrompt": "你是一个友好的客服助手。"
        }
      },
      "position": { "x": 400, "y": 300 }
    },
    {
      "id": "reply_1",
      "type": "reply",
      "data": {
        "label": "回复",
        "config": { "replyType": "lastOutput" }
      },
      "position": { "x": 250, "y": 400 }
    },
    {
      "id": "end_1",
      "type": "end",
      "data": { "label": "结束" },
      "position": { "x": 250, "y": 500 }
    }
  ],
  "edgesJson": [
    { "id": "e1", "source": "start_1", "target": "intent_1" },
    { "id": "e2", "source": "intent_1", "target": "condition_1" },
    { "id": "e3", "source": "condition_1", "target": "human_1", "sourceHandle": "true" },
    { "id": "e4", "source": "condition_1", "target": "llm_1", "sourceHandle": "false" },
    { "id": "e5", "source": "human_1", "target": "reply_1" },
    { "id": "e6", "source": "llm_1", "target": "reply_1" },
    { "id": "e7", "source": "reply_1", "target": "end_1" }
  ]
}
```

---

## 更新日志

| 版本 | 日期 | 说明 |
|-----|------|------|
| 1.0.0 | 2024-01-01 | 初始版本 |

