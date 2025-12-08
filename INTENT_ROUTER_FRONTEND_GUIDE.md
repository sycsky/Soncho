# 意图识别节点 (Intent Node) 前端配置指南

## 概述

意图识别节点是一个 **Switch 类型** 节点，它结合了：
1. **意图识别**：分析用户消息，识别用户意图
2. **路由分发**：根据识别的意图，自动路由到对应的处理节点

这个节点通过分析用户消息，匹配预定义的意图列表，然后根据匹配结果路由到不同的后续节点。

## 核心设计理念

```
用户消息 → [意图识别] → 匹配意图 → 找到 sourceHandle → 路由到对应节点
                          ↓
              Intents 配置中的 id 就是 sourceHandle
```

**关键点：**
- 每个意图配置有唯一的 `id`，这个 `id` 同时作为连线的 `sourceHandle`
- LLM 分析用户消息，匹配 `label`（意图描述）
- 匹配成功后，通过意图的 `id` 找到对应的出边和目标节点

## 节点配置结构

### ReactFlow 节点数据

```typescript
interface IntentNodeData {
  id: string;           // 节点唯一 ID，如 "intent_1"
  type: "intent";       // 节点类型，必须是 "intent"
  position: {
    x: number;
    y: number;
  };
  data: {
    label: string;      // 节点显示名称
    config: {
      modelId?: string;        // 可选：用于意图识别的 LLM 模型 ID
      modelCode?: string;      // 可选：用于意图识别的 LLM 模型代码
      recognitionType?: "llm" | "keyword";  // 识别方式，默认 llm
      defaultRouteId?: string; // 默认路由的 ID，默认 "default"
      Intents: IntentItem[];   // 意图列表配置
    };
  };
}

interface IntentItem {
  id: string;      // 意图唯一 ID，同时作为 sourceHandle
  label: string;   // 意图描述，用于 LLM 分析匹配
  keywords?: string[];  // 可选：关键词列表，用于 keyword 模式匹配
}
```

### 完整示例

```json
{
  "id": "intent_1",
  "type": "intent",
  "position": { "x": 400, "y": 200 },
  "data": {
    "label": "意图识别",
    "config": {
      "modelId": "model-uuid-xxx",
      "recognitionType": "llm",
      "defaultRouteId": "default",
      "Intents": [
        {
          "id": "c1764337030732",
          "label": "用户要退款"
        },
        {
          "id": "c1764337031100",
          "label": "用户要差评"
        },
        {
          "id": "c1764337031200",
          "label": "用户咨询产品"
        }
      ]
    }
  }
}
```

## 边（Edge）配置

边的 `sourceHandle` 必须与意图配置中的 `id` 对应：

```json
[
  {
    "id": "edge-1",
    "source": "intent_1",
    "target": "refund_handler",
    "sourceHandle": "c1764337030732"
  },
  {
    "id": "edge-2",
    "source": "intent_1",
    "target": "complaint_handler",
    "sourceHandle": "c1764337031100"
  },
  {
    "id": "edge-3",
    "source": "intent_1",
    "target": "product_faq",
    "sourceHandle": "c1764337031200"
  },
  {
    "id": "edge-4",
    "source": "intent_1",
    "target": "default_handler",
    "sourceHandle": "default"
  }
]
```

## ReactFlow 组件实现

### 1. 自定义意图节点组件

```tsx
import React, { useState, useCallback, useMemo } from 'react';
import { Handle, Position, NodeProps, useReactFlow } from 'reactflow';
import { nanoid } from 'nanoid';

interface IntentItem {
  id: string;
  label: string;
  keywords?: string[];
}

interface IntentNodeConfig {
  modelId?: string;
  modelCode?: string;
  recognitionType?: 'llm' | 'keyword';
  defaultRouteId?: string;
  Intents: IntentItem[];
}

interface IntentNodeData {
  label: string;
  config: IntentNodeConfig;
}

export const IntentNode: React.FC<NodeProps<IntentNodeData>> = ({ id, data, selected }) => {
  const { setNodes } = useReactFlow();
  const [isEditing, setIsEditing] = useState(false);
  const [newIntentLabel, setNewIntentLabel] = useState('');

  const intents = data.config?.Intents || [];

  // 生成唯一 ID（作为 sourceHandle）
  const generateIntentId = () => `c${Date.now()}${Math.random().toString(36).substr(2, 4)}`;

  // 添加新意图
  const handleAddIntent = useCallback(() => {
    if (!newIntentLabel.trim()) return;

    const newIntent: IntentItem = {
      id: generateIntentId(),
      label: newIntentLabel.trim(),
    };

    setNodes((nodes) =>
      nodes.map((node) => {
        if (node.id === id) {
          return {
            ...node,
            data: {
              ...node.data,
              config: {
                ...node.data.config,
                Intents: [...(node.data.config?.Intents || []), newIntent],
              },
            },
          };
        }
        return node;
      })
    );

    setNewIntentLabel('');
  }, [id, newIntentLabel, setNodes]);

  // 删除意图
  const handleDeleteIntent = useCallback((intentId: string) => {
    setNodes((nodes) =>
      nodes.map((node) => {
        if (node.id === id) {
          return {
            ...node,
            data: {
              ...node.data,
              config: {
                ...node.data.config,
                Intents: node.data.config?.Intents?.filter(
                  (intent: IntentItem) => intent.id !== intentId
                ) || [],
              },
            },
          };
        }
        return node;
      })
    );
  }, [id, setNodes]);

  // 更新意图 label
  const handleUpdateIntentLabel = useCallback((intentId: string, newLabel: string) => {
    setNodes((nodes) =>
      nodes.map((node) => {
        if (node.id === id) {
          return {
            ...node,
            data: {
              ...node.data,
              config: {
                ...node.data.config,
                Intents: node.data.config?.Intents?.map((intent: IntentItem) =>
                  intent.id === intentId ? { ...intent, label: newLabel } : intent
                ) || [],
              },
            },
          };
        }
        return node;
      })
    );
  }, [id, setNodes]);

  return (
    <div
      className={`intent-node ${selected ? 'selected' : ''}`}
      style={{
        padding: '16px',
        borderRadius: '12px',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        color: 'white',
        minWidth: '280px',
        boxShadow: selected 
          ? '0 0 0 2px #fff, 0 0 0 4px #667eea' 
          : '0 4px 20px rgba(102, 126, 234, 0.3)',
      }}
    >
      {/* 输入连接点 */}
      <Handle
        type="target"
        position={Position.Left}
        style={{ 
          background: '#fff', 
          border: '2px solid #667eea',
          width: 12,
          height: 12,
        }}
      />

      {/* 节点标题 */}
      <div style={{ 
        fontWeight: 'bold', 
        fontSize: '14px', 
        marginBottom: '12px',
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
      }}>
        <span>🎯</span>
        <span>{data.label || '意图识别'}</span>
      </div>

      {/* 意图列表 */}
      <div style={{ 
        display: 'flex', 
        flexDirection: 'column', 
        gap: '8px',
        marginBottom: '12px',
      }}>
        {intents.map((intent, index) => (
          <div
            key={intent.id}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              background: 'rgba(255,255,255,0.15)',
              padding: '8px 12px',
              borderRadius: '8px',
              position: 'relative',
            }}
          >
            <input
              value={intent.label}
              onChange={(e) => handleUpdateIntentLabel(intent.id, e.target.value)}
              style={{
                background: 'transparent',
                border: 'none',
                color: 'white',
                fontSize: '13px',
                flex: 1,
                outline: 'none',
              }}
              placeholder="输入意图描述..."
            />
            
            <button
              onClick={() => handleDeleteIntent(intent.id)}
              style={{
                background: 'rgba(255,255,255,0.2)',
                border: 'none',
                color: 'white',
                borderRadius: '4px',
                padding: '2px 6px',
                cursor: 'pointer',
                fontSize: '12px',
              }}
            >
              ✕
            </button>

            {/* 每个意图对应一个输出连接点 */}
            <Handle
              type="source"
              position={Position.Right}
              id={intent.id}
              style={{
                background: '#fff',
                border: '2px solid #764ba2',
                width: 10,
                height: 10,
                right: -6,
              }}
            />
          </div>
        ))}

        {/* 默认路由连接点 */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            background: 'rgba(255,255,255,0.1)',
            padding: '8px 12px',
            borderRadius: '8px',
            position: 'relative',
            borderStyle: 'dashed',
            borderWidth: '1px',
            borderColor: 'rgba(255,255,255,0.3)',
          }}
        >
          <span style={{ fontSize: '13px', opacity: 0.8 }}>默认 (无匹配)</span>
          <Handle
            type="source"
            position={Position.Right}
            id="default"
            style={{
              background: 'rgba(255,255,255,0.8)',
              border: '2px solid #999',
              width: 10,
              height: 10,
              right: -6,
            }}
          />
        </div>
      </div>

      {/* 添加新意图 */}
      <div style={{ 
        display: 'flex', 
        gap: '8px',
        marginTop: '8px',
      }}>
        <input
          value={newIntentLabel}
          onChange={(e) => setNewIntentLabel(e.target.value)}
          onKeyPress={(e) => e.key === 'Enter' && handleAddIntent()}
          placeholder="添加新意图..."
          style={{
            flex: 1,
            padding: '8px 12px',
            borderRadius: '6px',
            border: 'none',
            background: 'rgba(255,255,255,0.2)',
            color: 'white',
            fontSize: '13px',
            outline: 'none',
          }}
        />
        <button
          onClick={handleAddIntent}
          style={{
            padding: '8px 16px',
            borderRadius: '6px',
            border: 'none',
            background: 'rgba(255,255,255,0.25)',
            color: 'white',
            cursor: 'pointer',
            fontSize: '13px',
          }}
        >
          + 添加
        </button>
      </div>
    </div>
  );
};
```

### 2. 注册自定义节点

```tsx
import ReactFlow, { 
  Controls, 
  Background, 
  MiniMap,
  useNodesState,
  useEdgesState,
  addEdge,
} from 'reactflow';
import { IntentNode } from './nodes/IntentNode';

// 注册自定义节点类型
const nodeTypes = {
  intent: IntentNode,
  // ... 其他节点类型
};

export const WorkflowEditor: React.FC = () => {
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

  const onConnect = useCallback(
    (params) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  return (
    <ReactFlow
      nodes={nodes}
      edges={edges}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      onConnect={onConnect}
      nodeTypes={nodeTypes}
      fitView
    >
      <Controls />
      <Background />
      <MiniMap />
    </ReactFlow>
  );
};
```

### 3. 创建新意图节点

```typescript
const createIntentNode = (position: { x: number; y: number }): Node => ({
  id: `intent_${nanoid(8)}`,
  type: 'intent',
  position,
  data: {
    label: '意图识别',
    config: {
      recognitionType: 'llm',
      defaultRouteId: 'default',
      Intents: [],
    },
  },
});
```

## 工作流执行流程

```
┌─────────────────────────────────────────────────────────────────┐
│                        意图识别节点执行流程                        │
└─────────────────────────────────────────────────────────────────┘

1. 接收用户消息
   │
   ▼
2. 读取节点配置 Intents
   ├── id: "c1764337030732", label: "用户要退款"
   ├── id: "c1764337031100", label: "用户要差评"
   └── id: "c1764337031200", label: "用户咨询产品"
   │
   ▼
3. 调用 LLM 分析用户意图
   │
   │  系统提示词：
   │  "你是一个意图分类器。根据用户输入，从以下意图中选择最匹配的一个:
   │   - 用户要退款
   │   - 用户要差评
   │   - 用户咨询产品"
   │
   ▼
4. LLM 返回匹配的意图 label
   │  例如: "用户要退款"
   │
   ▼
5. 通过 label 查找对应的 id
   │  "用户要退款" → id: "c1764337030732"
   │
   ▼
6. 从边数据中查找路由
   │  sourceHandle: "c1764337030732" → target: "refund_handler"
   │
   ▼
7. 路由到目标节点
   └── 执行 refund_handler 节点
```

## 边数据自动生成

当用户从意图节点的某个输出点拖拽连线到目标节点时，ReactFlow 会自动生成带有正确 `sourceHandle` 的边：

```typescript
// ReactFlow onConnect 回调中的 params
{
  source: "intent_1",           // 源节点 ID
  sourceHandle: "c1764337030732", // 从哪个意图的输出点连出
  target: "refund_handler",     // 目标节点 ID
  targetHandle: null,           // 目标节点的输入点
}
```

## LiteFlow EL 表达式

后端会将意图节点转换为 SWITCH 类型的 EL 表达式：

```
SWITCH(intent_1).TO(refund_handler, complaint_handler, product_faq, default_handler)
```

## 保存工作流时的数据格式

```typescript
interface SaveWorkflowPayload {
  name: string;
  description?: string;
  nodesJson: string;  // JSON.stringify(nodes)
  edgesJson: string;  // JSON.stringify(edges)
  triggerType?: string;
  triggerConfig?: string;
}

// 示例
const saveWorkflow = async () => {
  const payload = {
    name: '客服意图分流工作流',
    description: '根据用户意图分流到不同处理流程',
    nodesJson: JSON.stringify(nodes),
    edgesJson: JSON.stringify(edges),
    triggerType: 'ALL',
  };

  const response = await fetch('/api/v1/ai/workflows', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  return response.json();
};
```

## 完整工作流示例

```json
{
  "nodes": [
    {
      "id": "start_1",
      "type": "start",
      "position": { "x": 100, "y": 200 },
      "data": { "label": "开始" }
    },
    {
      "id": "intent_1",
      "type": "intent",
      "position": { "x": 300, "y": 200 },
      "data": {
        "label": "意图识别",
        "config": {
          "recognitionType": "llm",
          "Intents": [
            { "id": "c001", "label": "用户要退款" },
            { "id": "c002", "label": "用户要差评" },
            { "id": "c003", "label": "用户咨询产品" }
          ]
        }
      }
    },
    {
      "id": "refund_handler",
      "type": "llm",
      "position": { "x": 600, "y": 50 },
      "data": { 
        "label": "退款处理",
        "config": { "prompt": "你是退款专员，帮助用户处理退款..." }
      }
    },
    {
      "id": "complaint_handler",
      "type": "llm",
      "position": { "x": 600, "y": 200 },
      "data": { 
        "label": "投诉处理",
        "config": { "prompt": "你是投诉处理专员..." }
      }
    },
    {
      "id": "product_faq",
      "type": "llm",
      "position": { "x": 600, "y": 350 },
      "data": { 
        "label": "产品咨询",
        "config": { "prompt": "你是产品顾问..." }
      }
    },
    {
      "id": "default_handler",
      "type": "llm",
      "position": { "x": 600, "y": 500 },
      "data": { 
        "label": "默认处理",
        "config": { "prompt": "通用客服回复..." }
      }
    }
  ],
  "edges": [
    { "id": "e1", "source": "start_1", "target": "intent_1" },
    { "id": "e2", "source": "intent_1", "target": "refund_handler", "sourceHandle": "c001" },
    { "id": "e3", "source": "intent_1", "target": "complaint_handler", "sourceHandle": "c002" },
    { "id": "e4", "source": "intent_1", "target": "product_faq", "sourceHandle": "c003" },
    { "id": "e5", "source": "intent_1", "target": "default_handler", "sourceHandle": "default" }
  ]
}
```

## 意图节点配置面板

```tsx
interface IntentNodeConfigPanelProps {
  nodeId: string;
  config: IntentNodeConfig;
  onConfigChange: (config: IntentNodeConfig) => void;
}

export const IntentNodeConfigPanel: React.FC<IntentNodeConfigPanelProps> = ({
  nodeId,
  config,
  onConfigChange,
}) => {
  const [models, setModels] = useState<LlmModel[]>([]);

  useEffect(() => {
    // 加载可用的 LLM 模型
    fetch('/api/v1/llm/models/enabled')
      .then(res => res.json())
      .then(setModels);
  }, []);

  return (
    <div className="config-panel">
      <h3>意图识别配置</h3>

      {/* 识别方式 */}
      <div className="form-group">
        <label>识别方式</label>
        <select
          value={config.recognitionType || 'llm'}
          onChange={(e) => onConfigChange({
            ...config,
            recognitionType: e.target.value as 'llm' | 'keyword',
          })}
        >
          <option value="llm">LLM 智能识别</option>
          <option value="keyword">关键词匹配</option>
        </select>
      </div>

      {/* 模型选择（仅 LLM 模式） */}
      {config.recognitionType === 'llm' && (
        <div className="form-group">
          <label>识别模型</label>
          <select
            value={config.modelId || ''}
            onChange={(e) => onConfigChange({
              ...config,
              modelId: e.target.value || undefined,
            })}
          >
            <option value="">使用默认模型</option>
            {models.map((model) => (
              <option key={model.id} value={model.id}>
                {model.name} ({model.provider})
              </option>
            ))}
          </select>
        </div>
      )}

      {/* 意图列表 */}
      <div className="form-group">
        <label>意图列表</label>
        <p className="help-text">
          每个意图会生成一个输出连接点，拖拽连线到目标节点即可配置路由
        </p>
        
        {config.Intents?.map((intent, index) => (
          <div key={intent.id} className="intent-item">
            <input
              value={intent.label}
              onChange={(e) => {
                const newIntents = [...config.Intents];
                newIntents[index] = { ...intent, label: e.target.value };
                onConfigChange({ ...config, Intents: newIntents });
              }}
              placeholder="意图描述"
            />
            <span className="intent-id">ID: {intent.id}</span>
          </div>
        ))}
      </div>
    </div>
  );
};
```

## 关键概念总结

| 概念 | 说明 |
|------|------|
| `id` | 意图的唯一标识符，同时作为连线的 `sourceHandle` |
| `label` | 意图描述，用于 LLM 分析匹配 |
| `sourceHandle` | 边的属性，标识从哪个输出点连出，值为意图的 `id` |
| `default` | 特殊的 sourceHandle，用于未匹配任何意图时的默认路由 |

## 注意事项

1. **意图 ID 唯一性**：每个意图的 `id` 必须唯一，建议使用时间戳+随机数生成
2. **必须有默认路由**：始终配置一个 `sourceHandle: "default"` 的边作为兜底
3. **意图描述清晰**：`label` 应该清晰描述用户意图，便于 LLM 准确识别
4. **模型选择**：对于复杂意图，建议使用更强大的 LLM 模型
5. **关键词模式**：简单场景可使用 keyword 模式，在 `keywords` 数组中配置关键词

## API 参考

### 测试意图识别

```bash
POST /api/v1/ai/workflows/{workflowId}/test
Content-Type: application/json

{
  "userMessage": "我想退款，商品有质量问题",
  "variables": {}
}
```

响应：

```json
{
  "success": true,
  "reply": "您好，我是退款专员...",
  "nodeDetailsJson": "[{\"nodeId\":\"intent_1\",\"output\":{\"intentId\":\"c001\",\"intentLabel\":\"用户要退款\",\"confidence\":0.85,\"targetNode\":\"refund_handler\"}}]"
}
```
