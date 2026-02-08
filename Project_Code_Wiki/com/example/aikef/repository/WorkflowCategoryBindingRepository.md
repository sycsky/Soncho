# WorkflowCategoryBindingRepository

## 1. Class Profile
- **Class Name**: `WorkflowCategoryBindingRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `WorkflowCategoryBinding` entities.

## 2. Method Deep Dive
### Query Methods
- `findByCategory_Id(UUID categoryId)`: Finds the binding for a specific category (One-to-One logic).
- `findByCategoryIdWithWorkflow(UUID categoryId)`: Finds binding and fetches the workflow eagerly, ensuring the workflow is enabled.
- `findByWorkflow_Id(UUID workflowId)`: Finds all categories bound to a specific workflow.
- `findByWorkflowIdWithCategory(UUID workflowId)`: Finds all bindings for a workflow, eagerly fetching categories, ordered by priority.
- `deleteByWorkflow_Id(UUID workflowId)`: Removes all bindings for a workflow.
- `existsByCategory_Id(UUID categoryId)`: Checks if a category is already bound.
- `findAllBoundCategoryIds()`: Retrieves IDs of all categories that have a workflow binding.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.WorkflowCategoryBinding`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `WorkflowService` and `SessionCategoryService`.
- **Triggering**: When a session category is set, `findByCategoryIdWithWorkflow` is called to see if a workflow needs to be started.
- **Configuration**: Used in the settings UI to show which categories are handled by which workflows.

## 5. Source Link
[WorkflowCategoryBindingRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/WorkflowCategoryBindingRepository.java)
