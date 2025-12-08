# 知识库向量存储 API 文档

## 概述

知识库模块使用 **Redis Stack** 作为向量数据库，配合 **LangChain4j** 进行文本嵌入和语义搜索。支持多知识库管理，自动分块和向量化，以及在 AI 工作流中的知识检索节点使用。

## 架构

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   知识库 API    │────▶│  向量存储服务   │────▶│   Redis Stack   │
│  Controller     │     │ VectorStoreService│   │  (RediSearch)   │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                               │
                               ▼
                        ┌─────────────────┐
                        │  嵌入模型服务   │
                        │  (LangChain4j)  │
                        └─────────────────┘
```

## 前置要求

### 1. Redis Stack 安装

知识库需要 **Redis Stack**（包含 RediSearch 模块）才能使用向量搜索功能。

**Docker 安装：**
```bash
docker run -d --name redis-stack \
  -p 6379:6379 \
  -p 8001:8001 \
  redis/redis-stack:latest
```

**验证安装：**
```bash
redis-cli
> FT._LIST  # 应返回空列表，不报错
```

### 2. 嵌入模型配置

配置 OpenAI API Key（或其他嵌入模型）：
```bash
export OPENAI_API_KEY=your-api-key
```

## API 接口

### 知识库管理

#### 获取所有知识库
```http
GET /api/v1/knowledge-bases?enabledOnly=false
```

**响应：**
```json
[
  {
    "id": "uuid",
    "name": "产品FAQ",
    "description": "产品常见问题知识库",
    "indexName": "kb_xxx",
    "embeddingModelId": null,
    "vectorDimension": 1536,
    "documentCount": 10,
    "enabled": true,
    "createdAt": "2025-11-29T10:00:00Z",
    "updatedAt": "2025-11-29T10:00:00Z"
  }
]
```

#### 创建知识库
```http
POST /api/v1/knowledge-bases
Content-Type: application/json

{
  "name": "产品FAQ",
  "description": "产品常见问题知识库",
  "embeddingModelId": null,  // 可选，不填使用默认 OpenAI 嵌入模型
  "vectorDimension": 1536    // 可选，默认 1536
}
```

#### 更新知识库
```http
PUT /api/v1/knowledge-bases/{id}
Content-Type: application/json

{
  "name": "新名称",
  "description": "新描述",
  "enabled": true
}
```

#### 删除知识库
```http
DELETE /api/v1/knowledge-bases/{id}
```

#### 重建知识库索引
```http
POST /api/v1/knowledge-bases/{id}/rebuild
```

### 文档管理

#### 获取文档列表
```http
GET /api/v1/knowledge-bases/{kbId}/documents?page=0&size=20
```

**响应：**
```json
{
  "content": [
    {
      "id": "uuid",
      "knowledgeBaseId": "uuid",
      "title": "退款政策",
      "content": "文档内容...",
      "docType": "TEXT",
      "sourceUrl": null,
      "chunkSize": 500,
      "chunkOverlap": 50,
      "chunkCount": 5,
      "status": "COMPLETED",
      "errorMessage": null,
      "createdAt": "2025-11-29T10:00:00Z",
      "updatedAt": "2025-11-29T10:00:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

#### 添加文档
```http
POST /api/v1/knowledge-bases/{kbId}/documents
Content-Type: application/json

{
  "title": "退款政策",
  "content": "我们的退款政策如下：...",
  "docType": "TEXT",        // TEXT, MARKDOWN, HTML, PDF, URL
  "sourceUrl": null,        // 可选，文档来源 URL
  "chunkSize": 500,         // 可选，分块大小（字符数）
  "chunkOverlap": 50,       // 可选，分块重叠（字符数）
  "metadata": "{}"          // 可选，自定义元数据 JSON
}
```

**文档状态流程：**
```
PENDING → PROCESSING → COMPLETED
                   ↘ FAILED
```

#### 批量添加文档
```http
POST /api/v1/knowledge-bases/{kbId}/documents/batch
Content-Type: application/json

[
  { "title": "文档1", "content": "内容1..." },
  { "title": "文档2", "content": "内容2..." }
]
```

#### 更新文档
```http
PUT /api/v1/knowledge-bases/{kbId}/documents/{docId}
Content-Type: application/json

{
  "title": "新标题",
  "content": "新内容...",
  "chunkSize": 800
}
```
> 注：修改 content、chunkSize 或 chunkOverlap 会触发重新向量化

#### 删除文档
```http
DELETE /api/v1/knowledge-bases/{kbId}/documents/{docId}
```

#### 重新处理文档
```http
POST /api/v1/knowledge-bases/{kbId}/documents/{docId}/reprocess
```

### 搜索

#### 搜索单个知识库
```http
POST /api/v1/knowledge-bases/{kbId}/search
Content-Type: application/json

{
  "query": "如何申请退款",
  "maxResults": 5,    // 可选，默认 5
  "minScore": 0.7     // 可选，最小相似度，默认 0.7
}
```

**响应：**
```json
[
  {
    "content": "退款申请流程：1. 登录账户 2. 找到订单...",
    "score": 0.92,
    "documentId": "uuid",
    "title": "退款政策"
  }
]
```

#### 搜索多个知识库
```http
POST /api/v1/knowledge-bases/search
Content-Type: application/json

{
  "knowledgeBaseIds": ["uuid1", "uuid2"],
  "query": "如何申请退款",
  "maxResults": 5,
  "minScore": 0.7
}
```

## 工作流节点配置

在 AI 工作流中使用 `knowledge` 节点进行知识检索：

### 节点配置示例

```json
{
  "id": "node123",
  "type": "knowledge",
  "data": {
    "label": "知识库检索",
    "config": {
      "knowledgeBaseId": "uuid",           // 单个知识库
      // 或
      "knowledgeBaseIds": ["uuid1", "uuid2"], // 多个知识库
      
      "querySource": "query",              // query | lastOutput | custom
      "customQuery": "{{sys.query}}",      // querySource=custom 时使用
      
      "maxResults": 3,                     // 返回结果数量
      "minScore": 0.7,                     // 最小相似度
      
      "outputFormat": "combined",          // combined | list | first | json
      "noResultMessage": "未找到相关知识",
      "errorMessage": "知识库查询失败"
    }
  }
}
```

### 输出格式说明

| 格式 | 说明 |
|------|------|
| `combined` | 所有结果内容用 `\n\n---\n\n` 连接 |
| `list` | Markdown 列表格式，包含标题和相似度 |
| `first` | 只返回第一个结果的内容 |
| `json` | JSON 数组格式 |

### 上下文变量

执行后，以下变量会保存到工作流上下文：

| 变量名 | 类型 | 说明 |
|--------|------|------|
| `knowledgeResults` | `List<SearchResult>` | 完整搜索结果 |
| `knowledgeContent` | `String` | 格式化后的内容 |
| `knowledgeResultCount` | `Integer` | 结果数量 |
| `knowledgeContents` | `List<String>` | 所有内容列表 |

### 结合 LLM 使用示例

```
[Start] → [Knowledge] → [LLM] → [Reply] → [End]
```

LLM 节点配置：
```json
{
  "type": "llm",
  "data": {
    "config": {
      "systemPrompt": "你是一个客服助手，请根据以下知识库内容回答用户问题：\n\n{{var.knowledgeContent}}",
      "messages": [
        { "role": "user", "content": "{{sys.query}}" }
      ]
    }
  }
}
```

## 嵌入模型管理

### 支持的嵌入模型

| 提供商 | 模型 | 维度 | 说明 |
|--------|------|------|------|
| OpenAI | text-embedding-3-small | 1536 | 推荐，性价比高 |
| OpenAI | text-embedding-3-large | 3072 | 效果最佳 |
| Azure OpenAI | - | - | 通过部署名配置 |
| Ollama | nomic-embed-text | 768 | 本地运行 |

### 配置嵌入模型

在 `llm_models` 表中添加类型为 `EMBEDDING` 的模型：

```sql
INSERT INTO llm_models (id, name, code, provider, model_type, model_name, base_url, api_key, enabled, is_default)
VALUES (UUID_TO_BIN(UUID()), 'OpenAI Embedding', 'openai-embedding', 'OPENAI', 'EMBEDDING',
        'text-embedding-3-small', 'https://api.openai.com/v1', 'your-api-key', 1, 1);
```

## 最佳实践

### 1. 文档分块

- **chunkSize**: 建议 300-800 字符，太大会降低检索精度，太小会丢失上下文
- **chunkOverlap**: 建议设为 chunkSize 的 10%-20%，保持上下文连贯

### 2. 相似度阈值

- **0.8+**: 高度相关，适合精准匹配场景
- **0.7-0.8**: 相关性较好，通用推荐值
- **0.6-0.7**: 较宽松，可能包含边缘相关内容

### 3. 知识库组织

- 按主题分类创建多个知识库（如：产品FAQ、售后政策、技术文档）
- 工作流中可根据意图选择不同知识库检索

## 数据库表结构

### knowledge_bases
```sql
CREATE TABLE knowledge_bases (
    id BINARY(16) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    index_name VARCHAR(100) NOT NULL UNIQUE,
    embedding_model_id BINARY(16),
    vector_dimension INT DEFAULT 1536,
    document_count INT DEFAULT 0,
    enabled TINYINT(1) DEFAULT 1,
    created_by_agent_id BINARY(16),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### knowledge_documents
```sql
CREATE TABLE knowledge_documents (
    id BINARY(16) PRIMARY KEY,
    knowledge_base_id BINARY(16) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content LONGTEXT NOT NULL,
    doc_type VARCHAR(20) DEFAULT 'TEXT',
    source_url VARCHAR(500),
    chunk_size INT DEFAULT 500,
    chunk_overlap INT DEFAULT 50,
    chunk_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING',
    error_message VARCHAR(1000),
    metadata_json TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

## 故障排查

### 1. Redis 连接失败
检查 Redis Stack 是否运行，以及 `application.yml` 中的连接配置。

### 2. 文档处理失败
查看文档的 `errorMessage` 字段，常见原因：
- 嵌入模型 API Key 无效
- 文档内容过长
- 网络问题

### 3. 搜索无结果
- 检查文档状态是否为 `COMPLETED`
- 降低 `minScore` 阈值
- 确认查询文本与文档内容有语义相关性


