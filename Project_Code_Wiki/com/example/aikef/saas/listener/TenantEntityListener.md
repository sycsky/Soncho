# TenantEntityListener

## Class Profile
`TenantEntityListener` is a JPA Entity Listener that automatically populates the `tenantId` field on entities before they are persisted to the database.

## Method Deep Dive

### `prePersist(Object entity)`
- **Annotation**: `@PrePersist`
- **Logic**:
    1.  Checks if the entity implements `AuditableEntity` (or has a tenant ID field).
    2.  If `tenantId` is null, retrieves it from `TenantContext`.
    3.  Sets the `tenantId` on the entity.
    4.  Allows manual override if the code explicitly sets a tenant ID before saving.

## Dependency Graph
- `TenantContext`: Source of the tenant ID.
- `AuditableEntity`: The base class/interface expected for tenant-aware entities.

## Usage Guide
This listener is typically registered on the base entity class or specific entities via `@EntityListeners(TenantEntityListener.class)`.

## Source Link
[TenantEntityListener.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/saas/listener/TenantEntityListener.java)
