# System Index: AI客服系统代码索引

该文件提供了 `ai_kef` 项目的宏观导航，帮助开发者快速定位业务模块、核心功能和关键组件。

## Part 1: 模块与包全景图 (Global Module Map)

| 业务模块名称 (Module) | 对应包路径 (Package Path) | 核心职责 (Responsibility) |
| :--- | :--- | :--- |
| **认证与安全** | `com.example.aikef.security` | 负责客服/客户登录、Token校验、权限控制 |
| **即时通讯** | `com.example.aikef.controller` (Chat/Message)<br>`com.example.aikef.websocket` | 处理WebSocket连接、消息收发、会话管理 |
| **工作流引擎** | `com.example.aikef.workflow` | AI Agent 编排、节点执行、Liteflow 集成 |
| **客户管理** | `com.example.aikef.service` (Customer*Service)<br>`com.example.aikef.model` (Customer*) | 客户档案、标签、画像管理 |
| **AI 能力中心** | `com.example.aikef.llm`<br>`com.example.aikef.tool` | LLM 模型对接、AI 工具调用、Prompt 管理 |
| **事件与触发器** | `com.example.aikef.controller` (EventController)<br>`com.example.aikef.service` (EventService)<br>`com.example.aikef.model` (Event*) | 外部事件/钩子驱动工作流执行 |
| **会话组织与已读** | `com.example.aikef.controller` (SessionGroup/Category/Note/ReadRecord)<br>`com.example.aikef.service` | 会话分类、分组、备注与未读统计 |
| **模型管理** | `com.example.aikef.llm` (LlmModelService)<br>`com.example.aikef.controller` (LlmModelController) | LLM 模型配置、启用、测试 |
| **结构化提取** | `com.example.aikef.extraction` | 表单化信息抽取、Schema 与会话管理 |
| **文件与存储** | `com.example.aikef.storage`<br>`com.example.aikef.controller` (FileController) | 文件上传、下载与存储适配 |
| **多语言翻译** | `com.example.aikef.service` (TranslationService)<br>`com.example.aikef.config` (TranslationConfig) | 语言检测与消息翻译 |
| **多渠道集成** | `com.example.aikef.channel`<br>`com.example.aikef.service.channel` | 对接微信、WhatsApp、Email 等外部渠道适配器 |
| **知识库** | `com.example.aikef.knowledge` | RAG 检索、向量存储、文档管理 |
| **SaaS 租户** | `com.example.aikef.saas` | 多租户数据隔离、上下文管理 |
| **基础设施** | `com.example.aikef.config`<br>`com.example.aikef.advice` | 数据库配置、全局异常处理、Redis 配置 |

## Part 2: 功能反向索引表 (Feature-to-Code Index)

### [认证与用户] Auth & User
| 功能名称 (Feature) | 触发关键词 (Keywords) | 涉及的核心 Java 文件 (Related Files) | 备注 |
| :--- | :--- | :--- | :--- |
| **客服登录** | login, auth, admin | 1. `AuthController.java` (入口)<br>2. `AgentAuthenticationProvider.java` (校验)<br>3. `TokenService.java` (Token生成) | 支持邮箱密码登录 |
| **客户匿名/快速登录** | quick, customer, token | 1. `PublicController.java` (`customer-token`)<br>2. `CustomerTokenService.java` | 用于 Widget 访客模式 |
| **Token 校验** | token, validate | 1. `PublicController.java` (`validate-token`)<br>2. `TokenService.java` | 校验客服/客户 Token 有效性 |
| **系统初始化** | bootstrap, init, config | 1. `BootstrapController.java`<br>2. `BootstrapService.java` | 前端加载时拉取全量配置 |
| **客服管理** | agent, create, update | 1. `AdminController.java`<br>2. `AgentService.java` | 增删改查客服账号 |
| **角色权限** | role, permission | 1. `CustomerRoleController.java` / `RoleService.java` | 角色与权限配置 |

### [即时通讯] Chat & Session
| 功能名称 (Feature) | 触发关键词 (Keywords) | 涉及的核心 Java 文件 (Related Files) | 备注 |
| :--- | :--- | :--- | :--- |
| **发送消息** | send, message, chat | 1. `ChatController.java` (`sendMessage`)<br>2. `MessageService.java`<br>3. `SessionMessageGateway.java` | 统一消息入口 |
| **会话管理** | session, create, list | 1. `ChatController.java`<br>2. `ChatSessionService.java` | 会话列表、详情、状态流转 |
| **WebSocket 连接** | ws, connect, handshake | 1. `ChatWebSocketHandler.java`<br>2. `WebSocketConfig.java` | 实时双向通信 |
| **WebSocket 事件处理** | event, realtime | 1. `ChatWebSocketHandler.java`<br>2. `WebSocketEventService.java` | 实时事件分发与业务处理 |
| **会话转接** | transfer, assign | 1. `ChatController.java`<br>2. `ChatSessionService.java` | 转接给其他客服或组 |
| **快捷回复** | quick reply, canned | 1. `QuickReplyController.java`<br>2. `QuickReplyService.java` | 客服常用语管理 |
| **会话分组** | group, folder | 1. `SessionGroupController.java`<br>2. `SessionGroupService.java` | 自定义会话分组与分类绑定 |
| **会话分类** | category, topic | 1. `SessionCategoryController.java`<br>2. `SessionCategoryService.java` | 会话分类管理 |
| **会话备注** | note, remark | 1. `SessionNoteController.java`<br>2. `ChatSessionService.java` | 会话备注增删改查 |
| **已读/未读统计** | read, unread | 1. `ReadRecordController.java`<br>2. `ReadRecordService.java` | 已读时间与未读数 |
| **消息网关与外发** | gateway, outbound | 1. `SessionMessageGateway.java`<br>2. `ExternalPlatformService.java`<br>3. `OfficialChannelMessageService.java` | 消息持久化、广播与外发 |
| **离线消息补发** | offline, delivery | 1. `OfflineMessageService.java`<br>2. `MessageDeliveryRepository.java` | 客服离线消息补发 |
| **消息附件管理** | attachment, file | 1. `Attachment.java`<br>2. `AttachmentRepository.java`<br>3. `FileUploadService.java` | 消息附件存储与关联 |

### [AI 工作流] Workflow & Agent
| 功能名称 (Feature) | 触发关键词 (Keywords) | 涉及的核心 Java 文件 (Related Files) | 备注 |
| :--- | :--- | :--- | :--- |
| **工作流执行** | workflow, execute, run | 1. `AiWorkflowController.java`<br>2. `AiWorkflowService.java` | 触发 Liteflow 流程 |
| **工作流测试** | test, debug, dry-run | 1. `WorkflowTestController.java`<br>2. `WorkflowTestService.java` | 调试模式运行工作流 |
| **节点逻辑** | node, component | 1. `AgentNode.java` (LLM对话)<br>2. `ToolNode.java` (工具调用)<br>3. `IntentNode.java` (意图识别) | 位于 `workflow/node` 包 |
| **AI 润色/重写** | rewrite, polish | 1. `AiController.java` (`rewrite`)<br>2. `AiAssistantService.java` | 客服输入框辅助功能 |
| **会话总结** | summary, summarize | 1. `AiController.java`<br>2. `SessionSummaryService.java` | 自动生成会话小结 |
| **AI 工具管理** | tool, function | 1. `AiToolController.java`<br>2. `AiToolService.java` | 管理 Function Calling 工具 |
| **AI 定时任务** | scheduled, task, cron | 1. `AiScheduledTaskController.java`<br>2. `AiScheduledTaskService.java` | 定时触发工作流 |
| **工作流生成** | generate, prompt | 1. `WorkflowGeneratorController.java`<br>2. `WorkflowGeneratorService.java` | 根据提示生成或修改工作流 |
| **事件触发** | event, hook | 1. `EventController.java`<br>2. `EventService.java` | 事件绑定并触发工作流 |
| **工作流执行调度** | scheduler, debounce | 1. `WorkflowExecutionScheduler.java`<br>2. `AiWorkflowService.java` | 防抖执行与队列调度 |

### [AI 能力] AI Capabilities
| 功能名称 (Feature) | 触发关键词 (Keywords) | 涉及的核心 Java 文件 (Related Files) | 备注 |
| :--- | :--- | :--- | :--- |
| **LLM 模型管理** | model, provider, llm | 1. `LlmModelController.java`<br>2. `LlmModelService.java` | 模型配置、启用、测试 |
| **系统提示词增强** | prompt, system | 1. `SystemPromptController.java`<br>2. `SystemPromptEnhancementService.java` | Prompt 自动优化 |
| **结构化信息抽取** | extraction, schema | 1. `ExtractionController.java`<br>2. `StructuredExtractionService.java` | Schema/会话式抽取 |

### [客户与渠道] Customer & Channel
| 功能名称 (Feature) | 触发关键词 (Keywords) | 涉及的核心 Java 文件 (Related Files) | 备注 |
| :--- | :--- | :--- | :--- |
| **客户档案** | customer, profile | 1. `CustomerController.java`<br>2. `CustomerService.java` | 客户信息 CRUD |
| **客户标签** | tag, label | 1. `CustomerTagController.java`<br>2. `CustomerTagService.java` | 客户画像标签 |
| **第三方平台 Webhook** | webhook, platform | 1. `WebhookController.java`<br>2. `ExternalPlatformService.java` | 接收第三方平台消息与会话映射 |
| **第三方平台配置** | platform, config | 1. `WebhookController.java`<br>2. `ExternalPlatformRepository.java` | 管理外部平台接入参数 |
| **官方渠道 Webhook** | webhook, official | 1. `OfficialChannelWebhookController.java`<br>2. `OfficialChannelMessageService.java` | 处理官方渠道回调 |
| **渠道配置** | channel, config | 1. `OfficialChannelController.java`<br>2. `OfficialChannelService.java` | 配置微信/Whatsapp参数 |
| **渠道入站消息** | channel, inbound | 1. `ChannelMessageController.java`<br>2. `ChannelRouter.java`<br>3. `AiAssistantService.java` | 接收渠道消息并路由回复 |

### [知识库] Knowledge Base
| 功能名称 (Feature) | 触发关键词 (Keywords) | 涉及的核心 Java 文件 (Related Files) | 备注 |
| :--- | :--- | :--- | :--- |
| **文档上传/解析** | upload, parse, doc | 1. `KnowledgeBaseController.java`<br>2. `KnowledgeBaseService.java` | 处理文档入库 |
| **向量检索** | search, vector, rag | 1. `AiKnowledgeService.java`<br>2. `VectorStoreService.java` | 语义搜索核心逻辑 |

### [系统支撑] Platform Utilities
| 功能名称 (Feature) | 触发关键词 (Keywords) | 涉及的核心 Java 文件 (Related Files) | 备注 |
| :--- | :--- | :--- | :--- |
| **文件上传/下载** | file, upload, download | 1. `FileController.java`<br>2. `FileUploadService.java`<br>3. `StorageProvider.java` | 附件与图片资源管理 |
| **多语言翻译** | translation, detect | 1. `TranslationController.java`<br>2. `TranslationService.java` | 语言检测与翻译能力 |
| **健康检查** | health, ping | 1. `HealthController.java` | 服务探活与监控接入 |

## Part 3: 核心架构组件 (Core Components)

| 组件名称 | 类名 (Class Name) | 作用简述 |
| :--- | :--- | :--- |
| **全局异常处理** | `GlobalResponseAdvice.java` | 统一捕获异常并封装标准 JSON 响应 |
| **安全配置** | `SecurityConfig.java` | Spring Security 配置链，定义公开/保护路径 |
| **WebSocket 配置** | `WebSocketConfig.java` | 注册 WS 端点与拦截器 |
| **WebSocket 会话管理** | `WebSocketSessionManager.java` | 在线连接管理与消息广播 |
| **WebSocket 事件处理** | `WebSocketEventService.java` | WebSocket 事件分发与业务入口 |
| **消息发送网关** | `SessionMessageGateway.java` | 消息统一发送、翻译与外部转发 |
| **会话消息服务** | `ConversationService.java` | 会话消息主链路与路由 |
| **离线消息服务** | `OfflineMessageService.java` | 客服离线消息补发 |
| **消息投递记录** | `MessageDelivery.java` | 客服消息送达状态追踪 |
| **附件实体** | `Attachment.java` | 消息附件元数据 |
| **上传文件实体** | `UploadedFile.java` | 文件存储元数据 |
| **工作流执行调度** | `WorkflowExecutionScheduler.java` | 防抖与串行执行调度 |
| **租户上下文** | `TenantContext.java` | ThreadLocal 存储当前请求的租户 ID |
| **SaaS 配置** | `SaasConfig.java` | 多租户开关与拦截器配置 |
| **租户拦截器** | `TenantInterceptor.java` | 请求级租户解析与上下文注入 |
| **租户实体监听** | `TenantEntityListener.java` | 持久化写入时自动填充租户信息 |
| **租户过滤切面** | `TenantHibernateFilterAspect.java` | Hibernate 过滤器启停管理 |
| **实体映射器** | `EntityMapper.java` | MapStruct 接口，负责 Entity 与 DTO 互转 |
| **数据初始化** | `DataInitializer.java` | 系统启动时预加载初始数据 (Admin, Roles) |
| **Redis 令牌** | `RedisTokenService.java` | 分布式 Token 存储与校验 |
| **工作流上下文** | `WorkflowContext.java` | 存储工作流执行过程中的变量与状态 |
| **多语言配置** | `TranslationConfig.java` | 国际化资源加载 |
