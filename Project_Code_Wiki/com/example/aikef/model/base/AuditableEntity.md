# AuditableEntity

## 1. Class Profile
- **Class Name**: `AuditableEntity`
- **Package**: `com.example.aikef.model.base`
- **Type**: `Abstract Class` (MappedSuperclass)
- **Role**: Base Entity
- **Purpose**: Provides standard fields (`id`, `createdAt`, `updatedAt`, `tenantId`) for all JPA entities to ensure consistency and support multi-tenancy.

## 2. Method Deep Dive
### Fields
- `id`: UUID primary key.
- `createdAt` / `updatedAt`: Automatic timestamps.
- `tenantId`: The tenant identifier for SaaS isolation.

### Lifecycle Methods
- `onCreate()`: Sets timestamps and tries to auto-populate `tenantId` from context.
- `onUpdate()`: Refreshes `updatedAt`.

### Annotations
- `@MappedSuperclass`: Indicates this class is not a table itself but its fields are inherited.
- `@EntityListeners(TenantEntityListener.class)`: Hooks into the JPA lifecycle to enforce tenant rules.
- `@FilterDef` / `@Filter`: Hibernate filters for enforcing tenant isolation at the query level.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.saas.listener.TenantEntityListener`: The listener logic.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM standard.
  - `org.hibernate.annotations.*`: Hibernate extensions.

## 4. Usage Guide
Extended by almost every entity in the system (`Agent`, `ChatSession`, `AiWorkflow`, etc.).
- **Inheritance**: `public class Agent extends AuditableEntity { ... }`

## 5. Source Link
[AuditableEntity.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/base/AuditableEntity.java)
