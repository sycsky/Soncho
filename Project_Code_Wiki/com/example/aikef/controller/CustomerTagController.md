# CustomerTagController

## Class Profile
- **Package**: `com.example.aikef.controller`
- **Type**: Class
- **Description**: Controller for managing tags associated with a specific customer. Distinguishes between manual tags (Agent-assigned) and AI tags (Auto-assigned).
- **Key Features**:
  - Get Manual/AI Tags.
  - Add/Remove Tags (Manual/AI).
  - Batch Set Tags.

## Method Deep Dive

### `getAllTags`
- **Description**: Retrieves all tags for a customer.
- **Signature**: `public CustomerDto getAllTags(UUID customerId)`
- **Logic**: Calls `customerTagService.getAllTags`.

### `addManualTag`
- **Description**: Adds a manually assigned tag.
- **Signature**: `public CustomerDto addManualTag(UUID customerId, AddCustomerTagRequest request)`
- **Logic**: Calls `customerTagService.addManualTag`.

### `addAiTag`
- **Description**: Adds an AI-assigned tag.
- **Signature**: `public CustomerDto addAiTag(UUID customerId, AddCustomerTagRequest request)`
- **Logic**: Calls `customerTagService.addAiTag`.

## Dependency Graph
- **Injected Services**:
  - `CustomerTagService`
- **DTOs**:
  - `CustomerDto`, `AddCustomerTagRequest`, `RemoveCustomerTagRequest`

## Usage Guide
```bash
POST /api/v1/customers/{id}/tags/manual
{ "tag": "VIP" }
```
