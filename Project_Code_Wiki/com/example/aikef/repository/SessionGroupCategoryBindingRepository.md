# SessionGroupCategoryBindingRepository

## 1. Class Profile
- **Class Name**: `SessionGroupCategoryBindingRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `SessionGroupCategoryBinding` entities.

## 2. Method Deep Dive
### Query Methods
- `findBySessionGroup_Id(...)`: Get contents of a group.
- `findGroupIdByAgentIdAndCategoryId(...)`: Find where a category is currently filed.
- `deleteBySessionGroup_Id(...)`: Cleanup when deleting a group.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.SessionGroupCategoryBinding`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `SessionGroupService`.
- **Display**: When rendering the sidebar, `findGroupIdByAgentIdAndCategoryId` determines under which folder to display a category.

## 5. Source Link
[SessionGroupCategoryBindingRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/SessionGroupCategoryBindingRepository.java)
