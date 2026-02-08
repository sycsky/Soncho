# ExtractionSchema

## 1. Class Profile
- **Class Name**: `ExtractionSchema`
- **Package**: `com.example.aikef.extraction.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Configuration Entity
- **Purpose**: Defines the blueprint for extracting structured data from unstructured text. It specifies the fields, types, and validation rules that the AI should follow when processing user input.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `name`: Unique name for the schema (e.g., "order_info_extraction").
- `description`: Purpose of this schema.
- `fieldsJson`: JSON string containing a list of `FieldDefinition` objects.
- `extractionPrompt`: Custom system prompt to guide the LLM during extraction.
- `followupPrompt`: Template for generating follow-up questions when required fields are missing.
- `llmModelId`: Optional specific LLM model to use for this schema.
- `enabled`: Toggle for availability.
- `createdBy`: User ID of the creator.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.extraction.model.FieldDefinition`: The structure of the `fieldsJson` content.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
This entity is similar to a "Form Definition" in traditional systems, but for AI.
- **Configuration**: An admin defines a schema for "Ticket Creation" with fields: `issue_type` (enum), `severity` (enum), `description` (string).
- **Execution**: The `ExtractionService` uses this schema to instruct the LLM: "Extract these specific fields from the user's message."

## 5. Source Link
[ExtractionSchema.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/extraction/model/ExtractionSchema.java)
