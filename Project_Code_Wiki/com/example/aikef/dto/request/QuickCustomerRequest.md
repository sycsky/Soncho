# QuickCustomerRequest

## 1. Class Profile
- **Class Name**: `QuickCustomerRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: Optimized payload for "Guest Login" or "Quick Start" scenarios where a customer needs to be created instantly to start a chat.

## 2. Method Deep Dive
### Fields
- `name`: Temporary or provided name.
- `channel`: The channel used (e.g., WEB).
- `email`, `phone`, `channelUserId`: Optional identifiers if available.
- `metadata`: Context data (e.g., `categoryId` for routing, `referrer` for analytics).

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Channel`

## 4. Usage Guide
Used by `AuthController.quickCustomer`.
- **Widget Integration**: When a user opens the chat widget and types a message, this endpoint creates a customer record and returns a token in one go, avoiding a complex registration flow.

## 5. Source Link
[QuickCustomerRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/QuickCustomerRequest.java)
