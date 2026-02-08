# SessionSummaryService

## 1. Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `Slf4j`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 会话总结服务。利用 LLM 对历史聊天记录进行分析，生成结构化的会话摘要（客户诉求、服务过程、处理结果），主要用于会话结束（Resolve）时的归档记录。

## 2. Method Deep Dive

### `generateSummary`
- **Signature**: `public SummaryResult generateSummary(UUID sessionId)`
- **Description**: 生成总结预览。
- **Logic**:
  1. 调用 `getMessagesToSummarize` 获取待总结的消息列表。
  2. 将消息列表格式化为文本（包含时间、角色、内容）。
  3. 调用 `LangChainChatService`，使用预设的 `SUMMARY_SYSTEM_PROMPT` 进行总结生成。
  4. 返回总结内容。

### `generateAndSaveSummary`
- **Signature**: `public Message generateAndSaveSummary(UUID sessionId)`
- **Description**: 生成并保存总结。
- **Logic**:
  1. 生成总结内容。
  2. 调用 `messageGateway.sendSystemMessage` 将总结作为一条 SYSTEM 类型的消息插入到会话末尾。

### `getMessagesToSummarize`
- **Signature**: `public List<Message> getMessagesToSummarize(UUID sessionId)`
- **Description**: 智能确定总结范围。
- **Logic**: 查找最后一条 SYSTEM 消息。如果存在，只总结从那之后的新消息（增量总结）；否则总结所有消息（全量总结）。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `LangChainChatService`: LLM 调用。
  - `MessageRepository`: 消息查询。
  - `SessionMessageGateway`: 发送总结消息。

## 4. Usage Guide
### 场景：会话结单
客服点击 "Resolve" 按钮。前端先调用预览接口，展示 AI 生成的总结草稿。客服确认无误或微调后，系统将该总结保存到会话流中，并关闭会话。这为后续的工单回溯提供了高价值的概览信息。
