# SessionStatus

## 1. Class Profile
- **Class Name**: `SessionStatus`
- **Package**: `com.example.aikef.model.enums`
- **Type**: `Enum`
- **Role**: Enumeration
- **Purpose**: Defines the lifecycle state of a chat session.

## 2. Method Deep Dive
### Values
- `AI_HANDLING`: The bot is in charge.
- `HUMAN_HANDLING`: A human agent has taken over.
- `RESOLVED`: The conversation is finished.

## 3. Usage Guide
Used in `ChatSession` entity.
- **Workflow**: New chats start as `AI_HANDLING`. If the user asks for a human, it flips to `HUMAN_HANDLING`.

## 4. Source Link
[SessionStatus.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/enums/SessionStatus.java)
