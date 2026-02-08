# CreateSessionCategoryRequest

## 1. Class Profile
- **Class Name**: `CreateSessionCategoryRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: Request payload to create a new category for organizing chat sessions.

## 2. Method Deep Dive
### Fields
- `name`: Category name (e.g., "Technical Support").
- `description`: Description.
- `icon`: UI icon identifier.
- `color`: UI color hex code.
- `sortOrder`: Display order.

## 3. Dependency Graph
- **Internal Dependencies**: None.

## 4. Usage Guide
Used by `SessionCategoryController`.
- **Organization**: Categories are used to route and filter sessions.

## 5. Source Link
[CreateSessionCategoryRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/CreateSessionCategoryRequest.java)
