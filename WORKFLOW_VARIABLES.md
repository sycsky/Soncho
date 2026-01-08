# 工作流变量占位符使用指南

在工作流中（如 AI 回复、工具参数、节点配置等），你可以使用 `{{namespace.key}}` 或 `{{key}}` 的格式来引用动态变量。

## 1. 系统变量 (sys)

最常用的内置变量，获取当前会话和系统状态。

| 变量表达式 | 简写 (可选) | 说明 |
| :--- | :--- | :--- |
| `{{sys.query}}` | `{{query}}` | 用户发送的最新消息内容 |
| `{{sys.lastOutput}}` | `{{lastOutput}}` | 上一个节点的执行输出结果 |
| `{{sys.intent}}` | `{{intent}}` | AI 识别到的用户意图 |
| `{{sys.intentConfidence}}` | - | 意图识别的置信度 (0.00-1.00) |
| `{{sys.sessionId}}` | - | 当前会话 ID |
| `{{sys.customerId}}` | - | 当前客户 ID |
| `{{sys.now}}` | - | 当前日期 (格式: yyyy-MM-dd) |
| `{{sys.needHumanTransfer}}` | - | 是否标记为需要转人工 ("true"/"false") |

## 2. 自定义变量 (var)

引用你在工作流中设置的变量（例如通过“变量设置”节点）。

| 变量表达式 | 说明 |
| :--- | :--- |
| `{{var.orderId}}` | 获取名为 `orderId` 的变量值 |
| `{{var.myCustomKey}}` | 获取名为 `myCustomKey` 的变量值 |

## 3. 客户信息 (customer)

获取当前对话客户的详细资料。

| 变量表达式 | 说明 |
| :--- | :--- |
| `{{customer.name}}` | 客户姓名 |
| `{{customer.email}}` | 客户邮箱 |
| `{{customer.phone}}` | 客户电话 |
| `{{customer.id}}` | 客户 ID |

## 4. 节点输出 (node)

获取特定节点的执行结果。**注意**：只能引用已经执行过的节点。

| 变量表达式 | 说明 |
| :--- | :--- |
| `{{node.nodeId}}` | 获取指定 `nodeId` 节点的输出内容 |
| `{{node.llm_1}}` | 例如：获取 ID 为 `llm_1` 的 LLM 节点回复 |

## 5. 实体提取 (entity)

获取 NLU/LLM 节点从用户消息中提取到的实体信息。

| 变量表达式 | 说明 |
| :--- | :--- |
| `{{entity.productName}}` | 获取名为 `productName` 的实体值 |
| `{{entity.time}}` | 获取名为 `time` 的实体值 |

## 6. 会话元数据 (meta)

获取会话的元数据（通常由前端或渠道传入）。

| 变量表达式 | 说明 |
| :--- | :--- |
| `{{meta.source}}` | 会话来源 (如 wechat, web) |
| `{{meta.clientIp}}` | 客户端 IP |
| `{{meta.browser}}` | 浏览器信息 |

## 7. 事件数据 (event)

**仅用于 Webhook 触发的工作流**。获取外部 Webhook 推送的 JSON 数据。

| 变量表达式 | 说明 |
| :--- | :--- |
| `{{event.order_status}}` | 获取 Webhook payload 中的 `order_status` 字段 |
| `{{event.user_id}}` | 获取 Webhook payload 中的 `user_id` 字段 |

## 8. 工具参数 (局部变量)

在**API 工具配置**中，可以直接引用工具的输入参数名。

| 变量表达式 | 说明 |
| :--- | :--- |
| `{{productName}}` | 引用名为 `productName` 的工具参数值 |
| `{{city}}` | 引用名为 `city` 的工具参数值 |

---

### 优先级说明

如果你省略了命名空间（例如直接写 `{{key}}`），系统将按以下顺序查找：

1.  **工具参数** (仅在工具执行时)
2.  **系统变量** (sys)
3.  **自定义变量** (var)
4.  **实体** (entity)
5.  **客户信息** (customer)
6.  **节点输出** (node)

**建议**：为了避免歧义，建议尽可能使用带命名空间的完整写法（如 `{{var.key}}`）。
