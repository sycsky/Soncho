# ExtractionSession

## 1. Class Profile
- **Class Name**: `ExtractionSession`
- **Package**: `com.example.aikef.extraction.model`
- **Type**: `Class` (JPA Entity)
- **Role**: State Object / Persistence Entity
- **Purpose**: Tracks the progress of a multi-turn information extraction process. It stores what has been collected so far and what is still missing.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `schema`: Reference to the `ExtractionSchema` being filled.
- `status`: Current status (`IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `MAX_ROUNDS_REACHED`).
- `extractedData`: JSON string of the key-value pairs collected so far.
- `missingFields`: JSON array of field names that still need to be collected.
- `conversationHistory`: JSON array storing the relevant dialogue context.
- `currentRound` / `maxRounds`: Limits the number of back-and-forth turns to prevent infinite loops.
- `referenceId` / `referenceType`: Links this session to a business object (e.g., a specific ChatSession or Ticket).

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.extraction.model.ExtractionSchema`: The definition being used.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
This entity manages the state of "slot filling".
1. **Start**: User says "I want to return a shirt." -> Session created with `schema="return_request"`.
2. **Turn 1**: AI checks schema, sees `order_id` is missing. AI asks "What is your order number?". `missingFields=["order_id"]`.
3. **Turn 2**: User says "It's #12345". AI updates `extractedData={"order_id": "12345"}` and clears `missingFields`.
4. **Finish**: Status becomes `COMPLETED`.

## 5. Source Link
[ExtractionSession.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/extraction/model/ExtractionSession.java)
