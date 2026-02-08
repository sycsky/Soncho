# TenantContext

## Class Profile
`TenantContext` is a utility class that holds the context of the current tenant for the executing thread. It uses `ThreadLocal` to ensure that tenant IDs are isolated per request.

## Method Deep Dive

### `getTenantId()`
- **Returns**: The current tenant ID String, or `null` if not set.

### `setTenantId(String tenantId)`
- **Description**: Sets the tenant ID for the current thread.

### `clear()`
- **Description**: Removes the tenant ID from the current thread to prevent memory leaks or context pollution in thread pools.

## Dependency Graph
- None (Pure Java utility).

## Usage Guide
Used by interceptors, aspects, and listeners to pass the tenant ID around without cluttering method signatures.

```java
// In a service or controller
String currentTenant = TenantContext.getTenantId();
```

## Source Link
[TenantContext.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/saas/context/TenantContext.java)
