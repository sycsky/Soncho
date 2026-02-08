# SpecialCustomer

## 1. Class Profile
- **Class Name**: `SpecialCustomer`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Persistence Entity
- **Purpose**: Marks specific customers as "special" by assigning them a `CustomerRole`. This is used to give VIP status or specific access rights to certain customers.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID, inherited from AuditableEntity).
- `customer`: The customer being marked as special (One-to-One, Unique).
- `role`: The role assigned to this special customer (Many-to-One).

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Customer`: The customer entity.
  - `com.example.aikef.model.CustomerRole`: The role entity.
  - `com.example.aikef.model.base.AuditableEntity`: Base class.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.
  - `lombok.Data`: Boilerplate code generation.

## 4. Usage Guide
Used to implement VIP logic or special handling for specific customers.
- **VIP Handling**: Can be used to prioritize chats from customers with a "VIP" role.
- **Access Control**: Can restrict access to certain features or workflows based on the customer's role.

## 5. Source Link
[SpecialCustomer.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/SpecialCustomer.java)
