# CustomerPrincipal

## Class Profile
`CustomerPrincipal` represents an authenticated end-customer (not an agent). It implements `Principal` and holds customer-specific details like ID, name, channel, and tenant ID.

## Method Deep Dive

### Fields
- `id`: Customer UUID.
- `name`: Customer name.
- `channel`: The channel they are communicating from (e.g., "email", "whatsapp").
- `tenantId`: The tenant they belong to.

## Dependency Graph
- None.

## Usage Guide
Stored in `SecurityContext` when a customer is authenticated via a customer token (e.g., via the widget or public API).

## Source Link
[CustomerPrincipal.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/security/CustomerPrincipal.java)
