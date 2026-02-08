# Class Profile: HealthController

**File Path**: `com/example/aikef/controller/HealthController.java`
**Type**: Controller (`@RestController`)
**Purpose**: Provides a simple health check endpoint to verify that the application is running and reachable. Used by load balancers, container orchestrators (k8s), or monitoring tools.

# Method Deep Dive

## `health()`
- **Endpoint**: `GET /api/health`
- **Description**: Returns a standard HTTP 200 OK response with a status payload.
- **Output**: `{"status": "UP1"}`

# Dependency Graph

**Core Dependencies**:
- None (Standard Spring Web).

**Key Imports**:
```java
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
```

# Usage Guide

## Check Status
`GET /api/health`

# Source Link
[HealthController.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/controller/HealthController.java)
