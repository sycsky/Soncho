package com.example.aikef.knowledge;

import com.example.aikef.model.Agent;
import com.example.aikef.model.KnowledgeBase;
import com.example.aikef.model.KnowledgeDocument;
import com.example.aikef.repository.AgentRepository;
import com.example.aikef.repository.KnowledgeBaseRepository;
import com.example.aikef.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 知识库管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final VectorStoreService vectorStoreService;
    private final AgentRepository agentRepository;
    private final com.example.aikef.repository.LlmModelRepository llmModelRepository;

    // ==================== 知识库 CRUD ====================

    /**
     * 获取所有知识库
     */
    public List<KnowledgeBase> getAllKnowledgeBases() {
        return knowledgeBaseRepository.findAll();
    }

    /**
     * 获取启用的知识库
     */
    public List<KnowledgeBase> getEnabledKnowledgeBases() {
        return knowledgeBaseRepository.findByEnabledTrue();
    }

    /**
     * 获取单个知识库
     */
    public Optional<KnowledgeBase> getKnowledgeBase(UUID id) {
        return knowledgeBaseRepository.findById(id);
    }

    /**
     * 创建知识库
     */
    @Transactional
    public KnowledgeBase createKnowledgeBase(CreateKnowledgeBaseRequest request, UUID agentId) {
        if (knowledgeBaseRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("知识库名称已存在: " + request.name());
        }

        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(request.name());
        kb.setDescription(request.description());

        // 自动设定 Embedding 模型
        UUID embeddingModelId = request.embeddingModelId();
        if (embeddingModelId == null) {
            List<com.example.aikef.model.LlmModel> embeddingModels = llmModelRepository.findByModelTypeAndEnabledTrueOrderBySortOrderAsc(com.example.aikef.model.LlmModel.ModelType.EMBEDDING);
            if (!embeddingModels.isEmpty()) {
                embeddingModelId = embeddingModels.get(0).getId();
            } else {
                log.warn("未找到可用的 EMBEDDING 模型，知识库将无法进行向量化");
            }
        }
        kb.setEmbeddingModelId(embeddingModelId);
        
        kb.setVectorDimension(request.vectorDimension() != null ? request.vectorDimension() : 1536);
        kb.setEnabled(true);

        if (agentId != null) {
            agentRepository.findById(agentId).ifPresent(kb::setCreatedByAgent);
        }

        KnowledgeBase saved = knowledgeBaseRepository.save(kb);
        log.info("创建知识库: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    /**
     * 更新知识库
     */
    @Transactional
    public KnowledgeBase updateKnowledgeBase(UUID id, UpdateKnowledgeBaseRequest request) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在: " + id));

        boolean embeddingModelChanged = false;

        if (request.name() != null && !request.name().equals(kb.getName())) {
            if (knowledgeBaseRepository.existsByName(request.name())) {
                throw new IllegalArgumentException("知识库名称已存在: " + request.name());
            }
            kb.setName(request.name());
        }

        if (request.description() != null) {
            kb.setDescription(request.description());
        }

        if (request.enabled() != null) {
            kb.setEnabled(request.enabled());
        }

        // 检查嵌入模型是否变更
        if (request.embeddingModelId() != null) {
            UUID oldModelId = kb.getEmbeddingModelId();
            if (!request.embeddingModelId().equals(oldModelId)) {
                kb.setEmbeddingModelId(request.embeddingModelId());
                embeddingModelChanged = true;
                log.info("知识库嵌入模型已更新: id={}, oldModelId={}, newModelId={}", 
                        id, oldModelId, request.embeddingModelId());
            }
        }

        // 更新向量维度
        if (request.vectorDimension() != null && !request.vectorDimension().equals(kb.getVectorDimension())) {
            kb.setVectorDimension(request.vectorDimension());
            embeddingModelChanged = true;  // 维度变化也需要重建
            log.info("知识库向量维度已更新: id={}, newDimension={}", id, request.vectorDimension());
        }

        KnowledgeBase saved = knowledgeBaseRepository.save(kb);

        // 如果嵌入模型或维度变更，清除缓存
        if (embeddingModelChanged) {
            vectorStoreService.clearCache();
            log.warn("嵌入模型或向量维度已变更，已清除缓存。建议重建索引以确保向量一致性。");
        }

        log.info("更新知识库: id={}", id);
        return saved;
    }

    /**
     * 删除知识库
     */
    @Transactional
    public void deleteKnowledgeBase(UUID id) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在: " + id));

        // 删除所有文档
        documentRepository.deleteByKnowledgeBase_Id(id);

        // 清除向量存储缓存
        vectorStoreService.clearCache();

        // 删除知识库
        knowledgeBaseRepository.delete(kb);

        log.info("删除知识库: id={}, name={}", id, kb.getName());
    }

    // ==================== 文档管理 ====================

    /**
     * 获取知识库的文档列表
     */
    public Page<KnowledgeDocument> getDocuments(UUID knowledgeBaseId, Pageable pageable) {
        return documentRepository.findByKnowledgeBase_Id(knowledgeBaseId, pageable);
    }

    /**
     * 获取单个文档
     */
    public Optional<KnowledgeDocument> getDocument(UUID documentId) {
        return documentRepository.findById(documentId);
    }

    /**
     * 添加文档到知识库
     */
    @Transactional
    public KnowledgeDocument addDocument(UUID knowledgeBaseId, AddDocumentRequest request) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在: " + knowledgeBaseId));

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setKnowledgeBase(kb);
        doc.setTitle(request.title());
        doc.setContent(request.content());
        doc.setDocType(request.docType() != null ? request.docType() : KnowledgeDocument.DocumentType.TEXT);
        doc.setSourceUrl(request.sourceUrl());
        doc.setChunkSize(request.chunkSize() != null ? request.chunkSize() : 500);
        doc.setChunkOverlap(request.chunkOverlap() != null ? request.chunkOverlap() : 50);
        doc.setMetadataJson(request.metadata());
        doc.setStatus(KnowledgeDocument.ProcessStatus.PENDING);

        KnowledgeDocument saved = documentRepository.save(doc);
        
        final UUID documentId = saved.getId();

        // 在事务提交后异步处理文档向量化
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                vectorStoreService.processDocument(documentId);
            }
        });

        log.info("添加文档: documentId={}, title={}, knowledgeBaseId={}", 
                saved.getId(), saved.getTitle(), knowledgeBaseId);
        return saved;
    }

    /**
     * 批量添加文档
     */
    @Transactional
    public List<KnowledgeDocument> addDocuments(UUID knowledgeBaseId, List<AddDocumentRequest> requests) {
        return requests.stream()
                .map(req -> addDocument(knowledgeBaseId, req))
                .toList();
    }

    /**
     * 更新文档
     */
    @Transactional
    public KnowledgeDocument updateDocument(UUID documentId, UpdateDocumentRequest request) {
        KnowledgeDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + documentId));

        boolean needReprocess = false;

        if (request.title() != null) {
            doc.setTitle(request.title());
        }

        if (request.content() != null && !request.content().equals(doc.getContent())) {
            doc.setContent(request.content());
            needReprocess = true;
        }

        if (request.chunkSize() != null && !request.chunkSize().equals(doc.getChunkSize())) {
            doc.setChunkSize(request.chunkSize());
            needReprocess = true;
        }

        if (request.chunkOverlap() != null && !request.chunkOverlap().equals(doc.getChunkOverlap())) {
            doc.setChunkOverlap(request.chunkOverlap());
            needReprocess = true;
        }

        if (request.metadata() != null) {
            doc.setMetadataJson(request.metadata());
        }

        // 如果内容或分块参数变化，重新处理
        if (needReprocess) {
            doc.setStatus(KnowledgeDocument.ProcessStatus.PENDING);
        }
        
        KnowledgeDocument saved = documentRepository.save(doc);

        // 在事务提交后异步处理文档向量化
        if (needReprocess) {
            final UUID docId = saved.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    vectorStoreService.processDocument(docId);
                }
            });
        }

        log.info("更新文档: documentId={}", documentId);
        return saved;
    }

    /**
     * 删除文档
     */
    @Transactional
    public void deleteDocument(UUID documentId) {
        KnowledgeDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + documentId));

        UUID kbId = doc.getKnowledgeBase().getId();

        // 删除向量数据
        vectorStoreService.deleteDocumentVectors(doc);

        // 删除文档记录
        documentRepository.delete(doc);

        log.info("删除文档: documentId={}", documentId);
    }

    /**
     * 重新处理文档
     */
    @Transactional
    public void reprocessDocument(UUID documentId) {
        KnowledgeDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + documentId));

        doc.setStatus(KnowledgeDocument.ProcessStatus.PENDING);
        documentRepository.save(doc);

        vectorStoreService.processDocument(documentId);
        log.info("重新处理文档: documentId={}", documentId);
    }

    /**
     * 重建知识库索引
     */
    public void rebuildIndex(UUID knowledgeBaseId) {
        vectorStoreService.rebuildIndex(knowledgeBaseId);
    }

    // ==================== 搜索 ====================

    /**
     * 搜索知识库
     */
    public List<VectorStoreService.SearchResult> search(UUID knowledgeBaseId, String query, int maxResults, double minScore) {
        return vectorStoreService.search(knowledgeBaseId, query, maxResults, minScore);
    }

    /**
     * 从多个知识库搜索
     */
    public List<VectorStoreService.SearchResult> searchMultiple(List<UUID> knowledgeBaseIds, String query, int maxResults, double minScore) {
        return vectorStoreService.searchMultiple(knowledgeBaseIds, query, maxResults, minScore);
    }

    // ==================== Request Records ====================

    public record CreateKnowledgeBaseRequest(
            String name,
            String description,
            UUID embeddingModelId,
            Integer vectorDimension
    ) {}

    public record UpdateKnowledgeBaseRequest(
            String name,
            String description,
            Boolean enabled,
            UUID embeddingModelId,
            Integer vectorDimension
    ) {}

    public record AddDocumentRequest(
            String title,
            String content,
            KnowledgeDocument.DocumentType docType,
            String sourceUrl,
            Integer chunkSize,
            Integer chunkOverlap,
            String metadata
    ) {}

    public record UpdateDocumentRequest(
            String title,
            String content,
            Integer chunkSize,
            Integer chunkOverlap,
            String metadata
    ) {}

    public record SearchRequest(
            String query,
            Integer maxResults,
            Double minScore
    ) {}
}


