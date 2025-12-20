package com.example.aikef.controller;

import com.example.aikef.extraction.model.FieldDefinition;
import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.tool.model.AiTool;
import com.example.aikef.tool.model.ToolExecution;
import com.example.aikef.tool.service.AiToolService;
import com.example.aikef.tool.service.AiToolService.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 工具 API
 */
@RestController
@RequestMapping("/api/v1/tools")
@RequiredArgsConstructor
public class AiToolController {

    private final AiToolService toolService;

    // ==================== 工具 CRUD ====================

    /**
     * 获取所有启用的工具
     */
    @GetMapping
    public List<ToolDto> getTools(@RequestParam(required = false) String keyword) {
        List<AiTool> tools;
        if (keyword != null && !keyword.isEmpty()) {
            tools = toolService.searchTools(keyword);
        } else {
            tools = toolService.getEnabledTools();
        }
        return tools.stream().map(this::toDto).toList();
    }

    /**
     * 获取工具详情
     */
    @GetMapping("/{toolId}")
    public ResponseEntity<ToolDto> getTool(@PathVariable UUID toolId) {
        return toolService.getTool(toolId)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根据名称获取工具
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<ToolDto> getToolByName(@PathVariable String name) {
        return toolService.getToolByName(name)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建工具
     */
    @PostMapping
    public ToolDto createTool(
            @Valid @RequestBody CreateToolDto request,
            Authentication authentication) {
        UUID createdBy = getCurrentAgentId(authentication);

        // 转换参数定义（支持嵌套的 properties 和 items）
        List<ParameterDefinition> parameters = request.parameters() != null
                ? request.parameters().stream()
                    .map(this::convertParameterDto)
                    .toList()
                : Collections.emptyList();

        AiTool tool = toolService.createTool(
                new CreateToolRequest(
                        request.name(),
                        request.displayName(),
                        request.description(),
                        request.toolType(),
                        parameters,
                        request.apiMethod(),
                        request.apiUrl(),
                        request.apiHeaders(),
                        request.apiBodyTemplate(),
                        request.apiResponsePath(),
                        request.apiTimeout(),
                        request.mcpEndpoint(),
                        request.mcpToolName(),
                        request.mcpServerType(),
                        request.mcpConfig(),
                        request.authType(),
                        request.authConfig(),
                        request.inputExample(),
                        request.outputExample(),
                        request.resultDescription(),
                        request.resultMetadata(),
                        request.retryCount(),
                        request.requireConfirmation(),
                        request.sortOrder(),
                        request.tags()
                ),
                createdBy
        );

        return toDto(tool);
    }

    /**
     * 覆盖创建工具（按 name 幂等）
     * <p>
     * 若存在同名工具：立即删除旧工具并复用旧 ID 创建新工具。
     */
    @PostMapping("/upsert-by-name")
    public ToolDto createOrReplaceToolByName(
            @Valid @RequestBody CreateToolDto request,
            Authentication authentication) {
        UUID createdBy = getCurrentAgentId(authentication);

        List<ParameterDefinition> parameters = request.parameters() != null
                ? request.parameters().stream()
                .map(this::convertParameterDto)
                .toList()
                : Collections.emptyList();

        AiTool tool = toolService.createOrReplaceToolByName(
                new CreateToolRequest(
                        request.name(),
                        request.displayName(),
                        request.description(),
                        request.toolType(),
                        parameters,
                        request.apiMethod(),
                        request.apiUrl(),
                        request.apiHeaders(),
                        request.apiBodyTemplate(),
                        request.apiResponsePath(),
                        request.apiTimeout(),
                        request.mcpEndpoint(),
                        request.mcpToolName(),
                        request.mcpServerType(),
                        request.mcpConfig(),
                        request.authType(),
                        request.authConfig(),
                        request.inputExample(),
                        request.outputExample(),
                        request.resultDescription(),
                        request.resultMetadata(),
                        request.retryCount(),
                        request.requireConfirmation(),
                        request.sortOrder(),
                        request.tags()
                ),
                createdBy
        );

        return toDto(tool);
    }

    /**
     * 更新工具
     */
    @PutMapping("/{toolId}")
    public ToolDto updateTool(
            @PathVariable UUID toolId,
            @Valid @RequestBody UpdateToolDto request) {

        // 转换参数定义（支持嵌套的 properties 和 items）
        List<ParameterDefinition> parameters = request.parameters() != null
                ? request.parameters().stream()
                    .map(this::convertParameterDto)
                    .toList()
                : null;

        AiTool tool = toolService.updateTool(toolId,
                new UpdateToolRequest(
                        request.displayName(),
                        request.description(),
                        parameters,
                        request.apiMethod(),
                        request.apiUrl(),
                        request.apiHeaders(),
                        request.apiBodyTemplate(),
                        request.apiResponsePath(),
                        request.apiTimeout(),
                        request.mcpEndpoint(),
                        request.mcpToolName(),
                        request.mcpServerType(),
                        request.mcpConfig(),
                        request.authType(),
                        request.authConfig(),
                        request.inputExample(),
                        request.outputExample(),
                        request.resultDescription(),
                        request.resultMetadata(),
                        request.retryCount(),
                        request.requireConfirmation(),
                        request.enabled(),
                        request.sortOrder(),
                        request.tags()
                )
        );

        return toDto(tool);
    }

    /**
     * 删除工具
     */
    @DeleteMapping("/{toolId}")
    public ResponseEntity<Void> deleteTool(@PathVariable UUID toolId) {
        toolService.deleteTool(toolId);
        return ResponseEntity.noContent().build();
    }

    // ==================== 工具执行 ====================

    /**
     * 执行工具
     */
    @PostMapping("/{toolId}/execute")
    public ToolExecutionResult executeTool(
            @PathVariable UUID toolId,
            @RequestBody ExecuteToolDto request,
            Authentication authentication) {
        UUID executedBy = getCurrentAgentId(authentication);

        return toolService.executeTool(
                toolId,
                request.params(),
                request.sessionId(),
                executedBy
        );
    }

    /**
     * 根据工具名称执行
     */
    @PostMapping("/execute/{toolName}")
    public ToolExecutionResult executeToolByName(
            @PathVariable String toolName,
            @RequestBody ExecuteToolDto request,
            Authentication authentication) {
        UUID executedBy = getCurrentAgentId(authentication);

        AiTool tool = toolService.getToolByName(toolName)
                .orElseThrow(() -> new IllegalArgumentException("工具不存在: " + toolName));

        return toolService.executeTool(
                tool.getId(),
                request.params(),
                request.sessionId(),
                executedBy
        );
    }

    /**
     * 测试工具（无需保存执行记录）
     */
    @PostMapping("/{toolId}/test")
    public ToolExecutionResult testTool(
            @PathVariable UUID toolId,
            @RequestBody ExecuteToolDto request) {
        return toolService.executeTool(toolId, request.params(), null, null);
    }

    // ==================== 执行记录 ====================

    /**
     * 获取工具执行记录
     */
    @GetMapping("/{toolId}/executions")
    public Page<ExecutionDto> getExecutions(
            @PathVariable UUID toolId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return toolService.getExecutions(toolId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toExecutionDto);
    }

    /**
     * 获取工具统计
     */
    @GetMapping("/{toolId}/stats")
    public ToolStats getToolStats(@PathVariable UUID toolId) {
        return toolService.getToolStats(toolId);
    }

    // ==================== 工具定义（用于 LLM） ====================

    /**
     * 获取工具定义列表
     */
    @GetMapping("/definitions")
    public List<ToolDefinition> getToolDefinitions() {
        return toolService.generateToolDefinitions();
    }

    /**
     * 获取 OpenAI Function Calling 格式
     */
    @GetMapping("/definitions/openai")
    public String getOpenAiFunctions() {
        return toolService.generateOpenAiFunctions();
    }

    /**
     * 获取工具的参数定义
     */
    @GetMapping("/{toolId}/parameters")
    public List<FieldDefinition> getToolParameters(@PathVariable UUID toolId) {
        return toolService.getToolParameters(toolId);
    }

    // ==================== DTOs ====================

    /**
     * 参数定义 DTO
     */
    public record ParameterDto(
            @NotBlank String name,
            String displayName,
            @NotNull FieldDefinition.FieldType type,
            boolean required,
            String description,
            List<String> enumValues,
            String defaultValue,
            List<ParameterDto> properties,  // 嵌套属性（当type为OBJECT时）
            ParameterDto items  // 数组元素定义（当type为ARRAY时）
    ) {}

    public record CreateToolDto(
            @NotBlank String name,
            String displayName,
            String description,
            @NotNull AiTool.ToolType toolType,
            List<ParameterDto> parameters,  // 内嵌参数定义
            String apiMethod,
            String apiUrl,
            String apiHeaders,
            String apiBodyTemplate,
            String apiResponsePath,
            Integer apiTimeout,
            String mcpEndpoint,
            String mcpToolName,
            String mcpServerType,
            String mcpConfig,
            AiTool.AuthType authType,
            String authConfig,
            String inputExample,
            String outputExample,
            String resultDescription,   // 返回结果描述
            String resultMetadata,      // 返回字段元数据 (JSON)
            Integer retryCount,
            Boolean requireConfirmation,
            Integer sortOrder,
            String tags
    ) {}

    public record UpdateToolDto(
            String displayName,
            String description,
            List<ParameterDto> parameters,  // 内嵌参数定义
            String apiMethod,
            String apiUrl,
            String apiHeaders,
            String apiBodyTemplate,
            String apiResponsePath,
            Integer apiTimeout,
            String mcpEndpoint,
            String mcpToolName,
            String mcpServerType,
            String mcpConfig,
            AiTool.AuthType authType,
            String authConfig,
            String inputExample,
            String outputExample,
            String resultDescription,   // 返回结果描述
            String resultMetadata,      // 返回字段元数据 (JSON)
            Integer retryCount,
            Boolean requireConfirmation,
            Boolean enabled,
            Integer sortOrder,
            String tags
    ) {}

    public record ExecuteToolDto(
            Map<String, Object> params,
            UUID sessionId
    ) {}

    public record ToolDto(
            String authConfig,
            UUID id,
            String name,
            String displayName,
            String description,
            AiTool.ToolType toolType,
            UUID schemaId,                       // 关联的 ExtractionSchema ID
            List<FieldDefinition> parameters,    // 参数定义
            String apiMethod,
            String apiUrl,
            Integer apiTimeout,
            String mcpEndpoint,
            String mcpToolName,
            AiTool.AuthType authType,
            String inputExample,
            String outputExample,
            String resultDescription,            // 返回结果描述
            String resultMetadata,               // 返回字段元数据 (JSON)
            Integer retryCount,
            Boolean requireConfirmation,
            Boolean enabled,
            Integer sortOrder,
            String tags,
            String createdAt,
            String apiBodyTemplate,
            String apiHeaders,
            String apiResponsePath
    ) {}

    public record ExecutionDto(
            UUID id,
            UUID toolId,
            String toolName,
            ToolExecution.ExecutionStatus status,
            String inputParams,
            String outputResult,
            String errorMessage,
            Long durationMs,
            Integer httpStatus,
            String triggerSource,
            String createdAt
    ) {}

    // ==================== Converters ====================

    private ToolDto toDto(AiTool tool) {
        // 获取参数定义（该方法内部会处理 Schema 的懒加载）
        List<FieldDefinition> parameters = toolService.getToolParameters(tool.getId());

        // Schema ID 可能为 null，需要安全访问
        UUID schemaId = null;
        try {
            if (tool.getSchema() != null) {
                schemaId = tool.getSchema().getId();
            }
        } catch (Exception e) {
            // 忽略懒加载异常，schemaId 保持为 null
        }

        return new ToolDto(
                tool.getAuthConfig(),
                tool.getId(),
                tool.getName(),
                tool.getDisplayName(),
                tool.getDescription(),
                tool.getToolType(),
                schemaId,
                parameters,
                tool.getApiMethod(),
                tool.getApiUrl(),
                tool.getApiTimeout(),
                tool.getMcpEndpoint(),
                tool.getMcpToolName(),
                tool.getAuthType(),
                tool.getInputExample(),
                tool.getOutputExample(),
                tool.getResultDescription(),
                tool.getResultMetadata(),
                tool.getRetryCount(),
                tool.getRequireConfirmation(),
                tool.getEnabled(),
                tool.getSortOrder(),
                tool.getTags(),
                tool.getCreatedAt() != null ? tool.getCreatedAt().toString() : null,
                tool.getApiBodyTemplate(),
                tool.getApiHeaders(),
                tool.getApiResponsePath()
        );
    }

    private ExecutionDto toExecutionDto(ToolExecution exec) {
        return new ExecutionDto(
                exec.getId(),
                exec.getTool().getId(),
                exec.getTool().getName(),
                exec.getStatus(),
                exec.getInputParams(),
                exec.getOutputResult(),
                exec.getErrorMessage(),
                exec.getDurationMs(),
                exec.getHttpStatus(),
                exec.getTriggerSource(),
                exec.getCreatedAt() != null ? exec.getCreatedAt().toString() : null
        );
    }

    private UUID getCurrentAgentId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AgentPrincipal) {
            return ((AgentPrincipal) authentication.getPrincipal()).getId();
        }
        return null;
    }

    /**
     * 递归转换 ParameterDto 到 ParameterDefinition（支持嵌套结构）
     */
    private ParameterDefinition convertParameterDto(ParameterDto dto) {
        // 递归转换 properties
        List<ParameterDefinition> properties = null;
        if (dto.properties() != null && !dto.properties().isEmpty()) {
            properties = dto.properties().stream()
                    .map(this::convertParameterDto)
                    .toList();
        }

        // 递归转换 items
        ParameterDefinition items = null;
        if (dto.items() != null) {
            items = convertParameterDto(dto.items());
        }

        return new ParameterDefinition(
                dto.name(),
                dto.displayName(),
                dto.type(),
                dto.required(),
                dto.description(),
                dto.enumValues(),
                dto.defaultValue(),
                properties,
                items
        );
    }
}

