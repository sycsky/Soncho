# KnowledgeDocument

## 1. Class Profile
- **Class Name**: `KnowledgeDocument`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Content Entity
- **Purpose**: Represents a single source file (PDF, Text, URL) within a Knowledge Base. It stores the raw content, processing status, and chunking configuration.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `knowledgeBase`: Parent knowledge base.
- `title`: Document name.
- `content`: Raw text content (extracted from PDF/HTML).
- `docType`: Format (`TEXT`, `MARKDOWN`, `HTML`, `PDF`, `URL`).
- `sourceUrl`: Original location (if applicable).
- `chunkSize` / `chunkOverlap`: Settings for the splitter algorithm.
- `chunkCount`: Number of vectors generated.
- `status`: Processing state (`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`).
- `errorMessage`: Failure reason.
- `metadataJson`: Custom tags or attributes.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.KnowledgeBase`: Parent.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
This entity tracks the ingestion pipeline.
1. **Upload**: User uploads a PDF. A `KnowledgeDocument` is created with status `PENDING`.
2. **Processing**: A background job picks it up, extracts text, splits it into chunks, and generates embeddings.
3. **Completion**: Status updates to `COMPLETED`, and `chunkCount` is updated.

## 5. Source Link
[KnowledgeDocument.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/KnowledgeDocument.java)
