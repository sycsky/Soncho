# KnowledgeEntryDto

## 1. Class Profile
- **Class Name**: `KnowledgeEntryDto`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: A lightweight summary of a Knowledge Base, used in lists (e.g., Bootstrap data) to avoid fetching heavy content.

## 2. Method Deep Dive
### Fields
- `id`: Knowledge Base UUID.
- `name`: Name of the knowledge base.
- `description`: Short description.
- `documentCount`: Number of documents contained (statistical info).
- `enabled`: Status flag.

## 3. Dependency Graph
- **Internal Dependencies**: None.

## 4. Usage Guide
Used in `BootstrapResponse` and Knowledge Base list views.
- **Dashboard**: Allows the frontend to display a card for each knowledge base with summary statistics.

## 5. Source Link
[KnowledgeEntryDto.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/KnowledgeEntryDto.java)
