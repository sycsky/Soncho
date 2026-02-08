# UpdateCustomerRequest

## 1. Class Profile
- **Class Name**: `UpdateCustomerRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: Request payload for updating a customer's profile information. Supports partial updates (nullable fields are ignored).

## 2. Method Deep Dive
### Fields
- `name`: Display name.
- `primaryChannel`: Preferred contact channel.
- `email`, `phone`: Contact details.
- `wechatOpenId`...`facebookId`: Channel-specific identifiers.
- `avatarUrl`: Profile picture.
- `location`: Geographic location.
- `notes`: Internal notes about the customer.
- `customFields`: Extensible JSON attributes.
- `active`: Account status.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Channel`

## 4. Usage Guide
Used by `CustomerController.update`.
- **Profile Editing**: Agents can update customer details (e.g., add a phone number) from the sidebar.

## 5. Source Link
[UpdateCustomerRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/UpdateCustomerRequest.java)
