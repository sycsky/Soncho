# ExternalSessionMapping

## 1. Class Profile
- **Class Name**: `ExternalSessionMapping`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Mapping Entity
- **Purpose**: Maps a conversation thread from an external platform (e.g., a WhatsApp chat ID) to an internal `ChatSession` ID. This is critical for maintaining state across messages.

## 2. Method Deep Dive
### Fields
- `platform`: The external platform config.
- `externalThreadId`: The thread ID from the provider (e.g., WhatsApp phone number).
- `session`: The internal `ChatSession`.
- `customer`: The linked customer.
- `externalUserId` / `externalUserName`: Metadata about the external user.
- `metadata`: JSON blob for platform-specific state.
- `active`: Whether this mapping is currently valid.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.ExternalPlatform`
  - `com.example.aikef.model.ChatSession`
  - `com.example.aikef.model.Customer`
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
Used by `ExternalPlatformService`.
- **Inbound**: When a webhook arrives with `thread_id=123`, the system queries this table. If a mapping exists, it routes the message to the existing `session`. If not, it creates a new session and a new mapping.

## 5. Source Link
[ExternalSessionMapping.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/ExternalSessionMapping.java)
