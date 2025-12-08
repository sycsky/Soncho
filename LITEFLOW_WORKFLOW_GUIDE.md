# LiteFlow 工作流集成指南

## 概述

本项目集成了 LiteFlow 工作流引擎，用于构建智能客服 AI 的工作流编排系统。前端使用 ReactFlow 进行可视化编辑，后端使用 LiteFlow 执行工作流逻辑。

## 架构

```
┌─────────────────────────────────────────────────────────────┐
│                      ReactFlow 前端                          │
│   ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐       │
│   │  开始   │──│  LLM    │──│  条件   │──│  回复   │       │
│   └─────────┘  └─────────┘  └────┬────┘  └─────────┘       │
│                                  │                          │
│                             ┌────┴────┐                     │
│                             │  转人工  │                     │
│                             └─────────┘                     │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼ JSON (nodes/edges)
┌─────────────────────────────────────────────────────────────┐
│                      Java 后端                               │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              ReactFlowToLiteflowConverter            │   │
│  │         (ReactFlow JSON → LiteFlow EL 表达式)         │   │
│  └─────────────────────────────────────────────────────┘   │
│                            │                                │
│                            ▼                                │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                  LiteFlow 引擎                        │   │
│  │   EL: start_1, llm_1, IF(condition_1, reply_1)      │   │
│  └─────────────────────────────────────────────────────┘   │
│                            │                                │
│                            ▼                                │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                   节点组件执行                         │   │
│  │  StartNode → LlmNode → ConditionNode → ReplyNode    │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 可用节点类型

| 节点类型 | ID | 说明 |
|---------|----|----|
| 开始节点 | `start` | 工作流入口 |
| LLM 节点 | `llm` | 调用大语言模型 |
| 条件节点 | `condition` | 条件分支判断 |
| 回复节点 | `reply` | 设置最终回复 |
| API 节点 | `api` | 调用外部 API |
| 知识库节点 | `knowledge` | 查询知识库 |
| 意图识别节点 | `intent` | 识别用户意图 |
| 转人工节点 | `human_transfer` | 转接人工客服 |
| 变量节点 | `variable` | 操作变量 |
| 结束节点 | `end` | 工作流出口 |

## API 接口

### 1. 工作流 CRUD

#### 获取所有工作流
```
GET /api/v1/ai-workflows
```

#### 获取工作流详情
```
GET /api/v1/ai-workflows/{workflowId}
```

#### 创建工作流
```
POST /api/v1/ai-workflows
Content-Type: application/json

{
  "name": "智能客服工作流",
  "description": "处理客户咨询的基础工作流",
  "nodesJson": "[...]",
  "edgesJson": "[...]",
  "triggerType": "ALL",
  "triggerConfig": null
}
```

#### 更新工作流
```
PUT /api/v1/ai-workflows/{workflowId}
```

#### 删除工作流
```
DELETE /api/v1/ai-workflows/{workflowId}
```

#### 启用/禁用工作流
```
PATCH /api/v1/ai-workflows/{workflowId}/toggle?enabled=true
```

#### 设置默认工作流
```
POST /api/v1/ai-workflows/{workflowId}/set-default
```

### 2. 工作流执行

#### 执行工作流
```
POST /api/v1/ai-workflows/{workflowId}/execute
Content-Type: application/json

{
  "sessionId": "会话ID（可选）",
  "userMessage": "用户输入的消息",
  "variables": {
    "customKey": "customValue"
  }
}
```

#### 测试执行（不保存日志）
```
POST /api/v1/ai-workflows/{workflowId}/test
```

#### 为会话自动匹配执行
```
POST /api/v1/ai-workflows/execute-for-session?sessionId=xxx&userMessage=xxx
```

### 3. 工具接口

#### 验证工作流结构
```
POST /api/v1/ai-workflows/validate
Content-Type: application/json

{
  "nodesJson": "[...]",
  "edgesJson": "[...]"
}
```

#### 预览 LiteFlow EL 表达式
```
POST /api/v1/ai-workflows/preview-el
```

## ReactFlow 数据格式

### 节点格式

```typescript
interface WorkflowNode {
  id: string;           // 唯一标识，如 "llm_1"
  type: string;         // 节点类型，如 "llm"
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

### 边格式

```typescript
interface WorkflowEdge {
  id: string;           // 唯一标识
  source: string;       // 源节点ID
  target: string;       // 目标节点ID
  sourceHandle?: string; // 源节点句柄（用于条件分支，如 "true"/"false"）
  targetHandle?: string;
  label?: string;
}
```

## 节点配置详解

### LLM 节点配置

```json
{
  "model": "gpt-3.5-turbo",
  "systemPrompt": "你是一个智能客服助手...",
  "temperature": 0.7,
  "maxTokens": 1000,
  "useHistory": true
}
```

### 条件节点配置

```json
{
  "conditionType": "contains",
  "value": "退款",
  "sourceType": "userMessage"
}
```

支持的条件类型：
- `contains` - 包含
- `notContains` - 不包含
- `equals` - 等于
- `notEquals` - 不等于
- `startsWith` - 以...开头
- `endsWith` - 以...结尾
- `regex` - 正则匹配
- `isEmpty` - 为空
- `isNotEmpty` - 不为空
- `intentEquals` - 意图等于
- `confidenceGreaterThan` - 意图置信度大于

### 回复节点配置

```json
{
  "replyType": "template",
  "template": "您好，{{customerName}}！关于您的问题：{{lastOutput}}"
}
```

支持的回复类型：
- `template` - 模板回复（支持变量替换）
- `lastOutput` - 使用上一个节点输出
- `nodeOutput` - 使用指定节点输出
- `variable` - 使用变量值

### API 节点配置

```json
{
  "url": "https://api.example.com/query?q={{userMessage}}",
  "method": "POST",
  "headers": {
    "Authorization": "Bearer xxx"
  },
  "body": {
    "query": "{{userMessage}}"
  },
  "responseMapping": "$.data.result",
  "saveToVariable": "apiResult"
}
```

### 意图识别节点配置

```json
{
  "recognitionType": "keyword",
  "intents": [
    {
      "name": "退款",
      "keywords": ["退款", "退钱", "退货"],
      "description": "用户想要退款"
    },
    {
      "name": "咨询",
      "keywords": ["咨询", "问一下", "请问"],
      "description": "用户咨询问题"
    }
  ]
}
```

### 转人工节点配置

```json
{
  "reason": "用户情绪激动",
  "message": "正在为您转接人工客服，请稍候..."
}
```

## 示例工作流

### 简单问答工作流

```json
{
  "nodes": [
    {
      "id": "start_1",
      "type": "start",
      "data": { "label": "开始" },
      "position": { "x": 250, "y": 0 }
    },
    {
      "id": "llm_1",
      "type": "llm",
      "data": {
        "label": "AI 对话",
        "config": {
          "model": "gpt-3.5-turbo",
          "systemPrompt": "你是一个友好的智能客服助手。"
        }
      },
      "position": { "x": 250, "y": 100 }
    },
    {
      "id": "reply_1",
      "type": "reply",
      "data": {
        "label": "回复用户",
        "config": { "replyType": "lastOutput" }
      },
      "position": { "x": 250, "y": 200 }
    },
    {
      "id": "end_1",
      "type": "end",
      "data": { "label": "结束" },
      "position": { "x": 250, "y": 300 }
    }
  ],
  "edges": [
    { "id": "e1", "source": "start_1", "target": "llm_1" },
    { "id": "e2", "source": "llm_1", "target": "reply_1" },
    { "id": "e3", "source": "reply_1", "target": "end_1" }
  ]
}
```

### 带意图识别的工作流

```json
{
  "nodes": [
    { "id": "start_1", "type": "start", "data": { "label": "开始" }, "position": { "x": 250, "y": 0 } },
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
        "config": { "conditionType": "intentEquals", "value": "refund" }
      },
      "position": { "x": 250, "y": 200 }
    },
    {
      "id": "human_1",
      "type": "human_transfer",
      "data": {
        "label": "转人工",
        "config": { "reason": "退款需求", "message": "正在为您转接退款专员..." }
      },
      "position": { "x": 100, "y": 300 }
    },
    {
      "id": "llm_1",
      "type": "llm",
      "data": {
        "label": "AI回答",
        "config": { "systemPrompt": "你是一个客服助手。" }
      },
      "position": { "x": 400, "y": 300 }
    },
    { "id": "end_1", "type": "end", "data": { "label": "结束" }, "position": { "x": 250, "y": 400 } }
  ],
  "edges": [
    { "id": "e1", "source": "start_1", "target": "intent_1" },
    { "id": "e2", "source": "intent_1", "target": "condition_1" },
    { "id": "e3", "source": "condition_1", "target": "human_1", "sourceHandle": "true" },
    { "id": "e4", "source": "condition_1", "target": "llm_1", "sourceHandle": "false" },
    { "id": "e5", "source": "human_1", "target": "end_1" },
    { "id": "e6", "source": "llm_1", "target": "end_1" }
  ]
}
```

## 配置说明

### application.yml

```yaml
# LiteFlow 工作流引擎配置
liteflow:
  rule-source:                    # 留空，使用代码动态加载
  print-execution-log: true       # 打印执行日志
  when-max-wait-seconds: 60       # 并行执行最大等待时间
  when-max-workers: 16            # 并行执行线程数
  monitor:
    enable-log: true
    period: 300000

# AI 配置
ai:
  openai:
    api-key: ${OPENAI_API_KEY:}
    base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
```

### 环境变量

```bash
# OpenAI API 配置
OPENAI_API_KEY=sk-xxx
OPENAI_BASE_URL=https://api.openai.com/v1
```

## 数据库

执行以下 SQL 脚本创建必要的表：

```bash
mysql -u root -p your_database < db/create_ai_workflows.sql
```

## 前端集成示例

### React + ReactFlow

```tsx
import ReactFlow, { 
  Node, 
  Edge,
  Controls,
  Background 
} from 'reactflow';
import 'reactflow/dist/style.css';

const WorkflowEditor: React.FC = () => {
  const [nodes, setNodes] = useState<Node[]>([]);
  const [edges, setEdges] = useState<Edge[]>([]);

  // 保存工作流
  const handleSave = async () => {
    const response = await fetch('/api/v1/ai-workflows', {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        name: '我的工作流',
        nodesJson: JSON.stringify(nodes),
        edgesJson: JSON.stringify(edges),
        triggerType: 'ALL'
      })
    });
    
    if (response.ok) {
      alert('保存成功！');
    }
  };

  // 测试工作流
  const handleTest = async (workflowId: string) => {
    const response = await fetch(`/api/v1/ai-workflows/${workflowId}/test`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userMessage: '我想退款',
        variables: {}
      })
    });
    
    const result = await response.json();
    console.log('测试结果:', result);
  };

  return (
    <div style={{ width: '100%', height: '600px' }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
      >
        <Controls />
        <Background />
      </ReactFlow>
      <button onClick={handleSave}>保存工作流</button>
    </div>
  );
};
```

## 注意事项

1. **节点 ID 规范**：建议使用 `类型_序号` 格式，如 `llm_1`、`condition_1`
2. **条件节点分支**：使用 `sourceHandle` 区分分支，值为 `"true"` 或 `"false"`
3. **变量引用**：在模板中使用 `{{variableName}}` 格式引用变量
4. **错误处理**：所有节点都有错误处理，执行失败不会中断整个工作流
5. **执行日志**：每次执行都会记录详细日志，可用于调试和监控

