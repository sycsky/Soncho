# KnowledgeBaseService

## Class Profile
`KnowledgeBaseService` is the primary service for managing the AI Knowledge Base system. It handles the lifecycle of `KnowledgeBase` entities and their associated `KnowledgeDocument`s. It orchestrates the creation, updating, and deletion of knowledge bases and ensures that documents are asynchronously processed and vectorized via `VectorStoreService`.

## Method Deep Dive

### `createKnowledgeBase(CreateKnowledgeBaseRequest request, UUID agentId)`
- **Description**: Creates a new Knowledge Base.
- **Logic**: Checks for name uniqueness, sets defaults (vector dimension: 1536), and saves to DB.

### `addDocument(UUID knowledgeBaseId, AddDocumentRequest request)`
- **Description**: Adds a document to a KB.
- **Logic**:
    1.  Saves the document with `PENDING` status.
    2.  Registers a transaction synchronization to call `vectorStoreService.processDocument` *after* commit.
    3.  This ensures the DB transaction is complete before starting the potentially long-running vectorization.

### `updateDocument(UUID documentId, UpdateDocumentRequest request)`
- **Description**: Updates document content or metadata.
- **Logic**: If content or chunking parameters change, it marks the document as `PENDING` and triggers reprocessing.

### `search(UUID knowledgeBaseId, String query, ...)`
- **Description**: Delegates semantic search to `VectorStoreService`.

## Dependency Graph
- `KnowledgeBaseRepository`: DB access for KBs.
- `KnowledgeDocumentRepository`: DB access for documents.
- `VectorStoreService`: Handles embedding and vector storage.
- `AgentRepository`: For linking KBs to creators.

## Usage Guide
This service is typically called by `KnowledgeBaseController` to handle API requests.

```java
// Create a KB
KnowledgeBase kb = service.createKnowledgeBase(new CreateKnowledgeBaseRequest("Product Manual", ...), agentId);

// Add a document
service.addDocument(kb.getId(), new AddDocumentRequest("Manual V1", "Content...", ...));
```

## Source Link
[KnowledgeBaseService.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/knowledge/KnowledgeBaseService.java)
