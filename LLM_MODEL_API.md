# LLM 模型管理 API 文档

## 概述

本系统集成了 LangChain4j 框架，支持多种 LLM 提供商的模型配置和管理。工作流节点可以从模型表中选择要使用的模型，而不是使用固定的系统模型。

## 支持的 LLM 提供商

| 提供商 | 代码 | 说明 |
|-------|------|------|
| OpenAI | `OPENAI` | GPT 系列模型 |
| Azure OpenAI | `AZURE_OPENAI` | Azure 上的 OpenAI 服务 |
| Ollama | `OLLAMA` | 本地运行的开源模型 |
| 智谱 AI | `ZHIPU` | GLM 系列模型 |
| 通义千问 | `DASHSCOPE` | 阿里云千问系列 |
| 月之暗面 | `MOONSHOT` | Kimi/Moonshot 系列 |
| DeepSeek | `DEEPSEEK` | DeepSeek 系列模型 |
| 自定义 | `CUSTOM` | OpenAI 兼容接口 |

## API 接口

### 1. 模型管理

#### 获取所有模型
```
GET /api/v1/llm-models
```

#### 获取启用的模型
```
GET /api/v1/llm-models/enabled
```

#### 获取模型详情
```
GET /api/v1/llm-models/{modelId}
```

#### 创建模型
```
POST /api/v1/llm-models
Content-Type: application/json

{
  "name": "GPT-4o",
  "code": "gpt-4o",
  "provider": "OPENAI",
  "modelName": "gpt-4o",
  "baseUrl": "https://api.openai.com/v1",
  "apiKey": "sk-xxx",
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

#### 更新模型
```
PUT /api/v1/llm-models/{modelId}
```

#### 删除模型
```
DELETE /api/v1/llm-models/{modelId}
```

#### 启用/禁用模型
```
PATCH /api/v1/llm-models/{modelId}/toggle?enabled=true
```

#### 设置默认模型
```
POST /api/v1/llm-models/{modelId}/set-default
```

#### 获取提供商列表
```
GET /api/v1/llm-models/providers
```

#### 测试模型连接
```
POST /api/v1/llm-models/{modelId}/test
```

#### 清除模型缓存
```
POST /api/v1/llm-models/clear-cache
```

---

## 模型配置字段说明

| 字段 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| name | string | 是 | 模型显示名称 |
| code | string | 是 | 模型唯一编码，用于 API 调用 |
| provider | string | 是 | 提供商代码 |
| modelName | string | 是 | API 中使用的模型名（如 gpt-4o） |
| baseUrl | string | 否 | API Base URL |
| apiKey | string | 否 | API Key |
| azureDeploymentName | string | 否 | Azure 部署名称（Azure 专用） |
| defaultTemperature | double | 否 | 默认温度，范围 0-2 |
| defaultMaxTokens | int | 否 | 默认最大输出 Token |
| contextWindow | int | 否 | 上下文窗口大小 |
| inputPricePer1k | double | 否 | 输入价格（$/1K tokens） |
| outputPricePer1k | double | 否 | 输出价格（$/1K tokens） |
| supportsFunctions | boolean | 否 | 是否支持函数调用 |
| supportsVision | boolean | 否 | 是否支持图片输入 |
| enabled | boolean | 否 | 是否启用 |
| sortOrder | int | 否 | 排序顺序 |
| description | string | 否 | 描述 |

---

## 工作流节点配置

### LLM 节点配置

```json
{
  "modelId": "550e8400-e29b-41d4-a716-446655440000",
  "modelCode": "gpt-4o",
  "systemPrompt": "你是一个智能客服助手。",
  "temperature": 0.7,
  "maxTokens": 2000,
  "useHistory": true
}
```

| 字段 | 说明 |
|-----|------|
| modelId | 模型ID（优先使用） |
| modelCode | 模型编码（modelId 为空时使用） |
| systemPrompt | 系统提示词 |
| temperature | 温度（可选，使用模型默认值） |
| maxTokens | 最大 Token（可选，使用模型默认值） |
| useHistory | 是否使用聊天历史 |

### 意图识别节点配置（LLM 模式）

```json
{
  "recognitionType": "llm",
  "modelId": "550e8400-e29b-41d4-a716-446655440000",
  "modelCode": "gpt-4o-mini",
  "intents": [
    {
      "name": "refund",
      "description": "用户想要退款退货",
      "keywords": ["退款", "退货"]
    }
  ]
}
```

---

## 前端集成示例

### TypeScript 类型定义

```typescript
interface LlmModel {
  id: string;
  name: string;
  code: string;
  provider: string;
  modelName: string;
  baseUrl: string | null;
  azureDeploymentName: string | null;
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

interface Provider {
  code: string;
  name: string;
  defaultBaseUrl: string | null;
}
```

### 模型选择器组件

```tsx
import React, { useState, useEffect } from 'react';

interface ModelSelectorProps {
  value: string | null;
  onChange: (modelId: string) => void;
}

const ModelSelector: React.FC<ModelSelectorProps> = ({ value, onChange }) => {
  const [models, setModels] = useState<LlmModel[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch('/api/v1/llm-models/enabled')
      .then(res => res.json())
      .then(data => {
        setModels(data);
        setLoading(false);
      });
  }, []);

  if (loading) return <div>加载中...</div>;

  // 按提供商分组
  const groupedModels = models.reduce((acc, model) => {
    const provider = model.provider;
    if (!acc[provider]) acc[provider] = [];
    acc[provider].push(model);
    return acc;
  }, {} as Record<string, LlmModel[]>);

  return (
    <select 
      value={value || ''} 
      onChange={(e) => onChange(e.target.value)}
      className="model-selector"
    >
      <option value="">使用默认模型</option>
      {Object.entries(groupedModels).map(([provider, providerModels]) => (
        <optgroup key={provider} label={provider}>
          {providerModels.map(model => (
            <option key={model.id} value={model.id}>
              {model.name} {model.isDefault && '(默认)'}
            </option>
          ))}
        </optgroup>
      ))}
    </select>
  );
};

export default ModelSelector;
```

### 在 ReactFlow 节点编辑器中使用

```tsx
const LlmNodeEditor: React.FC<NodeEditorProps> = ({ node, onChange }) => {
  const [config, setConfig] = useState(node.data.config || {});

  const handleConfigChange = (key: string, value: any) => {
    const newConfig = { ...config, [key]: value };
    setConfig(newConfig);
    onChange({
      ...node,
      data: { ...node.data, config: newConfig }
    });
  };

  return (
    <div className="node-editor">
      <div className="form-group">
        <label>选择模型</label>
        <ModelSelector
          value={config.modelId}
          onChange={(modelId) => handleConfigChange('modelId', modelId)}
        />
      </div>
      
      <div className="form-group">
        <label>系统提示词</label>
        <textarea
          value={config.systemPrompt || ''}
          onChange={(e) => handleConfigChange('systemPrompt', e.target.value)}
          placeholder="你是一个智能客服助手..."
        />
      </div>
      
      <div className="form-group">
        <label>温度 (留空使用模型默认值)</label>
        <input
          type="number"
          min="0"
          max="2"
          step="0.1"
          value={config.temperature || ''}
          onChange={(e) => handleConfigChange('temperature', 
            e.target.value ? parseFloat(e.target.value) : null)}
        />
      </div>
      
      <div className="form-group">
        <label>
          <input
            type="checkbox"
            checked={config.useHistory !== false}
            onChange={(e) => handleConfigChange('useHistory', e.target.checked)}
          />
          使用聊天历史
        </label>
      </div>
    </div>
  );
};
```

---

## 数据库初始化

执行以下 SQL 脚本创建模型表并插入示例数据：

```bash
mysql -u root -p your_database < db/create_llm_models.sql
```

脚本会创建以下预配置模型（需手动设置 API Key）：
- OpenAI: GPT-4o, GPT-4o Mini, GPT-4 Turbo, GPT-3.5 Turbo
- DeepSeek: DeepSeek Chat, DeepSeek Coder
- 月之暗面: Moonshot v1 8K/32K/128K
- 智谱 AI: GLM-4, GLM-4 Flash
- 通义千问: Qwen Max/Plus/Turbo
- Ollama: Llama3, Qwen2（本地模型）

---

## 使用流程

```
┌──────────────────────────────────────────────────────────┐
│                    管理员配置模型                          │
├──────────────────────────────────────────────────────────┤
│  1. 访问模型管理页面                                       │
│  2. 添加/编辑模型配置                                      │
│  3. 设置 API Key                                          │
│  4. 测试模型连接                                          │
│  5. 启用模型并设置默认                                     │
└──────────────────────────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────┐
│                   工作流编辑器                             │
├──────────────────────────────────────────────────────────┤
│  1. 拖入 LLM 节点或意图识别节点                            │
│  2. 从下拉列表选择要使用的模型                             │
│  3. 配置系统提示词等参数                                   │
│  4. 保存工作流                                            │
└──────────────────────────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────┐
│                    工作流执行                              │
├──────────────────────────────────────────────────────────┤
│  1. LlmNode 从配置读取 modelId                             │
│  2. LangChainChatService 根据 modelId 获取模型配置         │
│  3. 动态创建 ChatModel 实例                                │
│  4. 发送请求并返回结果                                     │
└──────────────────────────────────────────────────────────┘
```

---

## 注意事项

1. **API Key 安全**：API Key 存储在数据库中，建议在生产环境中进行加密
2. **模型缓存**：系统会缓存已创建的模型实例，修改配置后会自动更新
3. **默认模型**：如果节点未指定模型，将使用标记为默认的模型
4. **价格统计**：系统会估算 Token 使用量，可用于成本统计
5. **本地模型**：Ollama 模型需要先在本地安装并运行 Ollama

