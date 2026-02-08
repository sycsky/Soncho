# LoginResponse

## 1. Class Profile
- **Class Name**: `LoginResponse`
- **Package**: `com.example.aikef.dto.response`
- **Type**: `Record`
- **Role**: Data Transfer Object (Response)
- **Purpose**: Returns the authentication token and user details after a successful login.

## 2. Method Deep Dive
### Fields
- `token`: JWT Bearer token.
- `agent`: Full profile of the logged-in agent.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.dto.AgentDto`

## 4. Usage Guide
Used by `AuthController`.
- **Response**: `POST /api/v1/auth/login`.

## 5. Source Link
[LoginResponse.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/response/LoginResponse.java)
