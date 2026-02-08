# TemplateEngine

## 1. Class Profile
- **Class Name**: `TemplateEngine`
- **Package**: `com.example.aikef.workflow.util`
- **Type**: `Utility Class`
- **Role**: String Processor
- **Purpose**: A lightweight template engine for resolving variable expressions (e.g., `{{customer.name}}`) within workflow strings. It supports nested namespaces and fallback logic.

## 2. Method Deep Dive
### `render(String template, WorkflowContext ctx, Map<String, Object> localVariables)`
- **Functionality**: Replaces all occurrences of `{{...}}` with their resolved values.
- **Parameters**:
  - `template`: The string containing variables.
  - `ctx`: The workflow context containing global data.
  - `localVariables`: Optional local variables that override global ones.

### Variable Resolution Logic
- **Namespaces**:
  - `sys` / `system`: System variables (e.g., `sys.query`, `sys.now`).
  - `var` / `variable`: Custom workflow variables defined by `VariableNode`.
  - `node`: Output from specific nodes (e.g., `node.llm_1`).
  - `customer`: Customer profile fields (e.g., `customer.name`).
  - `entity`: Extracted entities (e.g., `entity.orderId`).
  - `agent`: Agent session data.
  - `event`: Webhook event data.
  - `meta`: Session metadata.
- **Fallback**: If no namespace is provided, it tries to find the key in the context's variable map.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.workflow.context.WorkflowContext`: Source of truth for variable data.

## 4. Usage Guide
Used extensively by almost every workflow node type to inject dynamic data into prompts, messages, or API payloads.
```java
String prompt = "Hello {{customer.name}}, your order {{var.orderId}} is status: {{node.api_check_status}}";
String rendered = TemplateEngine.render(prompt, context);
```

## 5. Source Link
[TemplateEngine.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/workflow/util/TemplateEngine.java)
