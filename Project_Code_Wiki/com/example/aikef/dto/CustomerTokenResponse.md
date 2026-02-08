# CustomerTokenResponse

## 1. Class Profile
- **Class Name**: `CustomerTokenResponse`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: Returned after a successful customer authentication/login. Contains the JWT token and essential session info.

## 2. Method Deep Dive
### Fields
- `customerId`: The authenticated customer's ID.
- `token`: The JWT access token for subsequent API calls.
- `name`: Customer's display name.
- `channel`: The channel they logged in from.
- `sessionId`: The ID of their active chat session (if any).

## 3. Dependency Graph
- **Internal Dependencies**: None (Standard Java types).

## 4. Usage Guide
Returned by `AuthController.customerLogin` or similar endpoints.
- **Client Storage**: The frontend client stores the `token` (e.g., in localStorage) and includes it in the `Authorization` header of future requests.
- **Session Resume**: The `sessionId` allows the client to immediately reconnect to their existing conversation.

## 5. Source Link
[CustomerTokenResponse.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/CustomerTokenResponse.java)
