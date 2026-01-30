# Soncho AI 后端技术设计

## 1. 系统概览
Soncho AI 是一个聚合多渠道消息、AI 辅助客服与团队协作能力的一体化工作台。本后端以 Spring Boot 3 为核心，结合 REST API + WebSocket 的混合通信模式，支撑坐席从登录、收件箱管理、AI 能力调用到团队协同的全链路体验。系统可同时接入网页、WhatsApp、LINE、微信、公众号、Email 等渠道，并能将平台内回复回推至对应渠道。

## 2. 技术栈
- Spring Boot 3.2（Web、WebSocket、Validation、Actuator）
- Spring Data JPA + MySQL 8（JSON / UUID）
- Spring Security（自定义 Token + Bearer 鉴权）
- Jackson（JSON、WebSocket Payload）
- SockJS/WebSocket（实时事件）

## 3. 核心模块
1. **认证与权限**：`AuthController` 基于 `AuthenticationManager + AgentAuthenticationProvider` 完成邮箱密码登录，`TokenAuthenticationFilter` 将 Bearer Token 注入安全上下文，实现 `/api/v1/**` 默认鉴权。
2. **聚合渠道网关**：`ChannelMessageController` 接收外部渠道消息（`ChannelInboundRequest`），统一转换为 `ChannelMessage`，调用 `AiAssistantService` 与 `ChannelRouter`；新增渠道只需实现 `ChannelAdapter` 即可接入。
3. **会话与消息编排**：`ConversationService` 负责 `sendMessage / updateSessionStatus / updateUserProfile`，写入关系数据库，并触发 ChannelRouter 将非内部消息转发至对应渠道，同时标准化 mentions、附件等结构化数据。
4. **AI 能力服务**：`AiKnowledgeService` 提供摘要、魔法改写、AI 标签建议；`AiAssistantService`（示例使用词匹配）可无缝替换为 Gemini 等 LLM。
5. **实时工作台**：`ChatWebSocketHandler + WebSocketEventService` 支持 `sendMessage`、`updateSessionStatus`、`agentTyping`、`updateUserProfile` 等客户端事件，并推送 `newMessage`、`sessionUpdated`、`userProfileUpdated`、`notification` 等服务端事件。
6. **管理与配置**：`AdminController` 暴露团队成员、角色的 CRUD；`BootstrapController` 一次性返回 `sessions/agents/groups/roles/quickReplies/knowledgeBase` 作为工作台初始数据。

## 4. API 设计
所有 REST 端点以 `/api/v1` 为前缀并要求 Bearer Token：
- `POST /auth/login`，`GET /auth/me`
- `GET /bootstrap`
- `POST /ai/summary`、`POST /ai/rewrite`、`POST /ai/suggest-tags`
- `GET /admin/agents`、`POST /admin/agents`
- `GET /admin/roles`、`PUT /admin/roles/{id}`
- `POST /channels/{channel}/messages`：外部渠道统一入口

响应统一使用 DTO（如 `AgentDto`、`ChatSessionDto`、`MessageDto`），保证前端只接收净化后的字段。大部分写操作开启事务并返回最新对象状态，方便前端直接合并状态。

## 5. WebSocket 事件
- **客户端 -> 服务端**：`sendMessage`（包含 mentions、附件）、`updateSessionStatus`（RESOLVE/TOGGLE_AI/TRANSFER）、`updateUserProfile`、`agentTyping`。
- **服务端 -> 客户端**：
  - `newMessage`：附带 `MessageDto`
  - `sessionUpdated`：返回最新状态/负责人
  - `userProfileUpdated`
  - `agentTyping`：广播坐席输入状态
  - `notification`：通用反馈（成功、失败、警告）
若 payload 缺失 `event` 字段，则按 `ChannelMessage` 解析，保留机器人直接对话能力。

## 6. 数据模型（MySQL 8）
所有实体以 UUID 为主键、`AuditableEntity` 统一维护 `created_at/updated_at`。
- `agents / roles`：记录坐席、角色及 JSON 权限
- `users`：客户画像（来源渠道、标签、AI 标签、内部笔记）
- `chat_sessions`：状态（AI_HANDLING/HUMAN_HANDLING/RESOLVED）、分组、负责人、协作人列表
- `messages`：`sender_type`、`is_internal`、`translation_data`、**mentions（message_mentions 表中的 agentId 列表）**、附件（`attachments` 表）
- `quick_replies / knowledge_entries / chat_groups`：系统效率工具

## 7. 安全与鉴权
- 登录采用邮箱 + 密码，PasswordEncoder = BCrypt
- `AgentAuthenticationProvider` 负责校验凭据并生成 `AgentPrincipal`
- `InMemoryTokenService` 基于 UUID 生成 Bearer Token，可根据部署替换为 JWT（只需实现 `TokenService`）
- `TokenAuthenticationFilter` 解析 Authorization Header，将 AgentPrincipal 写入 `SecurityContext`
- 安全策略：`/auth/login`、`/actuator/**` 放行，其余端点默认鉴权，可通过 `@PreAuthorize` 精细化控制

## 8. 多渠道与 mentions 设计
- `ChannelMessage` 新增 `mentions` 字段，所有客户端上报的 `@提及` 会被拆分为坐席 ID 数组，与消息主体分开存储/转发。
- `Message` 表的 `mentionAgentIds` 采用 `message_mentions` 映射表，方便后端根据 mentions 触发通知或权限校验。
- 任意渠道消息在落库后，通过 `ChannelRouter` 分发至具体 `ChannelAdapter`（示例实现 WhatsApp/Email），Outbound 消息同样保留 mentions 元信息，方便渠道能力扩展。

## 9. 扩展与运维
- **新增渠道**：实现 `ChannelAdapter`，Spring 自动注册后即可被 `ChannelRouter` 发现。
- **替换 Token/JWT**：实现 `TokenService` 即可（已在安全配置中解耦）。
- **引入真 AI 模型**：实现 `AiAssistantService` / 扩展 `AiKnowledgeService`，即可串接 Gemini、OpenAI 或自研模型。
- **数据库迁移**：生产可接入 Flyway/Liquibase，在当前实体基础上生成脚本。
- **监控**：Actuator 默认开启，结合 Prometheus/Grafana 监控响应与连接情况。
