# CustomerRole

## 1. Class Profile
- **Class Name**: `CustomerRole`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Classification Entity
- **Purpose**: Defines a business role for a customer (e.g., "Supplier", "Distributor", "VIP"). This allows for targeted messaging and workflow logic.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `code`: Unique technical code (e.g., `SUPPLIER`).
- `name`: Display name (e.g., "Supplier").
- `description`: Usage notes.

## 3. Dependency Graph
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
Used in `Customer` entity (via `roleCode` or a relationship) and `AiScheduledTask` (for `CUSTOMER_ROLE` targeting).
- **Targeting**: "Send a message to all customers with role `SUPPLIER`."

## 5. Source Link
[CustomerRole.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/CustomerRole.java)
