package com.example.aikef.controller;

import com.example.aikef.knowledge.KnowledgeBaseService;
import com.example.aikef.knowledge.KnowledgeBaseService.*;
import com.example.aikef.knowledge.VectorStoreService;
import com.example.aikef.model.KnowledgeBase;
import com.example.aikef.model.KnowledgeDocument;
import com.example.aikef.security.AgentPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 知识库管理 API
 */
@RestController
@RequestMapping("/api/v1/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final VectorStoreService vectorStoreService;

    // ==================== 知识库 CRUD ====================

    /**
     * 获取所有知识库
     */
    @GetMapping
    public ResponseEntity<List<KnowledgeBaseDto>> getAllKnowledgeBases(
            @RequestParam(required = false, defaultValue = "false") boolean enabledOnly) {
        List<KnowledgeBase> bases = enabledOnly
                ? knowledgeBaseService.getEnabledKnowledgeBases()
                : knowledgeBaseService.getAllKnowledgeBases();
        return ResponseEntity.ok(bases.stream().map(this::toDto).toList());
    }

    /**
     * 获取单个知识库
     */
    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeBaseDto> getKnowledgeBase(@PathVariable UUID id) {
        return knowledgeBaseService.getKnowledgeBase(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建知识库
     */
    @PostMapping
    public ResponseEntity<KnowledgeBaseDto> createKnowledgeBase(
            @Valid @RequestBody CreateKnowledgeBaseDto request,
            Authentication authentication) {
        UUID agentId = getCurrentAgentId(authentication);
        KnowledgeBase kb = knowledgeBaseService.createKnowledgeBase(
                new CreateKnowledgeBaseRequest(
                        request.name(),
                        request.description(),
                        request.embeddingModelId(),
                        request.vectorDimension()
                ),
                agentId
        );
        return ResponseEntity.ok(toDto(kb));
    }

    /**
     * 更新知识库
     */
    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeBaseDto> updateKnowledgeBase(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateKnowledgeBaseDto request) {
        KnowledgeBase kb = knowledgeBaseService.updateKnowledgeBase(id,
                new UpdateKnowledgeBaseRequest(
                        request.name(),
                        request.description(),
                        request.enabled(),
                        request.embeddingModelId(),
                        request.vectorDimension()
                )
        );
        return ResponseEntity.ok(toDto(kb));
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKnowledgeBase(@PathVariable UUID id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 重建知识库索引
     */
    @PostMapping("/{id}/rebuild")
    public ResponseEntity<Map<String, String>> rebuildIndex(@PathVariable UUID id) {
        knowledgeBaseService.rebuildIndex(id);
        return ResponseEntity.ok(Map.of("message", "索引重建任务已启动"));
    }

    // ==================== 文档管理 ====================

    /**
     * 获取知识库的文档列表
     */
    @GetMapping("/{kbId}/documents")
    public ResponseEntity<Page<KnowledgeDocumentDto>> getDocuments(
            @PathVariable UUID kbId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<KnowledgeDocument> docs = knowledgeBaseService.getDocuments(kbId, pageable);
        return ResponseEntity.ok(docs.map(this::toDocDto));
    }

    /**
     * 获取单个文档
     */
    @GetMapping("/{kbId}/documents/{docId}")
    public ResponseEntity<KnowledgeDocumentDto> getDocument(
            @PathVariable UUID kbId,
            @PathVariable UUID docId) {
        return knowledgeBaseService.getDocument(docId)
                .map(this::toDocDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 添加文档
     */
    @PostMapping("/{kbId}/documents")
    public ResponseEntity<KnowledgeDocumentDto> addDocument(
            @PathVariable UUID kbId,
            @Valid @RequestBody AddDocumentDto request) {
        KnowledgeDocument doc = knowledgeBaseService.addDocument(kbId,
                new AddDocumentRequest(
                        request.title(),
                        request.content(),
                        request.docType(),
                        request.sourceUrl(),
                        request.chunkSize(),
                        request.chunkOverlap(),
                        request.metadata()
                )
        );
        return ResponseEntity.ok(toDocDto(doc));
    }

    /**
     * 批量添加文档
     */
    @PostMapping("/{kbId}/documents/batch")
    public ResponseEntity<List<KnowledgeDocumentDto>> addDocuments(
            @PathVariable UUID kbId,
            @Valid @RequestBody List<AddDocumentDto> requests) {
        List<AddDocumentRequest> addRequests = requests.stream()
                .map(r -> new AddDocumentRequest(
                        r.title(), r.content(), r.docType(),
                        r.sourceUrl(), r.chunkSize(), r.chunkOverlap(), r.metadata()
                ))
                .toList();
        List<KnowledgeDocument> docs = knowledgeBaseService.addDocuments(kbId, addRequests);
        return ResponseEntity.ok(docs.stream().map(this::toDocDto).toList());
    }

    /**
     * 更新文档
     */
    @PutMapping("/{kbId}/documents/{docId}")
    public ResponseEntity<KnowledgeDocumentDto> updateDocument(
            @PathVariable UUID kbId,
            @PathVariable UUID docId,
            @Valid @RequestBody UpdateDocumentDto request) {
        KnowledgeDocument doc = knowledgeBaseService.updateDocument(docId,
                new UpdateDocumentRequest(
                        request.title(),
                        request.content(),
                        request.chunkSize(),
                        request.chunkOverlap(),
                        request.metadata()
                )
        );
        return ResponseEntity.ok(toDocDto(doc));
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{kbId}/documents/{docId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID kbId,
            @PathVariable UUID docId) {
        knowledgeBaseService.deleteDocument(docId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 重新处理文档
     */
    @PostMapping("/{kbId}/documents/{docId}/reprocess")
    public ResponseEntity<Map<String, String>> reprocessDocument(
            @PathVariable UUID kbId,
            @PathVariable UUID docId) {
        knowledgeBaseService.reprocessDocument(docId);
        return ResponseEntity.ok(Map.of("message", "文档重新处理任务已启动"));
    }

    // ==================== 搜索 ====================

    /**
     * 搜索知识库
     */
    @PostMapping("/{kbId}/search")
    public ResponseEntity<List<SearchResultDto>> search(
            @PathVariable UUID kbId,
            @Valid @RequestBody SearchRequestDto request) {
        List<VectorStoreService.SearchResult> results = knowledgeBaseService.search(
                kbId,
                request.query(),
                request.maxResults() != null ? request.maxResults() : 5,
                request.minScore() != null ? request.minScore() : 0.7
        );
        return ResponseEntity.ok(results.stream().map(this::toSearchResultDto).toList());
    }

    /**
     * 从多个知识库搜索
     */
    @PostMapping("/search")
    public ResponseEntity<List<SearchResultDto>> searchMultiple(
            @Valid @RequestBody MultiSearchRequestDto request) {
        List<VectorStoreService.SearchResult> results = knowledgeBaseService.searchMultiple(
                request.knowledgeBaseIds(),
                request.query(),
                request.maxResults() != null ? request.maxResults() : 5,
                request.minScore() != null ? request.minScore() : 0.7
        );
        return ResponseEntity.ok(results.stream().map(this::toSearchResultDto).toList());
    }

    /**
     * 清除向量存储缓存
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        vectorStoreService.clearCache();
        return ResponseEntity.ok(Map.of("message", "缓存已清除"));
    }

    // ==================== 测试接口 ====================

    /**
     * 测试知识库搜索
     * 用于快速验证知识库配置是否正确、文档向量化是否成功
     */
    @PostMapping("/{kbId}/test")
    public ResponseEntity<TestResultDto> testKnowledgeBase(
            @PathVariable UUID kbId,
            @Valid @RequestBody TestRequestDto request) {
        
        long startTime = System.currentTimeMillis();
        
        // 获取知识库信息
        KnowledgeBase kb = knowledgeBaseService.getKnowledgeBase(kbId)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在: " + kbId));
        
        // 搜索
        List<VectorStoreService.SearchResult> results = knowledgeBaseService.search(
                kbId,
                request.query(),
                request.maxResults() != null ? request.maxResults() : 5,
                request.minScore() != null ? request.minScore() : 0.5
        );
        
        long searchTime = System.currentTimeMillis() - startTime;
        
        // 构建上下文
        StringBuilder contextBuilder = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            VectorStoreService.SearchResult r = results.get(i);
            contextBuilder.append(String.format("[%d] %s (相关度: %.2f%%)\n%s\n\n", 
                    i + 1, 
                    r.getTitle(), 
                    r.getScore() * 100,
                    r.getContent()));
        }
        
        // 转换结果
        List<TestSearchResultDto> searchResults = results.stream()
                .map(r -> new TestSearchResultDto(
                        r.getDocumentId(),
                        r.getTitle(),
                        r.getContent(),
                        r.getScore(),
                        String.format("%.2f%%", r.getScore() * 100)
                ))
                .toList();
        
        return ResponseEntity.ok(new TestResultDto(
                kb.getName(),
                kb.getDocumentCount(),
                request.query(),
                searchResults.size(),
                searchResults,
                contextBuilder.toString().trim(),
                searchTime,
                results.isEmpty() ? "未找到相关内容，请检查：1. 文档是否已向量化完成 2. 查询内容是否与文档相关 3. minScore 是否设置过高" : "搜索成功"
        ));
    }

    /**
     * 批量测试知识库（用于评估搜索质量）
     */
    @PostMapping("/{kbId}/test/batch")
    public ResponseEntity<BatchTestResultDto> batchTestKnowledgeBase(
            @PathVariable UUID kbId,
            @Valid @RequestBody BatchTestRequestDto request) {
        
        List<TestResultDto> results = new java.util.ArrayList<>();
        long totalTime = 0;
        int totalHits = 0;
        
        for (String query : request.queries()) {
            long startTime = System.currentTimeMillis();
            
            List<VectorStoreService.SearchResult> searchResults = knowledgeBaseService.search(
                    kbId,
                    query,
                    request.maxResults() != null ? request.maxResults() : 3,
                    request.minScore() != null ? request.minScore() : 0.5
            );
            
            long searchTime = System.currentTimeMillis() - startTime;
            totalTime += searchTime;
            totalHits += searchResults.size();
            
            List<TestSearchResultDto> testResults = searchResults.stream()
                    .map(r -> new TestSearchResultDto(
                            r.getDocumentId(),
                            r.getTitle(),
                            r.getContent(),
                            r.getScore(),
                            String.format("%.2f%%", r.getScore() * 100)
                    ))
                    .toList();
            
            results.add(new TestResultDto(
                    null,
                    null,
                    query,
                    testResults.size(),
                    testResults,
                    null,
                    searchTime,
                    null
            ));
        }
        
        double avgTime = request.queries().isEmpty() ? 0 : (double) totalTime / request.queries().size();
        double avgHits = request.queries().isEmpty() ? 0 : (double) totalHits / request.queries().size();
        
        return ResponseEntity.ok(new BatchTestResultDto(
                request.queries().size(),
                totalHits,
                avgHits,
                totalTime,
                avgTime,
                results
        ));
    }

    // ==================== DTOs ====================

    public record KnowledgeBaseDto(
            UUID id,
            String name,
            String description,
            String indexName,
            UUID embeddingModelId,
            Integer vectorDimension,
            Integer documentCount,
            Boolean enabled,
            String createdAt,
            String updatedAt
    ) {}

    public record CreateKnowledgeBaseDto(
            @NotBlank String name,
            String description,
            UUID embeddingModelId,
            Integer vectorDimension
    ) {}

    public record UpdateKnowledgeBaseDto(
            String name,
            String description,
            Boolean enabled,
            UUID embeddingModelId,
            Integer vectorDimension
    ) {}

    public record KnowledgeDocumentDto(
            UUID id,
            UUID knowledgeBaseId,
            String title,
            String content,
            String docType,
            String sourceUrl,
            Integer chunkSize,
            Integer chunkOverlap,
            Integer chunkCount,
            String status,
            String errorMessage,
            String createdAt,
            String updatedAt
    ) {}

    public record AddDocumentDto(
            @NotBlank String title,
            @NotBlank String content,
            KnowledgeDocument.DocumentType docType,
            String sourceUrl,
            Integer chunkSize,
            Integer chunkOverlap,
            String metadata
    ) {}

    public record UpdateDocumentDto(
            String title,
            String content,
            Integer chunkSize,
            Integer chunkOverlap,
            String metadata
    ) {}

    public record SearchRequestDto(
            @NotBlank String query,
            Integer maxResults,
            Double minScore
    ) {}

    public record MultiSearchRequestDto(
            @NotNull List<UUID> knowledgeBaseIds,
            @NotBlank String query,
            Integer maxResults,
            Double minScore
    ) {}

    public record SearchResultDto(
            String content,
            double score,
            String documentId,
            String title
    ) {}

    public record TestRequestDto(
            @NotBlank String query,
            Integer maxResults,
            Double minScore
    ) {}

    public record TestSearchResultDto(
            String documentId,
            String title,
            String content,
            double score,
            String scoreFormatted
    ) {}

    public record TestResultDto(
            String knowledgeBaseName,
            Integer documentCount,
            String query,
            int resultCount,
            List<TestSearchResultDto> results,
            String context,
            long searchTimeMs,
            String message
    ) {}

    public record BatchTestRequestDto(
            @NotNull List<String> queries,
            Integer maxResults,
            Double minScore
    ) {}

    public record BatchTestResultDto(
            int totalQueries,
            int totalHits,
            double avgHitsPerQuery,
            long totalTimeMs,
            double avgTimeMs,
            List<TestResultDto> results
    ) {}

    // ==================== Converters ====================

    private KnowledgeBaseDto toDto(KnowledgeBase kb) {
        return new KnowledgeBaseDto(
                kb.getId(),
                kb.getName(),
                kb.getDescription(),
                kb.getIndexName(),
                kb.getEmbeddingModelId(),
                kb.getVectorDimension(),
                kb.getDocumentCount(),
                kb.getEnabled(),
                kb.getCreatedAt() != null ? kb.getCreatedAt().toString() : null,
                kb.getUpdatedAt() != null ? kb.getUpdatedAt().toString() : null
        );
    }

    private KnowledgeDocumentDto toDocDto(KnowledgeDocument doc) {
        return new KnowledgeDocumentDto(
                doc.getId(),
                doc.getKnowledgeBase().getId(),
                doc.getTitle(),
                doc.getContent(),
                doc.getDocType().name(),
                doc.getSourceUrl(),
                doc.getChunkSize(),
                doc.getChunkOverlap(),
                doc.getChunkCount(),
                doc.getStatus().name(),
                doc.getErrorMessage(),
                doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null,
                doc.getUpdatedAt() != null ? doc.getUpdatedAt().toString() : null
        );
    }

    private SearchResultDto toSearchResultDto(VectorStoreService.SearchResult result) {
        return new SearchResultDto(
                result.getContent(),
                result.getScore(),
                result.getDocumentId(),
                result.getTitle()
        );
    }

    /**
     * 获取当前登录的客服ID
     */
    private UUID getCurrentAgentId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AgentPrincipal) {
            return ((AgentPrincipal) authentication.getPrincipal()).getId();
        }
        return null;
    }
}

