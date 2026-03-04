# VectorStoreService

## Class Profile
`VectorStoreService` manages the interaction with the vector database (PGVector). It handles embedding generation and similarity search for the knowledge base. It supports model creation from configured providers and includes database bootstrap behavior for missing PG databases.

## Method Deep Dive

### `processDocument(UUID documentId)`
- **Description**: Asynchronously processes a document: splits it into chunks, generates embeddings, and stores them.
- **Annotation**: `@Async`, `@Transactional`.
- **Logic**:
    1.  Loads document.
    2.  Deletes existing vectors for this document (to support updates).
    3.  Splits text using `DocumentSplitters.recursive`.
    4.  Generates embeddings via `EmbeddingModel`.
    5.  Stores vectors in `PgVectorEmbeddingStore`.
    6.  Updates document status to `COMPLETED`.

### `getOrCreateStore(KnowledgeBase kb)`
- **Description**: Creates a `PgVectorEmbeddingStore` instance tailored to the KB's configuration (table name, dimension).
- **Logic**: Ensures target PG database exists first, then creates/caches store instance by table name.

### `createDefaultEmbeddingModel()`
- **Description**: Resolves default embedding model with priority.
- **Logic**:
    1.  Try `LlmModelService.getDefaultEmbeddingModel()` from DB.
    2.  If absent, fallback to `knowledge.embedding.default-model` + `OPENAI_API_KEY`.
    3.  Build model via provider-aware factory.

### `createEmbeddingModel(LlmModel model)`
- **Description**: Factory method to create LangChain4j `EmbeddingModel` based on the provider (OpenAI, Azure, Ollama).

### `search(...)`
- **Description**: Performs vector similarity search.
- **Logic**: Embeds the query string and executes a search against the PGVector store.

## Dependency Graph
- `KnowledgeBaseRepository`, `KnowledgeDocumentRepository`: DB access.
- `LlmModelService`: To retrieve embedding model configurations.
- `PgVectorEmbeddingStore` (LangChain4j): The underlying vector store implementation.
- PostgreSQL JDBC: Used to verify/create target database before vector table initialization.

## Usage Guide
Used internally by `KnowledgeBaseService`.

```java
// Rebuild index for a KB
vectorStoreService.rebuildIndex(kbId);

// Search
var results = vectorStoreService.search(kbId, "How do I reset password?", 5, 0.7);
```

## Source Link
[VectorStoreService.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/knowledge/VectorStoreService.java)
