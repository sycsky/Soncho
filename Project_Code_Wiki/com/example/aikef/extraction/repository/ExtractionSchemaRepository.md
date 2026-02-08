# ExtractionSchemaRepository

## 1. Class Profile
- **Class Name**: `ExtractionSchemaRepository`
- **Package**: `com.example.aikef.extraction.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `ExtractionSchema` entities.

## 2. Method Deep Dive
### Query Methods
- `findByEnabledTrue()`: Retrieves all currently active schemas.
- `findByName(String name)`: Looks up a schema by its unique name.
- `existsByName(String name)`: Checks for name uniqueness during creation.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.extraction.model.ExtractionSchema`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `ExtractionService` to load the definition of what needs to be extracted.
- **Workflow**: When a workflow node specifies "Use Schema: OrderForm", this repository is queried to get the details of "OrderForm".

## 5. Source Link
[ExtractionSchemaRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/extraction/repository/ExtractionSchemaRepository.java)
