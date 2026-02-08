# VectorStoreService

## Class Profile
`VectorStoreService` manages the interaction with the vector database (PGVector). It handles the embedding of text using various LLM providers (OpenAI, Azure, Ollama) and performs similarity searches. It abstracts the complexity of `LangChain4j`'s `EmbeddingStore` and `EmbeddingModel`.

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
- **Logic**: Caches instances by table name to avoid overhead.

### `createEmbeddingModel(LlmModel model)`
- **Description**: Factory method to create LangChain4j `EmbeddingModel` based on the provider (OpenAI, Azure, Ollama).

### `search(...)`
- **Description**: Performs vector similarity search.
- **Logic**: Embeds the query string and executes a search against the PGVector store.

## Dependency Graph
- `KnowledgeBaseRepository`, `KnowledgeDocumentRepository`: DB access.
- `LlmModelService`: To retrieve embedding model configurations.
- `PgVectorEmbeddingStore` (LangChain4j): The underlying vector store implementation.

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
