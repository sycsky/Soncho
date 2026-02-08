# SenderType

## 1. Class Profile
- **Class Name**: `SenderType`
- **Package**: `com.example.aikef.model.enums`
- **Type**: `Enum`
- **Role**: Enumeration
- **Purpose**: Identifies the originator of a message.

## 2. Method Deep Dive
### Values
- `USER`: The customer.
- `AGENT`: A human support staff.
- `AI`: The automated bot.
- `SYSTEM`: System notifications (e.g., "Chat started").
- `TOOL`: Output from an AI tool execution.

## 3. Usage Guide
Used in `Message` entity.
- **Filtering**: `findBySenderTypeNot(SYSTEM)` to hide system messages.

## 4. Source Link
[SenderType.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/enums/SenderType.java)
