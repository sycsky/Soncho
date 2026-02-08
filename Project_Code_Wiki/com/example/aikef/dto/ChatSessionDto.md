# ChatSessionDto

## 1. Class Profile
- **Class Name**: `ChatSessionDto`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record`
- **Role**: Data Transfer Object
- **Purpose**: Represents a summary of a chat session for the "Inbox" list view. It aggregates customer info, the last message, and unread counts.

## 2. Method Deep Dive
### Fields
- `id`: Session ID.
- `userId` / `user`: The customer details.
- `status`: `QUEUED`, `AI_HANDLING`, etc.
- `lastActive`: Timestamp of the last update.
- `unreadCount`: Number of messages the agent hasn't read yet.
- `sessionGroupId`: Which custom folder this is in.
- `primaryAgentId` / `agents`: Assigned staff.
- `lastMessage`: Preview of the most recent text.
- `categoryId` / `category`: Topic classification.
- `metadata`: Channel-specific data.
- `customerLanguage`: Language preference.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `CustomerDto`: Nested user info.
  - `SessionAgentDto`: Nested agent info.
  - `SessionMessageDto`: Nested message preview.
  - `SessionCategoryDto`: Nested category info.

## 4. Usage Guide
Used by `ChatSessionController`.
- **List View**: `GET /api/v1/sessions` returns a list of these DTOs.

## 5. Source Link
[ChatSessionDto.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/ChatSessionDto.java)
