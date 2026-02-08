# InternalToolRegistry

## Class Profile
The `InternalToolRegistry` is a core component responsible for discovering, registering, and executing internal tools within the system. It automatically scans Spring beans for methods annotated with `@Tool`, generates JSON schemas for their parameters, and registers them in the `AiToolRepository`. It also handles the execution of these tools, including parameter mapping and JSON body parsing.

## Method Deep Dive

### `init()`
- **Description**: Triggered on `ApplicationReadyEvent`. Scans the application context for beans with `@Tool` annotated methods.
- **Logic**:
    1.  Iterates through all bean definitions.
    2.  Reflectively checks methods for `@Tool`.
    3.  Generates `ToolSpecification` using LangChain4j.
    4.  Registers or updates the tool in `AiToolRepository`.
    5.  Generates and saves parameter schemas (filtering for `@P` annotated parameters).

### `execute(String toolName, Map<String, Object> params, String body, WorkflowContext ctx)`
- **Description**: Executes a registered internal tool.
- **Parameters**:
    - `toolName`: The unique name of the tool.
    - `params`: A map of parameter values.
    - `body`: Optional JSON body (used if the tool has a `bodyTemplate`).
    - `ctx`: The workflow context.
- **Logic**:
    1.  Retrieves the `ToolMethod` from the local cache.
    2.  If `body` is present and the method expects a single object or a map, it parses the body.
    3.  Invokes `invokeMethod` to execute the actual bean method.

### `convertToFieldDefinitions(JsonObjectSchema jsonObjectSchema, Method method)`
- **Description**: Converts LangChain4j's JSON schema to the system's `FieldDefinition` format.
- **Logic**:
    - Iterates through schema properties.
    - **Crucial Filter**: Only includes parameters that are explicitly annotated with `@P` in the method signature.
    - Maps JSON types (String, Integer, etc.) to `FieldType`.

## Dependency Graph
- `ApplicationContext`: For bean scanning.
- `AiToolRepository`: For persisting tool metadata.
- `ExtractionSchemaRepository`: For persisting parameter schemas.
- `ToolExecutionRepository`: (Injected but currently unused in the provided code snippet).
- `ObjectMapper`: For JSON serialization/deserialization.

## Usage Guide
This class works automatically at startup. To add a new internal tool, simply create a Spring component and annotate a method with `@Tool`.

```java
@Component
public class MyTools {
    @Tool("My custom tool")
    public String doSomething(@P("Input value") String input) {
        return "Done: " + input;
    }
}
```

## Source Link
[InternalToolRegistry.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/tool/internal/InternalToolRegistry.java)
