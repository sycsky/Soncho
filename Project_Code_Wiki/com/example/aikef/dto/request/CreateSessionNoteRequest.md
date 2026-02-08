# CreateSessionNoteRequest

## 1. Class Profile
- **Class Name**: `CreateSessionNoteRequest`
- **Package**: `com.example.aikef.dto.request`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: Request payload to add an internal note to a session.

## 2. Method Deep Dive
### Fields
- `content`: The text of the note.

## 3. Dependency Graph
- **Internal Dependencies**: None.

## 4. Usage Guide
Used by `SessionController.addNote`.
- **Collaboration**: Agents leave notes for each other (e.g., "Customer is angry, be careful").

## 5. Source Link
[CreateSessionNoteRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/request/CreateSessionNoteRequest.java)
