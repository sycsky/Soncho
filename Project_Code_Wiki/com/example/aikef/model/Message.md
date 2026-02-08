# Message

## 1. 类档案 (Class Profile)
- **功能定义**：聊天消息实体类，存储会话中的文本消息、附件、发送者信息以及相关的元数据（如翻译、Agent执行数据）。
- **注解与配置**：
  - `@Entity`: JPA 实体。
  - `@Table(name = "messages")`: 映射数据库表 `messages`。
  - `@AttributeOverrides`: 覆盖基类审计字段的列属性（精确到微秒 DATETIME(6)）。
- **继承/实现**：继承自 `AuditableEntity`（包含 `id`, `createdAt`, `updatedAt`）。

## 2. 核心方法详解 (Method Deep Dive)
*注：本类为贫血模型（Anemic Model），主要由 Getter/Setter 组成，无复杂业务逻辑方法。*

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| (无业务方法) | - | - | - |

> **字段特殊处理说明**：
> - `agentMetadata`, `translationData`, `toolCallData`: 使用 `@JdbcTypeCode(SqlTypes.JSON)` 映射为 MySQL JSON 类型，支持存储非结构化扩展数据。
> - `mentionAgentIds`: 使用 `@ElementCollection` 映射为关联表 `message_mentions`，存储被提及的 Agent ID 列表。

## 3. 依赖全景 (Dependency Graph)

### 关联实体 (Relationships)
- **`ChatSession`** (`@ManyToOne`): 多对一关联所属会话，延迟加载。
- **`Agent`** (`@ManyToOne`): 多对一关联发送消息的客服（如果发送者是 Agent）。
- **`Attachment`** (`@OneToMany`): 一对多关联消息附件，级联删除 (`CascadeType.ALL`, `orphanRemoval = true`)。

### 依赖的枚举/类型
- **`SenderType`**: 枚举，标识发送者类型（如 CUSTOMER, AGENT, SYSTEM）。

### 配置依赖
- 依赖 Hibernate 的 JSON 类型支持 (`org.hibernate.type.SqlTypes`)。

## 4. 调用指南 (Usage Guide)

### 实例化方式
通常通过 `new` 关键字创建，或由 JPA 查询返回。

### 代码示例
**构建并保存一条新消息**

```java
Message message = new Message();
message.setSession(chatSession);
message.setSenderType(SenderType.CUSTOMER);
message.setText("Hello, I need help.");
message.setReadByAgent(false);

// 设置 JSON 元数据
Map<String, Object> meta = new HashMap<>();
meta.put("source", "widget");
message.setAgentMetadata(meta);

messageRepository.save(message);
```
