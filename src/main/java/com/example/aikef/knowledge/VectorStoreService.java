package com.example.aikef.knowledge;

import com.example.aikef.model.KnowledgeBase;
import com.example.aikef.model.KnowledgeDocument;
import com.example.aikef.model.LlmModel;
import com.example.aikef.repository.KnowledgeBaseRepository;
import com.example.aikef.repository.KnowledgeDocumentRepository;
import com.example.aikef.llm.LlmModelService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 向量存储服务
 * 使用 PGVector (PostgreSQL) 作为向量数据库，支持多知识库
 */
@Slf4j
@Service
public class VectorStoreService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final LlmModelService llmModelService;

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

    @Value("${knowledge.embedding.default-model:text-embedding-3-small}")
    private String defaultEmbeddingModel;

    @Value("${knowledge.embedding.default-dimension:1536}")
    private int defaultDimension;

    // 缓存 EmbeddingStore 实例（按知识库表名）
    private final Map<String, PgVectorEmbeddingStore> storeCache = new ConcurrentHashMap<>();
    
    // 缓存 EmbeddingModel 实例
    private final Map<UUID, EmbeddingModel> embeddingModelCache = new ConcurrentHashMap<>();

    public VectorStoreService(
            KnowledgeBaseRepository knowledgeBaseRepository,
            KnowledgeDocumentRepository documentRepository,
            LlmModelService llmModelService) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.documentRepository = documentRepository;
        this.llmModelService = llmModelService;
    }

    /**
     * 获取或创建 PGVector 向量存储
     * 每个知识库使用独立的表
     */
    public PgVectorEmbeddingStore getOrCreateStore(KnowledgeBase kb) {
        String tableName = kb.getIndexName();
        
        return storeCache.computeIfAbsent(tableName, name -> {
            log.info("创建 PGVector 向量存储: table={}, dimension={}", name, kb.getVectorDimension());
            
            return PgVectorEmbeddingStore.builder()
                    .host(pgHost)
                    .port(pgPort)
                    .database(pgDatabase)
                    .user(pgUser)
                    .password(pgPassword)
                    .table(name)
                    .dimension(kb.getVectorDimension())
                    .createTable(true)  // 自动创建表
                    .dropTableFirst(false)  // 不删除已有表
                    .build();
        });
    }

    /**
     * 获取嵌入模型
     */
    public EmbeddingModel getEmbeddingModel(KnowledgeBase kb) {
        UUID modelId = kb.getEmbeddingModelId();
        
        if (modelId == null) {
            // 使用默认 OpenAI 嵌入模型
            return embeddingModelCache.computeIfAbsent(
                    UUID.fromString("00000000-0000-0000-0000-000000000000"),
                    id -> createDefaultEmbeddingModel());
        }
        
        return embeddingModelCache.computeIfAbsent(modelId, id -> {
            LlmModel model = llmModelService.getModel(id);
            return createEmbeddingModel(model);
        });
    }

    private EmbeddingModel createDefaultEmbeddingModel() {
        log.info("创建默认 OpenAI 嵌入模型: {}", defaultEmbeddingModel);
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("未配置 OPENAI_API_KEY 环境变量");
        }
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(defaultEmbeddingModel)
                .build();
    }

    private EmbeddingModel createEmbeddingModel(LlmModel model) {
        log.info("创建嵌入模型: provider={}, model={}", model.getProvider(), model.getModelName());
        
        String provider = model.getProvider();
        return switch (provider) {
            case "OPENAI" -> OpenAiEmbeddingModel.builder()
                    .apiKey(model.getApiKey())
                    .baseUrl(model.getBaseUrl() != null ? model.getBaseUrl() : "https://api.openai.com/v1")
                    .modelName(model.getModelName())
                    .build();
            case "AZURE_OPENAI" -> AzureOpenAiEmbeddingModel.builder()
                    .apiKey(model.getApiKey())
                    .endpoint(model.getBaseUrl())
                    .deploymentName(model.getAzureDeploymentName())
                    .build();
            case "OLLAMA" -> OllamaEmbeddingModel.builder()
                    .baseUrl(model.getBaseUrl() != null ? model.getBaseUrl() : "http://localhost:11434")
                    .modelName(model.getModelName())
                    .timeout(Duration.ofMinutes(2))
                    .build();
            default -> throw new IllegalArgumentException("不支持的嵌入模型提供商: " + provider);
        };
    }

    /**
     * 处理文档：分块并向量化存储
     */
    @Async
    @Transactional
    public void processDocument(UUID documentId) {
        KnowledgeDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + documentId));
        
        try {
            doc.setStatus(KnowledgeDocument.ProcessStatus.PROCESSING);
            documentRepository.save(doc);
            
            KnowledgeBase kb = doc.getKnowledgeBase();
            PgVectorEmbeddingStore store = getOrCreateStore(kb);
            EmbeddingModel embeddingModel = getEmbeddingModel(kb);
            
            // ★ 先删除旧的向量数据（使用元数据匹配）
            try {
                deleteDocumentVectors(doc);
            } catch (Exception e) {
                log.warn("清理旧向量时出现异常，继续处理: documentId={}, error={}", documentId, e.getMessage());
            }
            
            // 创建文档，添加元数据
            Document document = Document.from(doc.getContent(), Metadata.from(Map.of(
                    "documentId", doc.getId().toString(),
                    "knowledgeBaseId", kb.getId().toString(),
                    "title", doc.getTitle(),
                    "docType", doc.getDocType().name()
            )));
            
            // 分块
            DocumentSplitter splitter = DocumentSplitters.recursive(
                    doc.getChunkSize(),
                    doc.getChunkOverlap()
            );
            List<TextSegment> segments = splitter.split(document);
            
            log.info("文档分块完成: documentId={}, chunkCount={}", documentId, segments.size());
            
            // 向量化并存储
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            store.addAll(embeddings, segments);
            
            log.info("向量存储完成: documentId={}, vectorCount={}", documentId, segments.size());

            // 更新状态
            doc.setChunkCount(segments.size());
            doc.setStatus(KnowledgeDocument.ProcessStatus.COMPLETED);
            doc.setErrorMessage(null);
            documentRepository.save(doc);
            
            // 更新知识库文档计数
            updateKnowledgeBaseDocCount(kb.getId());
            
            log.info("文档向量化完成: documentId={}, title={}", documentId, doc.getTitle());
            
        } catch (Exception e) {
            log.error("文档处理失败: documentId={}", documentId, e);
            doc.setStatus(KnowledgeDocument.ProcessStatus.FAILED);
            doc.setErrorMessage(e.getMessage());
            documentRepository.save(doc);
        }
    }

    /**
     * 从知识库搜索相关内容
     */
    public List<SearchResult> search(UUID knowledgeBaseId, String query, int maxResults, double minScore) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在: " + knowledgeBaseId));
        
        return search(kb, query, maxResults, minScore);
    }

    /**
     * 从知识库搜索相关内容
     */
    public List<SearchResult> search(KnowledgeBase kb, String query, int maxResults, double minScore) {
        try {
            PgVectorEmbeddingStore store = getOrCreateStore(kb);
            EmbeddingModel embeddingModel = getEmbeddingModel(kb);
            
            // 将查询文本向量化
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            // 搜索相似向量
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(maxResults)
                    .minScore(minScore)
                    .build();
            
            EmbeddingSearchResult<TextSegment> searchResult = store.search(request);
            
            // 转换结果
            List<SearchResult> results = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
                TextSegment segment = match.embedded();
                if (segment != null) {
                    SearchResult result = new SearchResult();
                    result.setContent(segment.text());
                    result.setScore(match.score());
                    result.setDocumentId(getMetadataValue(segment, "documentId"));
                    result.setTitle(getMetadataValue(segment, "title"));
                    results.add(result);
                }
            }
            
            log.info("向量搜索完成: knowledgeBaseId={}, query={}, resultCount={}", 
                    kb.getId(), query.substring(0, Math.min(50, query.length())), results.size());
            
            return results;
            
        } catch (Exception e) {
            log.error("向量搜索失败: knowledgeBaseId={}", kb.getId(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 带过滤条件的搜索
     */
    public List<SearchResult> searchWithFilter(KnowledgeBase kb, String query, int maxResults, double minScore, Filter filter) {
        try {
            PgVectorEmbeddingStore store = getOrCreateStore(kb);
            EmbeddingModel embeddingModel = getEmbeddingModel(kb);
            
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(maxResults)
                    .minScore(minScore)
                    .filter(filter)  // 元数据过滤
                    .build();
            
            EmbeddingSearchResult<TextSegment> searchResult = store.search(request);
            
            List<SearchResult> results = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
                TextSegment segment = match.embedded();
                if (segment != null) {
                    SearchResult result = new SearchResult();
                    result.setContent(segment.text());
                    result.setScore(match.score());
                    result.setDocumentId(getMetadataValue(segment, "documentId"));
                    result.setTitle(getMetadataValue(segment, "title"));
                    results.add(result);
                }
            }
            
            return results;
            
        } catch (Exception e) {
            log.error("向量搜索失败: knowledgeBaseId={}", kb.getId(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 从多个知识库搜索
     */
    public List<SearchResult> searchMultiple(List<UUID> knowledgeBaseIds, String query, int maxResults, double minScore) {
        List<SearchResult> allResults = new ArrayList<>();
        
        for (UUID kbId : knowledgeBaseIds) {
            try {
                List<SearchResult> results = search(kbId, query, maxResults, minScore);
                allResults.addAll(results);
            } catch (Exception e) {
                log.warn("搜索知识库失败: kbId={}", kbId, e);
            }
        }
        
        // 按分数排序并限制结果数量
        return allResults.stream()
                .sorted(Comparator.comparing(SearchResult::getScore).reversed())
                .limit(maxResults)
                .toList();
    }

    /**
     * 删除文档的向量数据
     * 使用元数据过滤匹配删除，无需保存向量 IDs
     */
    public void deleteDocumentVectors(KnowledgeDocument doc) {
        try {
            KnowledgeBase kb = doc.getKnowledgeBase();
            PgVectorEmbeddingStore store = getOrCreateStore(kb);
            
            // 使用 documentId 元数据过滤删除
            Filter filter = MetadataFilterBuilder
                    .metadataKey("documentId")
                    .isEqualTo(doc.getId().toString());
            
            store.removeAll(filter);
            
            log.info("删除文档向量完成: documentId={}", doc.getId());
            
        } catch (Exception e) {
            log.error("删除文档向量失败: documentId={}", doc.getId(), e);
        }
    }

    /**
     * 删除知识库所有向量
     */
    public void deleteKnowledgeBaseVectors(KnowledgeBase kb) {
        try {
            PgVectorEmbeddingStore store = getOrCreateStore(kb);
            
            // 使用 knowledgeBaseId 元数据过滤删除
            Filter filter = MetadataFilterBuilder
                    .metadataKey("knowledgeBaseId")
                    .isEqualTo(kb.getId().toString());
            
            store.removeAll(filter);
            
            // 从缓存移除
            storeCache.remove(kb.getIndexName());
            
            log.info("删除知识库所有向量完成: knowledgeBaseId={}", kb.getId());
            
        } catch (Exception e) {
            log.error("删除知识库向量失败: knowledgeBaseId={}", kb.getId(), e);
        }
    }

    /**
     * 重建知识库索引
     */
    @Async
    @Transactional
    public void rebuildIndex(UUID knowledgeBaseId) {
        log.info("开始重建知识库索引: knowledgeBaseId={}", knowledgeBaseId);
        
        KnowledgeBase kb = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在: " + knowledgeBaseId));
        
        // 删除所有向量
        deleteKnowledgeBaseVectors(kb);
        
        // 清除缓存的 store
        storeCache.remove(kb.getIndexName());
        
        // 获取所有文档（不只是已完成的）
        List<KnowledgeDocument> docs = documentRepository.findByKnowledgeBase_Id(knowledgeBaseId);
        
        // 重置状态为待处理
        for (KnowledgeDocument doc : docs) {
            doc.setStatus(KnowledgeDocument.ProcessStatus.PENDING);
            doc.setChunkCount(0);
        }
        documentRepository.saveAll(docs);
        
        // 重新处理每个文档
        for (KnowledgeDocument doc : docs) {
            processDocument(doc.getId());
        }
        
        log.info("知识库索引重建任务已启动: knowledgeBaseId={}, documentCount={}", 
                knowledgeBaseId, docs.size());
    }

    /**
     * 更新知识库文档计数
     */
    private void updateKnowledgeBaseDocCount(UUID knowledgeBaseId) {
        int count = documentRepository.countByKnowledgeBase_Id(knowledgeBaseId);
        knowledgeBaseRepository.findById(knowledgeBaseId).ifPresent(kb -> {
            kb.setDocumentCount(count);
            knowledgeBaseRepository.save(kb);
        });
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        storeCache.clear();
        embeddingModelCache.clear();
        log.info("向量存储缓存已清除");
    }

    private String getMetadataValue(TextSegment segment, String key) {
        if (segment.metadata() != null) {
            return segment.metadata().getString(key);
        }
        return null;
    }

    /**
     * 搜索结果
     */
    @lombok.Data
    public static class SearchResult {
        private String content;
        private double score;
        private String documentId;
        private String title;
    }
}
