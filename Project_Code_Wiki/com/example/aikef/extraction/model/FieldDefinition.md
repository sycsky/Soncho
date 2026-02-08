# FieldDefinition

## 1. Class Profile
- **Class Name**: `FieldDefinition`
- **Package**: `com.example.aikef.extraction.model`
- **Type**: `Class` (POJO)
- **Role**: Data Structure / Schema Definition
- **Purpose**: Defines a single field within an `ExtractionSchema`. It describes the data type, validation rules, and interaction behavior for that field.

## 2. Method Deep Dive
### Fields
- `name`: Technical key name (e.g., `phone_number`).
- `displayName`: User-friendly name (e.g., "Contact Phone").
- `type`: Data type (`STRING`, `NUMBER`, `DATE`, `ENUM`, `ARRAY`, `OBJECT`, etc.).
- `required`: Whether this field is mandatory for completion.
- `description`: Instructions for the AI (e.g., "Extract the user's primary mobile number").
- `defaultValue`: Value to use if not found.
- `enumValues`: Allowed values list for `ENUM` type.
- `validationPattern`: Regex for validation (e.g., `^\d{10}$`).
- `validationMessage`: Error message if validation fails.
- `followupQuestion`: Specific phrasing for the AI to ask if this field is missing.
- `examples`: Few-shot examples for the AI.
- `properties` / `items`: For defining nested objects or arrays.

## 3. Dependency Graph
- **External Dependencies**:
  - `lombok.*`: Boilerplate reduction.

## 4. Usage Guide
This class is serialized to JSON and stored in the `fields_json` column of `ExtractionSchema`.
- **AI Context**: The `description` and `examples` are fed directly into the LLM system prompt to improve extraction accuracy.
- **Validation**: The `validationPattern` is used by the backend to verify the extracted data before accepting it.

## 5. Source Link
[FieldDefinition.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/extraction/model/FieldDefinition.java)
