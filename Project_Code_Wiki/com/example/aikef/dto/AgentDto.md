# AgentDto

## 1. Class Profile
- **Class Name**: `AgentDto`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record`
- **Role**: Data Transfer Object
- **Purpose**: Transfers `Agent` data to the frontend, stripping sensitive information like password hashes.

## 2. Method Deep Dive
### Fields
- `id`: Agent's UUID.
- `name` / `email` / `avatarUrl`: Profile details.
- `status`: Current availability (`ONLINE`, `BUSY`, `OFFLINE`).
- `roleId` / `roleName`: Security role information.
- `language`: The agent's preferred interface language.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.enums.AgentStatus`: Status enum.

## 4. Usage Guide
Used by `AgentController`.
- **Response**: `GET /api/v1/agents/me` returns this DTO.

## 5. Source Link
[AgentDto.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/AgentDto.java)
