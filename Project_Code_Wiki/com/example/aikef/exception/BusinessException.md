# BusinessException

## 1. Class Profile
- **Class Name**: `BusinessException`
- **Package**: `com.example.aikef.exception`
- **Type**: `Class` (Exception)
- **Role**: Custom Runtime Exception
- **Purpose**: A generic exception for handling expected business logic failures (e.g., "Insufficient balance", "Invalid status").

## 2. Method Deep Dive
### Constructors
- `BusinessException(String message)`: Sets the message and defaults the status code to 400.
- `BusinessException(int code, String message)`: Sets a custom status code and message.

## 3. Dependency Graph
- **Internal Dependencies**: None.
- **External Dependencies**:
  - `java.lang.RuntimeException`: Base class.

## 4. Usage Guide
Used throughout the service layer.
- **Throwing**: `throw new BusinessException("Order already processed");`
- **Handling**: Caught by `GlobalResponseAdvice` to return a clean JSON error response to the client.

## 5. Source Link
[BusinessException.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/exception/BusinessException.java)
