# Class Profile: SessionCategoryController

**File Path**: `com/example/aikef/controller/SessionCategoryController.java`
**Type**: Controller (`@RestController`)
**Purpose**: Manages session categories (also known as topics or tags, e.g., "Tech Support", "Sales", "Complaint"). These categories are used to classify chat sessions, route them to appropriate workflows, or filter them in the agent dashboard.

# Method Deep Dive

## Retrieval
- **`getEnabledCategories()`**: Public-facing list of active categories (e.g., for a "Select Topic" dropdown).
- **`getAllCategories()`**: Admin-facing list including disabled ones.
- **`getCategory(id)`**: Details of a specific category.

## Management (Admin/Manager)
- **`createCategory(...)`**: Defines a new category.
- **`updateCategory(...)`**: Modifies name, description, or status.
- **`deleteCategory(...)`**: Removes a category.

# Dependency Graph

**Core Dependencies**:
- `SessionCategoryService`: Business logic.
- `AgentRepository`: Validating agent permissions.
- `SessionCategoryDto`: Response DTO.

**Key Imports**:
```java
import com.example.aikef.service.SessionCategoryService;
import com.example.aikef.dto.SessionCategoryDto;
import org.springframework.web.bind.annotation.*;
```

# Usage Guide

## Creating a Category
`POST /api/v1/session-categories`
```json
{
  "name": "Refund Request",
  "description": "Issues related to money back",
  "icon": "money-bill",
  "color": "#FF0000"
}
```

# Source Link
[SessionCategoryController.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/controller/SessionCategoryController.java)
