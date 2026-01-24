# 会话结束（Resolve）API 文档

## 概述

本文档描述会话结束（Resolve）功能的 API 接口。在结束会话时，系统会自动使用 AI 生成对话总结，并将总结保存为 SYSTEM 类型的消息。

## 功能特点

1. **智能总结**：使用 AI 自动生成对话总结
2. **增量总结**：支持多次 Resolve，每次只总结上一次总结之后的对话
3. **总结预览**：支持在正式 Resolve 前预览总结内容
4. **历史追溯**：所有总结都保存为 SYSTEM 消息，方便后续查看
5. **自动分组**：Resolve 后自动将会话移动到所有参与客服的 Resolved 分组

## 总结范围规则

- **有 SYSTEM 消息**：总结从上一条 SYSTEM 消息之后到当前时间的所有对话
- **无 SYSTEM 消息**：总结会话的所有对话内容

## API 接口

### 1. 预览会话总结

在 Resolve 会话前，先预览 AI 生成的会话总结。

```http
GET /api/v1/chat/sessions/{sessionId}/summary/preview
Authorization: Bearer {agent_token}
```

**响应示例：**

```json
{
  "success": true,
  "summary": "客户咨询了产品退换货政策，客服详细解释了7天无理由退换的条件和流程。客户表示满意，问题已解决。",
  "errorMessage": null,
  "messageCount": 15
}
```

**响应字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| success | boolean | 是否成功生成总结 |
| summary | string | 总结内容（成功时返回） |
| errorMessage | string | 错误信息（失败时返回） |
| messageCount | int | 参与总结的消息数量 |

### 2. 结束会话（Resolve）

结束会话并生成 AI 总结，总结会保存为 SYSTEM 类型的消息。

```http
POST /api/v1/chat/sessions/{sessionId}/resolve?language={language_code}
Authorization: Bearer {agent_token}
```

**请求参数：**

| 参数名 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| language | Query | string | 否 | 目标语言代码（如 zh, en, ja）。默认生成中文总结，指定其他语言将自动翻译。 |

**处理流程：**

1. 获取需要总结的对话（从上一条 SYSTEM 消息之后到当前）
2. 调用 AI 生成对话总结
3. 将总结保存为 SYSTEM 类型消息
4. 将会话状态设置为 RESOLVED
5. 将会话移动到所有参与客服的 Resolved 分组（主责客服 + 支持客服）

**响应示例：**

```json
{
  "session": {
    "id": "xxx-xxx-xxx",
    "status": "RESOLVED",
    "userId": "xxx-xxx-xxx",
    "user": { ... },
    "lastActive": 1702012800000,
    "unreadCount": 0,
    ...
  },
  "summaryMessage": {
    "id": "xxx-xxx-xxx",
    "sessionId": "xxx-xxx-xxx",
    "senderType": "SYSTEM",
    "text": "【会话总结】\n客户咨询了产品退换货政策...",
    "createdAt": "2024-12-08T10:30:00Z",
    ...
  }
}
```

## 使用场景

### 场景1：首次结束会话

```
对话历史：
- [USER] 你好，我想咨询退货
- [AGENT] 您好，请问是什么商品？
- [USER] 是上周买的手机
- [AGENT] 好的，7天内可以无理由退货...
- [USER] 明白了，谢谢
- [AGENT] 不客气，再见

Resolve 后：
- [SYSTEM] 【会话总结】客户咨询手机退货事宜，客服解释了7天无理由退货政策，问题已解决。
```

### 场景2：会话重新打开后再次结束

```
已有历史（包含之前的总结）：
- [USER] 你好...
- [AGENT] ...
- [SYSTEM] 【会话总结】第一次对话总结...

新对话：
- [USER] 我还有个问题
- [AGENT] 请说
- [USER] ...
- [AGENT] ...

第二次 Resolve：
- [SYSTEM] 【会话总结】客户追问了xxx问题，已解答。

注意：第二次总结只包含第一次 SYSTEM 消息之后的对话
```

## 总结消息格式

SYSTEM 类型的总结消息格式：

```
【会话总结】
{AI 生成的总结内容}
```

## WebSocket 事件

当会话状态变更为 RESOLVED 时，会通过 WebSocket 广播 `sessionUpdated` 事件：

```json
{
  "event": "sessionUpdated",
  "payload": {
    "session": {
      "id": "会话ID",
      "status": "RESOLVED",
      "primaryAgentId": "主责客服ID"
    }
  }
}
```

同时，SYSTEM 消息也会通过 `newMessage` 事件广播给会话参与者。

## 注意事项

1. **权限要求**：只有客服（Agent）可以调用这些接口
2. **幂等性**：多次调用 Resolve 会生成多条 SYSTEM 消息
3. **空对话处理**：如果没有需要总结的消息，会返回相应提示
4. **AI 调用失败**：如果 AI 调用失败，Resolve 操作会失败并返回错误信息
5. **分组移动**：Resolve 后会自动将会话移动到所有参与客服（主责客服 + 支持客服）的 Resolved 分组

## 分组移动规则

当会话被 Resolve 时：

1. **主责客服**：如果会话有主责客服，会话会移动到该客服的 Resolved 分组
2. **支持客服**：如果会话有支持客服，会话也会移动到每个支持客服的 Resolved 分组
3. **多客服场景**：每个参与客服都会在自己的视角下看到该会话在 Resolved 分组中
4. **Resolved 分组**：是系统预设的分组，会在客服首次登录时自动创建

```
示例：
会话参与者：
- 主责客服 A
- 支持客服 B
- 支持客服 C

Resolve 后：
- 会话出现在客服 A 的 Resolved 分组
- 会话出现在客服 B 的 Resolved 分组
- 会话出现在客服 C 的 Resolved 分组
```

## 相关文件

- 服务类：`com.example.aikef.service.SessionSummaryService`
- 控制器：`com.example.aikef.controller.ChatController`
- 消息仓库：`com.example.aikef.repository.MessageRepository`

