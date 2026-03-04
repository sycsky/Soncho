package com.example.aikef.service;

import com.example.aikef.event.MessageSentEvent;
import com.example.aikef.knowledge.VectorStoreService;
import com.example.aikef.model.Message;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.data.document.Metadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ChatMemoryService {

    private final VectorStoreService vectorStoreService;
    private PgVectorEmbeddingStore embeddingStore;

    @Value("${knowledge.pgvector.host:localhost}")
    private String pgHost;

    @Value("${knowledge.pgvector.port:5432}")
    private int pgPort;

    @Value("${knowledge.pgvector.database:aikef_vector}")
    private String pgDatabase;

    @Value("${knowledge.pgvector.user:postgres}")
    private String pgUser;

    @Value("${knowledge.pgvector.password:}")
    private String pgPassword;

    private static final String TABLE_NAME = "chat_message_embeddings";
    private static final int DIMENSION = 1536;

    public ChatMemoryService(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    private synchronized PgVectorEmbeddingStore getStore() {
        if (embeddingStore == null) {
            embeddingStore = PgVectorEmbeddingStore.builder()
                    .host(pgHost)
                    .port(pgPort)
                    .database(pgDatabase)
                    .user(pgUser)
                    .password(pgPassword)
                    .table(TABLE_NAME)
                    .dimension(DIMENSION)
                    .createTable(true)
                    .dropTableFirst(false)
                    .build();
        }
        return embeddingStore;
    }

    @Async
    @EventListener
    public void onMessageSent(MessageSentEvent event) {
        Message message = event.getMessage();
        if (message.getText() == null || message.getText().trim().isEmpty()) {
            return;
        }

        try {
            log.info("Async vectorization for message: {}", message.getId());
            
            String customerId = message.getSession().getCustomer() != null ? 
                                message.getSession().getCustomer().getId().toString() : "anonymous";
            
            Metadata metadata = Metadata.from(Map.of(
                    "messageId", message.getId().toString(),
                    "sessionId", message.getSession().getId().toString(),
                    "customerId", customerId,
                    "senderType", message.getSenderType().name(),
                    "createdAt", message.getCreatedAt().toString()
            ));

            TextSegment segment = TextSegment.from(message.getText(), metadata);
            EmbeddingModel model = vectorStoreService.getChatEmbeddingModel();
            Embedding embedding = model.embed(segment).content();

            getStore().add(embedding, segment);
            
            log.info("Message vectorized successfully: {}", message.getId());
        } catch (Exception e) {
            log.error("Failed to vectorize message: {}", message.getId(), e);
        }
    }

    public List<String> searchRelevantHistory(String customerId, String query, int maxResults) {
        try {
            EmbeddingModel model = vectorStoreService.getChatEmbeddingModel();
            Embedding queryEmbedding = model.embed(query).content();

            Filter filter = MetadataFilterBuilder
                    .metadataKey("customerId")
                    .isEqualTo(customerId);

            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(maxResults)
                    .minScore(0.7) // Relevance threshold
                    .filter(filter)
                    .build();

            EmbeddingSearchResult<TextSegment> result = getStore().search(request);

            List<String> history = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : result.matches()) {
                history.add(match.embedded().text());
            }
            return history;
        } catch (Exception e) {
            log.error("Failed to search chat history for customer: {}", customerId, e);
            return new ArrayList<>();
        }
    }
}
