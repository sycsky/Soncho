# ExternalPlatform

## 1. Class Profile
- **Class Name**: `ExternalPlatform`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Configuration Entity
- **Purpose**: Configures generic third-party integrations (Webhooks) that are not native "Official Channels". This allows custom integrations with proprietary CRM systems or legacy chat apps.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `name`: Technical identifier (e.g., "legacy_crm_bot").
- `displayName`: Human-readable name.
- `platformType`: The generic channel type this maps to.
- `callbackUrl`: Where to POST messages from our system.
- `authType` / `authCredential`: How to authenticate with the external system.
- `extraHeaders`: Custom headers required by the external system.
- `webhookSecret`: Secret for verifying incoming requests from the external system.
- `enabled`: Toggle.

## 3. Dependency Graph
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
Used by `ExternalPlatformService`.
- **Outbound**: When a message needs to be sent to this platform, the system builds a POST request to `callbackUrl` using `authCredential`.
- **Inbound**: When the external system POSTs to our webhook, we verify the signature using `webhookSecret`.

## 5. Source Link
[ExternalPlatform.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/ExternalPlatform.java)
