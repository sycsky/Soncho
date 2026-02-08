# SessionAgentDto

## 1. Class Profile
- **Class Name**: `SessionAgentDto`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record`
- **Role**: Data Transfer Object
- **Purpose**: Represents an agent participating in a chat session, including their role (Primary vs Support).

## 2. Method Deep Dive
### Fields
- `id` / `name` / `avatarUrl`: Profile.
- `status`: Online status.
- `isPrimary`: Boolean flag.

### Methods
- `fromAgentDto(...)`: Helper factory to convert from the standard `AgentDto`.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `AgentDto`
  - `com.example.aikef.model.enums.AgentStatus`

## 4. Usage Guide
Used in `ChatSessionDto`.
- **UI**: The chat window displays the "Primary Agent" prominently and "Support Agents" in a list.

## 5. Source Link
[SessionAgentDto.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/SessionAgentDto.java)
