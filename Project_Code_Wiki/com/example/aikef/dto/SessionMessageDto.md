# SessionMessageDto

## 1. Class Profile
- **Class Name**: `SessionMessageDto`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record`
- **Role**: Data Transfer Object
- **Purpose**: A lightweight message object used in session lists (for the "last message" preview) or initial bootstrap.

## 2. Method Deep Dive
### Fields
- `id`: Message ID.
- `text`: Content.
- `sender`: Sender name/ID.
- `timestamp`: Time.
- `internal`: Visibility.
- `attachments`: File list.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `AttachmentDto`

## 4. Usage Guide
Used in `ChatSessionDto` (field `lastMessage`).

## 5. Source Link
[SessionMessageDto.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/SessionMessageDto.java)
