# ExtractionSessionRepository

## 1. Class Profile
- **Class Name**: `ExtractionSessionRepository`
- **Package**: `com.example.aikef.extraction.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `ExtractionSession` entities.

## 2. Method Deep Dive
### Query Methods
- `findBySchema_Id(UUID schemaId)`: Finds all sessions using a specific schema (useful for analytics).
- `findByStatus(ExtractionSession.SessionStatus status)`: Finds sessions by their current state (e.g., all `IN_PROGRESS` sessions).
- `findByReferenceIdAndReferenceType(UUID referenceId, String referenceType)`: Finds extraction sessions linked to a specific business object (e.g., finding the active extraction session for a specific chat window).

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.extraction.model.ExtractionSession`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `ExtractionService` to maintain state across HTTP requests.
- **Resume**: When a user replies to a follow-up question, the system uses `findByReferenceId...` to find the active session and update it with the new answer.

## 5. Source Link
[ExtractionSessionRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/extraction/repository/ExtractionSessionRepository.java)
