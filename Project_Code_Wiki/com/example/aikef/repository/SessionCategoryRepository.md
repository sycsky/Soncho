# SessionCategoryRepository

## 1. Class Profile
- **Class Name**: `SessionCategoryRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `SessionCategory` entities.

## 2. Method Deep Dive
### Query Methods
- `findByName(String name)`: Exact match lookup.
- `findByEnabledTrueOrderBySortOrderAsc()`: Get active categories for UI dropdowns.
- `findAllByOrderBySortOrderAsc()`: Admin view.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.SessionCategory`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `SessionCategoryService` and `AiWorkflowService`.
- **Validation**: Ensuring a category exists before assigning it to a session.

## 5. Source Link
[SessionCategoryRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/SessionCategoryRepository.java)
