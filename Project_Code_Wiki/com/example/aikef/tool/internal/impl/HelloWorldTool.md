# HelloWorldTool

## Class Profile
`HelloWorldTool` is a simple implementation of an internal tool used for demonstration and testing purposes. It provides a basic "Hello World" functionality to verify that the `InternalToolRegistry` is correctly discovering and executing tools.

## Method Deep Dive

### `helloWorld(String name)`
- **Description**: Returns a greeting message.
- **Annotation**: `@Tool("A simple Hello World tool that returns a greeting message")`
- **Parameters**:
    - `name`: The name of the person to greet (Annotated with `@P`).
- **Returns**: A formatted string "Hello, {name}! Welcome to the internal tool world."

## Dependency Graph
- None (POJO style component).

## Usage Guide
This tool is automatically registered by `InternalToolRegistry`. It can be used in workflows or tested via the tool execution API.

```java
// Example invocation via registry
Map<String, Object> params = Map.of("name", "Trae");
Object result = registry.execute("helloWorld", params, null);
// Result: "Hello, Trae! Welcome to the internal tool world."
```

## Source Link
[HelloWorldTool.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/tool/internal/impl/HelloWorldTool.java)
