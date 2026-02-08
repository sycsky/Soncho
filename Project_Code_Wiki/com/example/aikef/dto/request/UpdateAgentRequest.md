# UpdateAgentRequest

## 1. Class Profile
- **Class Name**: `UpdateAgentRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: Request payload to update an agent's profile or status.

## 2. Method Deep Dive
### Fields
- `name`: Display name.
- `email`: Login email.
- `status`: Availability status (ONLINE/BUSY/OFFLINE).
- `roleId`: Assigned security role.
- `language`: Interface language preference.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.enums.AgentStatus`

## 4. Usage Guide
Used by `AgentController.update`.
- **Self-Service**: Agents can update their status or language.
- **Admin**: Admins can change an agent's role or email.

## 5. Source Link
[UpdateAgentRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/UpdateAgentRequest.java)
