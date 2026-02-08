# OfficialChannelConfigRepository

## 1. Class Profile
- **Class Name**: `OfficialChannelConfigRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `OfficialChannelConfig` entities.

## 2. Method Deep Dive
### Query Methods
- `findByChannelType(...)`: Lookup by channel type enum.
- `findByChannelTypeAndEnabledTrue(...)`: Lookup only if active (used by adapters).
- `existsByChannelType(...)`: Existence check.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.OfficialChannelConfig`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `OfficialChannelService` and channel adapters.
- **Adapter Initialization**: `WechatOfficialAdapter` calls `findByChannelType(WECHAT_OFFICIAL)` to initialize its API client.

## 5. Source Link
[OfficialChannelConfigRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/OfficialChannelConfigRepository.java)
