# ExternalSessionMappingRepository

## 1. Class Profile
- **Class Name**: `ExternalSessionMappingRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `ExternalSessionMapping` entities.

## 2. Method Deep Dive
### Query Methods
- `findByPlatformNameAndThreadId(...)`: Main lookup for webhook handling.
- `findByPlatformIdAndThreadId(...)`: ID-based lookup.
- `findBySessionId(...)`: Reverse lookup (find the external thread for a given internal session).

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.ExternalSessionMapping`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `ExternalPlatformService`.
- **Outbound**: When sending a reply, `findBySessionId` is used to get the `externalThreadId` so the adapter knows where to send the message on the external platform.

## 5. Source Link
[ExternalSessionMappingRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/ExternalSessionMappingRepository.java)
