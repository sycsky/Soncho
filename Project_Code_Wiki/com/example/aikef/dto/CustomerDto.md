# CustomerDto

## 1. Class Profile
- **Class Name**: `CustomerDto`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record`
- **Role**: Data Transfer Object
- **Purpose**: Transfers customer profile information, including contact details, channel IDs, and CRM tags.

## 2. Method Deep Dive
### Fields
- `id` / `name`: Identity.
- `primaryChannel`: Main communication method.
- `email` / `phone`: Contact info.
- `wechatOpenId` / `whatsappId` / ...: Channel-specific IDs.
- `avatarUrl`: Profile picture.
- `notes`: Internal agent notes.
- `customFields`: Dynamic CRM properties (JSON).
- `tags` / `aiTags`: Segmentation labels.
- `roleCode` / `roleName`: Business role (e.g., "Supplier").

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Channel`: Channel enum.

## 4. Usage Guide
Used by `CustomerController`.
- **Detail View**: `GET /api/v1/customers/{id}`.

## 5. Source Link
[CustomerDto.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/CustomerDto.java)
