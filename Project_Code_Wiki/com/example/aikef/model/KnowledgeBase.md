# KnowledgeBase

## 1. Class Profile
- **Class Name**: `KnowledgeBase`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Configuration Entity
- **Purpose**: Represents a collection of documents used for RAG (Retrieval-Augmented Generation). It maps to a vector index in the underlying vector database (e.g., Redis).

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `name`: Human-readable name (e.g., "Product Manuals").
- `description`: Usage description.
- `indexName`: The technical name used in the vector database (e.g., `kb_12345...`).
- `embeddingModelId`: Reference to the `LlmModel` used to generate vectors for this base.
- `vectorDimension`: The dimensionality of the vectors (e.g., 1536 for OpenAI).
- `documentCount`: Cached counter for UI display.
- `enabled`: Toggle for availability.
- `createdByAgent`: The creator.

### Lifecycle
- `prePersist()`: Automatically generates a safe `indexName` if one isn't provided.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Agent`: Creator.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
This entity is the container for knowledge.
- **Creation**: When an agent creates a "Sales KB", the system also initializes a corresponding index in Redis.
- **Usage**: When a workflow uses a "Knowledge Search" node, it references this `KnowledgeBase` ID to know which index to query.

## 5. Source Link
[KnowledgeBase.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/KnowledgeBase.java)
