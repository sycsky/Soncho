# TenantAwareSimpleJpaRepository

## Class Profile
`TenantAwareSimpleJpaRepository` is a custom implementation of Spring Data JPA's `SimpleJpaRepository`. It overrides standard methods like `findById` and `existsById` to ensure they respect the Hibernate Filter enabled by `TenantHibernateFilterAspect`.

## Method Deep Dive

### Why Override?
Standard `SimpleJpaRepository.findById` might use `em.find()`, which sometimes bypasses filters depending on caching or implementation details, or it might not trigger the aspect correctly if called internally. By overriding it to use a JPQL query (`select e from Entity e where id = :id`), we ensure the query goes through the Hibernate engine where the `@Filter` is applied.

### `findById(ID id)`
- **Logic**: Constructs a JPQL query to find the entity by ID. This forces Hibernate to append the `AND tenant_id = ?` clause.

### `existsById(ID id)`
- **Logic**: Similar to `findById`, uses `count` query.

## Dependency Graph
- `EntityManager`: For executing queries.

## Usage Guide
Configured as the base repository class in `SaasConfig`. No manual usage required; all repositories will inherit this behavior.

## Source Link
[TenantAwareSimpleJpaRepository.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/saas/repository/TenantAwareSimpleJpaRepository.java)
