package com.example.aikef.controller;

import com.example.aikef.extraction.model.ExtractionSchema;
import com.example.aikef.extraction.model.ExtractionSession;
import com.example.aikef.extraction.model.FieldDefinition;
import com.example.aikef.extraction.service.StructuredExtractionService;
import com.example.aikef.extraction.service.StructuredExtractionService.*;
import com.example.aikef.security.AgentPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 结构化提取 API
 */
@RestController
@RequestMapping("/api/v1/extraction")
@RequiredArgsConstructor
public class ExtractionController {

    private final StructuredExtractionService extractionService;

    // ==================== Schema 管理 ====================

    /**
     * 获取所有启用的提取模式
     */
    @GetMapping("/schemas")
    public List<SchemaDto> getSchemas() {
        return extractionService.getEnabledSchemas().stream()
                .map(this::toSchemaDto)
                .toList();
    }

    /**
     * 获取模式详情
     */
    @GetMapping("/schemas/{id}")
    public ResponseEntity<SchemaDto> getSchema(@PathVariable UUID id) {
        return extractionService.getSchema(id)
                .map(this::toSchemaDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建提取模式
     */
    @PostMapping("/schemas")
    public SchemaDto createSchema(
            @Valid @RequestBody CreateSchemaDto request,
            Authentication authentication) {
        UUID createdBy = getCurrentAgentId(authentication);

        ExtractionSchema schema = extractionService.createSchema(
                new CreateSchemaRequest(
                        request.name(),
                        request.description(),
                        request.fields(),
                        request.extractionPrompt(),
                        request.followupPrompt(),
                        request.llmModelId()
                ),
                createdBy
        );

        return toSchemaDto(schema);
    }

    // ==================== 提取会话 ====================

    /**
     * 创建提取会话
     */
    @PostMapping("/sessions")
    public ExtractionResult createSession(
            @Valid @RequestBody CreateSessionDto request,
            Authentication authentication) {
        UUID createdBy = getCurrentAgentId(authentication);

        ExtractionSession session = extractionService.createSession(
                request.schemaId(),
                createdBy,
                request.referenceId(),
                request.referenceType()
        );

        // 返回初始状态
        return extractionService.getSession(session.getId())
                .map(s -> new ExtractionResult(
                        s.getId(),
                        s.getStatus(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        0,
                        s.getMaxRounds(),
                        "请提供要提取的信息。",
                        false
                ))
                .orElseThrow();
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<SessionDto> getSession(@PathVariable UUID sessionId) {
        return extractionService.getSession(sessionId)
                .map(this::toSessionDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 提交文本进行提取
     */
    @PostMapping("/sessions/{sessionId}/submit")
    public ExtractionResult submitText(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SubmitTextDto request) {
        return extractionService.submitText(sessionId, request.text());
    }

    /**
     * 手动更新字段
     */
    @PutMapping("/sessions/{sessionId}/fields/{fieldName}")
    public ExtractionResult updateField(
            @PathVariable UUID sessionId,
            @PathVariable String fieldName,
            @RequestBody UpdateFieldDto request) {
        return extractionService.updateField(sessionId, fieldName, request.value());
    }

    /**
     * 完成会话
     */
    @PostMapping("/sessions/{sessionId}/complete")
    public ExtractionResult completeSession(@PathVariable UUID sessionId) {
        return extractionService.completeSession(sessionId);
    }

    /**
     * 取消会话
     */
    @PostMapping("/sessions/{sessionId}/cancel")
    public ResponseEntity<Void> cancelSession(@PathVariable UUID sessionId) {
        extractionService.cancelSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 一次性提取（简化接口，自动创建会话并提取）
     */
    @PostMapping("/extract")
    public ExtractionResult extractOnce(
            @Valid @RequestBody ExtractOnceDto request,
            Authentication authentication) {
        UUID createdBy = getCurrentAgentId(authentication);

        // 创建会话
        ExtractionSession session = extractionService.createSession(
                request.schemaId(),
                createdBy,
                request.referenceId(),
                request.referenceType()
        );

        // 提交文本
        return extractionService.submitText(session.getId(), request.text());
    }

    // ==================== DTOs ====================

    public record CreateSchemaDto(
            @NotBlank String name,
            String description,
            @NotNull List<FieldDefinition> fields,
            String extractionPrompt,
            String followupPrompt,
            UUID llmModelId
    ) {}

    public record CreateSessionDto(
            @NotNull UUID schemaId,
            UUID referenceId,
            String referenceType
    ) {}

    public record SubmitTextDto(
            @NotBlank String text
    ) {}

    public record UpdateFieldDto(
            Object value
    ) {}

    public record ExtractOnceDto(
            @NotNull UUID schemaId,
            @NotBlank String text,
            UUID referenceId,
            String referenceType
    ) {}

    public record SchemaDto(
            UUID id,
            String name,
            String description,
            List<FieldDefinition> fields,
            String extractionPrompt,
            String followupPrompt,
            UUID llmModelId,
            Boolean enabled,
            String createdAt
    ) {}

    public record SessionDto(
            UUID id,
            UUID schemaId,
            String schemaName,
            ExtractionSession.SessionStatus status,
            Map<String, Object> extractedData,
            List<String> missingFields,
            int currentRound,
            int maxRounds,
            UUID referenceId,
            String referenceType,
            String createdAt
    ) {}

    // ==================== Converters ====================

    private SchemaDto toSchemaDto(ExtractionSchema schema) {
        List<FieldDefinition> fields;
        try {
            fields = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(schema.getFieldsJson(), 
                            new com.fasterxml.jackson.core.type.TypeReference<List<FieldDefinition>>() {});
        } catch (Exception e) {
            fields = List.of();
        }

        return new SchemaDto(
                schema.getId(),
                schema.getName(),
                schema.getDescription(),
                fields,
                schema.getExtractionPrompt(),
                schema.getFollowupPrompt(),
                schema.getLlmModelId(),
                schema.getEnabled(),
                schema.getCreatedAt() != null ? schema.getCreatedAt().toString() : null
        );
    }

    private SessionDto toSessionDto(ExtractionSession session) {
        Map<String, Object> extractedData;
        List<String> missingFields;
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            extractedData = session.getExtractedData() != null 
                    ? mapper.readValue(session.getExtractedData(), 
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {})
                    : Map.of();
            missingFields = session.getMissingFields() != null
                    ? mapper.readValue(session.getMissingFields(),
                            new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
                    : List.of();
        } catch (Exception e) {
            extractedData = Map.of();
            missingFields = List.of();
        }

        return new SessionDto(
                session.getId(),
                session.getSchema().getId(),
                session.getSchema().getName(),
                session.getStatus(),
                extractedData,
                missingFields,
                session.getCurrentRound(),
                session.getMaxRounds(),
                session.getReferenceId(),
                session.getReferenceType(),
                session.getCreatedAt() != null ? session.getCreatedAt().toString() : null
        );
    }

    private UUID getCurrentAgentId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AgentPrincipal) {
            return ((AgentPrincipal) authentication.getPrincipal()).getId();
        }
        return null;
    }
}

