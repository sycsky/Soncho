# HistoryMessageLoader

## 1. Class Profile
- **Class Name**: `HistoryMessageLoader`
- **Package**: `com.example.aikef.workflow.util`
- **Type**: `Component`
- **Role**: Data Loader / Converter
- **Purpose**: Loads historical chat messages from the database and converts them into a format suitable for LLM context (LangChain4j `ChatMessage`). It handles pagination, filtering, and special message types like Tool calls.

## 2. Method Deep Dive
### `loadHistoryMessages(UUID sessionId, int readCount, UUID messageId)`
- **Functionality**: Loads raw `Message` entities from the database.
- **Logic**:
  - Starts loading backwards from `messageId` (or latest).
  - Stops when it hits a `SYSTEM` message (context window boundary).
  - Skips empty messages.
  - Handles pagination to efficiently retrieve the requested `readCount`.
  - Returns messages in chronological order (oldest to newest).

### `loadChatMessages(UUID sessionId, int readCount, UUID messageId)`
- **Functionality**: Converts the loaded entities into LangChain4j `ChatMessage` objects (`UserMessage`, `AiMessage`, `ToolExecutionResultMessage`).
- **Complex Handling**:
  - Reconstructs `ToolExecutionRequest` objects from the saved JSON metadata in `TOOL` messages.
  - Ensures that the conversation history passed to the LLM is structurally valid (e.g., matching tool requests with results).

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.repository.MessageRepository`: Access to message data.
  - `com.example.aikef.model.Message`: The message entity.
- **External Dependencies**:
  - `dev.langchain4j.data.message.*`: LangChain4j message types.

## 4. Usage Guide
Used by `WorkflowContext` or `LlmNode` to prepare the context window for the LLM.
```java
List<ChatMessage> history = historyMessageLoader.loadChatMessages(sessionId, 10, currentMsgId);
prompt.addAll(history);
```

## 5. Source Link
[HistoryMessageLoader.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/workflow/util/HistoryMessageLoader.java)
