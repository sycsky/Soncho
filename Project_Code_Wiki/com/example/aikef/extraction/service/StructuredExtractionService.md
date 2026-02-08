# StructuredExtractionService

## Class Profile
`StructuredExtractionService` implements a multi-turn slot-filling engine. It is designed to extract specific fields (defined in an `ExtractionSchema`) from user conversations. It maintains session state (`ExtractionSession`), tracks missing fields, and generates follow-up questions until all required information is collected.

## Method Deep Dive

### `submitText(UUID sessionId, String userText)`
- **Description**: The main entry point for processing user input in an extraction session.
- **Logic**:
    1.  Validates session status.
    2.  Invokes `extractFromText` to analyze input using LLM.
    3.  Merges new data into `extractedData`.
    4.  Updates `missingFields`.
    5.  If fields are missing, generates a follow-up question via `generateFollowupQuestion`.
    6.  If complete, marks session as `COMPLETED`.

### `extractFromText(...)`
- **Description**: Uses `LangChainChatService` to extract data.
- **Logic**:
    - Converts `FieldDefinition`s into a JSON Schema.
    - Calls `chatWithStructuredOutput` (or `chatWithFieldDefinitions`) to get a strict JSON response from the LLM.
    - Handles fallback to standard chat if structured output fails.

### `createSchema(...)` / `createSession(...)`
- **Description**: CRUD for schemas and sessions.
- **Logic**: `createSession` initializes the tracking of missing fields based on the schema's required fields.

## Dependency Graph
- `ExtractionSchemaRepository`, `ExtractionSessionRepository`: DB access.
- `LangChainChatService`: For LLM inference.
- `LlmModelService`: To resolve model IDs.
- `ObjectMapper`: For JSON handling of `extractedData` and `fieldsJson`.

## Usage Guide
Useful for scenarios like collecting shipping information, booking appointments, or filling out forms via chat.

```java
// 1. Create a schema (e.g., "Shipping Info")
var schema = service.createSchema(new CreateSchemaRequest("Shipping", ..., fields, ...), userId);

// 2. Start a session
var session = service.createSession(schema.getId(), userId, null, null);

// 3. Submit user input
var result = service.submitText(session.getId(), "I live in New York");
// Result: extracted={"city": "New York"}, missing=["street", "zip"], nextQuestion="What is your street address?"
```

## Source Link
[StructuredExtractionService.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/extraction/service/StructuredExtractionService.java)
