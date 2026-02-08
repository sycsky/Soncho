# Class Profile: PublicController

**File Path**: `com/example/aikef/controller/PublicController.java`
**Type**: Controller (`@RestController`)
**Purpose**: Exposes public (unauthenticated or semi-authenticated) endpoints for customers and external clients. It handles the initial handshake, such as generating customer tokens, validating tokens, and creating tenant admins for testing purposes.

# Method Deep Dive

## Customer Access
- **`getCustomerToken(QuickCustomerRequest request)`**: The primary entry point for the chat widget. It finds or creates a customer based on channel details (e.g., email, phone) and returns a JWT token for subsequent authenticated requests.
- **`validateToken(...)`**: Verifies if a given token (Customer or Agent) is valid and active. Used by the frontend to check session validity on load.

## Testing
- **`createTenantAdminTest()`**: A test-only endpoint (enabled via config) to provision a new tenant environment with an admin user.
- **`test3(...)`**: A generic test endpoint for debugging payload reception.

# Dependency Graph

**Core Dependencies**:
- `CustomerService`: Customer lifecycle management.
- `CustomerTokenService`, `TokenService`: JWT generation and validation.
- `AgentService`: For tenant admin creation.

**Key Imports**:
```java
import com.example.aikef.service.CustomerService;
import com.example.aikef.security.TokenService;
import org.springframework.web.bind.annotation.*;
```

# Usage Guide

## Initializing Chat Widget
`POST /api/v1/public/customer-token`
```json
{
  "channel": "WEB",
  "name": "John Doe",
  "email": "john@example.com",
  "metadata": {
    "source": "landing_page",
    "referrer": "google"
  }
}
```
**Response**:
```json
{
  "token": "eyJhbGciOi...",
  "customerId": "uuid...",
  "sessionId": "uuid..."
}
```

# Source Link
[PublicController.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/controller/PublicController.java)
