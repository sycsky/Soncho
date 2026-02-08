# CustomerRoleController

## Class Profile
- **Package**: `com.example.aikef.controller`
- **Type**: Class
- **Description**: Controller for managing special customer roles/types (e.g., VIP, Blacklist).
- **Key Features**:
  - List customer roles.
  - Create customer roles.

## Method Deep Dive

### `listRoles`
- **Description**: Lists all defined customer roles.
- **Signature**: `public List<CustomerRole> listRoles()`
- **Logic**: Delegates to `specialCustomerService.getAllRoles()`.

### `createRole`
- **Description**: Creates a new customer role definition.
- **Signature**: `public CustomerRole createRole(CreateRoleRequest request)`
- **Logic**: Calls `specialCustomerService.createRole`.

## Dependency Graph
- **Injected Services**:
  - `SpecialCustomerService`
- **DTOs**:
  - `CustomerRole`, `CreateRoleRequest`

## Usage Guide
```bash
GET /api/v1/customer-roles
```
