# Result

## 1. Class Profile
- **Class Name**: `Result<T>`
- **Package**: `com.example.aikef.dto`
- **Type**: `Class` (Generic)
- **Role**: Data Transfer Object / Envelope
- **Purpose**: A standardized wrapper for all API responses, ensuring consistent structure (`code`, `message`, `data`) across the application.

## 2. Method Deep Dive
### Fields
- `code`: HTTP-like status code (200 = Success).
- `message`: Error description or "Success".
- `data`: The actual payload (generic type `T`).
- `success`: Boolean convenience flag.

### Static Factory Methods
- `success(T data)`: Creates a successful response with payload.
- `success()`: Creates a successful response with no payload.
- `error(int code, String message)`: Creates an error response.

## 3. Dependency Graph
- **Internal Dependencies**: None.

## 4. Usage Guide
Used by **all** Controllers.
- **Example**:
  ```java
  @GetMapping
  public Result<AgentDto> getAgent() {
      return Result.success(agentService.getCurrentAgent());
  }
  ```
- **JSON Output**:
  ```json
  {
    "code": 200,
    "message": "Success",
    "data": { ... },
    "success": true
  }
  ```

## 5. Source Link
[Result.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/Result.java)
