# AgentStatus

## 1. Class Profile
- **Class Name**: `AgentStatus`
- **Package**: `com.example.aikef.model.enums`
- **Type**: `Enum`
- **Role**: Enumeration
- **Purpose**: Defines the availability states of an agent.

## 2. Method Deep Dive
### Values
- `ONLINE`: Available to take new chats.
- `BUSY`: Logged in but not accepting new chats.
- `OFFLINE`: Logged out or away.

## 3. Usage Guide
Used in `Agent` entity and `AgentAssignmentStrategy`.
- **Logic**: The auto-assignment logic only considers agents with `status == ONLINE`.

## 4. Source Link
[AgentStatus.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/enums/AgentStatus.java)
