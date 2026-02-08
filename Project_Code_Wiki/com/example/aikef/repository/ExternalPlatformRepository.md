# ExternalPlatformRepository

## 1. Class Profile
- **Class Name**: `ExternalPlatformRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `ExternalPlatform` entities.

## 2. Method Deep Dive
### Query Methods
- `findByName(String name)`: Lookup by technical name.
- `findByNameAndEnabledTrue(String name)`: Active configuration lookup.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.ExternalPlatform`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `ExternalPlatformService`.
- **Validation**: Ensures unique naming.
- **Runtime**: Loads configuration for webhook dispatching.

## 5. Source Link
[ExternalPlatformRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/ExternalPlatformRepository.java)
