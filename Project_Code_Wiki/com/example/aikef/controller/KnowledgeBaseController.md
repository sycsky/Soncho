# Class Profile: KnowledgeBaseController

**File Path**: `com/example/aikef/controller/KnowledgeBaseController.java`
**Type**: Controller (`@RestController`)
**Purpose**: Manages the RAG (Retrieval-Augmented Generation) knowledge bases. It provides comprehensive APIs for managing knowledge bases (creation, configuration), managing documents (add, update, delete, chunking), and performing semantic search operations.

# Method Deep Dive

## Knowledge Base Management
- **`getAllKnowledgeBases(...)`**: Lists available knowledge bases.
- **`createKnowledgeBase(...)`**: Creates a new knowledge base, linking it to an embedding model.
- **`rebuildIndex(id)`**: Triggers a background task to re-index all documents in the vector store.

## Document Management
- **`getDocuments(kbId, ...)`**: Lists documents within a knowledge base.
- **`addDocument(kbId, ...)`**: Uploads/Adds a document. Handles text splitting (chunking) configuration.
- **`reprocessDocument(docId)`**: Re-runs the chunking and embedding process for a document.

## Search & Test
- **`search(kbId, ...)`**: Performs a semantic search within a specific knowledge base.
- **`searchMultiple(...)`**: Searches across multiple knowledge bases simultaneously.
- **`testKnowledgeBase(...)`**: A diagnostic endpoint to verify retrieval quality for a specific query.
- **`batchTestKnowledgeBase(...)`**: Runs multiple queries to evaluate overall performance (accuracy/latency).

## Cache
- **`clearCache()`**: Clears any in-memory caches for vector store results.

# Dependency Graph

**Core Dependencies**:
- `KnowledgeBaseService`: Orchestrates KB operations.
- `VectorStoreService`: Manages interactions with the vector database (e.g., pgvector, Milvus).
- `AgentPrincipal`: Security context.

**Key Imports**:
```java
import com.example.aikef.knowledge.KnowledgeBaseService;
import com.example.aikef.knowledge.VectorStoreService;
import com.example.aikef.model.KnowledgeBase;
import org.springframework.web.bind.annotation.*;
```

# Usage Guide

## Search Example
`POST /api/v1/knowledge-bases/{id}/search`
```json
{
  "query": "How to reset password?",
  "maxResults": 3,
  "minScore": 0.7
}
```

## Adding a Document
`POST /api/v1/knowledge-bases/{id}/documents`
```json
{
  "title": "Password Reset Guide",
  "content": "To reset your password, go to settings...",
  "docType": "TEXT",
  "chunkSize": 500
}
```

# Source Link
[KnowledgeBaseController.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/controller/KnowledgeBaseController.java)
