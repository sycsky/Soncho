# CreateAgentRequest

## 1. Class Profile
- **Class Name**: `CreateAgentRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Class` (POJO)
- **Role**: Data Transfer Object (Request)
- **Purpose**: Carries data to create a new agent.

## 2. Method Deep Dive
### Fields
- `name`: Agent's display name (Required).
- `email`: Login email (Required, Valid Email).
- `password`: Plain text password (Required, will be hashed).
- `roleId`: UUID of the assigned role (Required).
- `language`: Preferred UI language.
- `tenantId`: (Optional) For multi-tenant setup.

## 3. Dependency Graph
- **External Dependencies**:
  - `jakarta.validation.constraints.*`: Validation annotations.

## 4. Usage Guide
Used by `AgentController`.
- **Endpoint**: `POST /api/v1/agents`
- **Validation**: `@Valid` triggers checks on email format and non-blank fields.

## 5. Source Link
[CreateAgentRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/CreateAgentRequest.java)
