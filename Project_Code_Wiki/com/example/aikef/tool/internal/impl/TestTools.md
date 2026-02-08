# TestTools

## Class Profile
`TestTools` provides mock implementations of common utility tools (Weather Check, Email Sending) for testing workflow execution logic without relying on external APIs. These tools log their actions and return simulated responses.

## Method Deep Dive

### `checkWeather(String location, String unit)`
- **Description**: Simulates checking the weather for a location.
- **Parameters**:
    - `location`: City name.
    - `unit`: 'celsius' or 'fahrenheit'.
- **Logic**: Returns "Sunny" or "Cloudy" based on simple deterministic logic (string length) to provide dynamic-feeling results during tests.

### `sendEmail(String to, String subject, String body)`
- **Description**: Simulates sending an email.
- **Parameters**:
    - `to`: Recipient address.
    - `subject`: Email subject.
    - `body`: Email content.
- **Logic**: Logs the email details and returns a success message.

## Dependency Graph
- None.

## Usage Guide
Useful for testing `ToolNode` execution in workflows where actual side effects (like sending real emails) are not desired.

```java
// Example weather check
String weather = testTools.checkWeather("Beijing", "celsius");
// Output: "Current weather in Beijing: Sunny, 25°C"
```

## Source Link
[TestTools.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/tool/internal/impl/TestTools.java)
