# CreateCustomerRequest

## 1. Class Profile
- **Class Name**: `CreateCustomerRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record`
- **Role**: Data Transfer Object (Request)
- **Purpose**: Payload for manually creating a customer profile.

## 2. Method Deep Dive
### Fields
- `name`: Required.
- `primaryChannel`: `EMAIL`, `WHATSAPP`, etc. (Required).
- `email`, `phone`, `wechatOpenId`, etc.: Channel identifiers.
- `customFields`: Arbitrary JSON data for CRM extension.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Channel`: Enum.

## 4. Usage Guide
Used by `CustomerController`.
- **Endpoint**: `POST /api/v1/customers`

## 5. Source Link
[CreateCustomerRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/CreateCustomerRequest.java)
