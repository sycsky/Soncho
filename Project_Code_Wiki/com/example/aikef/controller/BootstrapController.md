# BootstrapController

## Class Profile
- **Package**: `com.example.aikef.controller`
- **Type**: Class
- **Description**: Controller for initial application data loading (Bootstrap).
- **Key Features**:
  - Load initial configuration and user state.

## Method Deep Dive

### `load`
- **Description**: Returns bootstrap data for the authenticated agent.
- **Signature**: `public BootstrapResponse load(Authentication authentication)`
- **Logic**:
  1. Extracts Agent ID from authentication.
  2. Calls `bootstrapService.bootstrap(agentId)`.
  3. Returns `BootstrapResponse`.

## Dependency Graph
- **Injected Services**:
  - `BootstrapService`
- **DTOs**:
  - `BootstrapResponse`

## Usage Guide
```bash
GET /api/v1/bootstrap
```
