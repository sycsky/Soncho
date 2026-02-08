# SpecialCustomerRepository

## 1. Class Profile
- **Class Name**: `SpecialCustomerRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `SpecialCustomer` entities.

## 2. Method Deep Dive
### Query Methods
- `findByRole_Code(String roleCode)`: Finds all special customers with a specific role code (e.g., "VIP").
- `findByRole_IdIn(List<UUID> roleIds)`: Finds special customers belonging to any of the specified role IDs.
- `findByCustomer_Id(UUID customerId)`: Checks if a specific customer is a special customer and retrieves their details.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.SpecialCustomer`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `CustomerService` or `ChatSessionService` to determine customer priority.
- **Priority Routing**: When a chat starts, check `findByCustomer_Id` to see if the user is VIP.
- **Targeted Messaging**: Use `findByRole_Code` to send messages to all VIP customers.

## 5. Source Link
[SpecialCustomerRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/SpecialCustomerRepository.java)
