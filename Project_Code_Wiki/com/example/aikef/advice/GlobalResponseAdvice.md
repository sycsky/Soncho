# GlobalResponseAdvice

## Class Profile
`GlobalResponseAdvice` is a `@RestControllerAdvice` that standardizes API responses. It intercepts successful responses to wrap them in a `Result<T>` envelope and catches exceptions to return uniform error JSONs.

## Method Deep Dive

### `beforeBodyWrite(...)`
- **Logic**:
    - Skips wrapping if body is already `Result`, `Resource` (file), or `void`.
    - Handles `String` specially by serializing it to JSON manually to avoid `ClassCastException`.
    - Wraps everything else in `Result.success(body)`.

### Exception Handlers
- `handleValidationExceptions`: 400 Bad Request.
- `handleEntityNotFound`: 404 Not Found.
- `handleAuthenticationException`: 401 Unauthorized.
- `handleAccessDeniedException`: 403 Forbidden.
- `handleAllUncaughtException`: 500 Internal Server Error (masks details for security).

## Dependency Graph
- `ObjectMapper`: For manual JSON serialization.

## Usage Guide
Applied automatically to all controllers under `com.example.aikef.controller`.

## Source Link
[GlobalResponseAdvice.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/advice/GlobalResponseAdvice.java)
