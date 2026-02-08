# LoginRequest

## 1. Class Profile
- **Class Name**: `LoginRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record`
- **Role**: Data Transfer Object (Request)
- **Purpose**: Payload for agent authentication.

## 2. Method Deep Dive
### Fields
- `email`: User email.
- `password`: User password.

## 3. Dependency Graph
- **External Dependencies**:
  - `jakarta.validation.constraints.*`

## 4. Usage Guide
Used by `AuthController`.
- **Endpoint**: `POST /api/v1/auth/login`

## 5. Source Link
[LoginRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/LoginRequest.java)
