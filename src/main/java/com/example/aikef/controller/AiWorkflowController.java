package com.example.aikef.controller;

import com.example.aikef.dto.AiWorkflowDto;
import com.example.aikef.dto.WorkflowExecutionResultDto;
import com.example.aikef.dto.request.ExecuteWorkflowRequest;
import com.example.aikef.dto.request.SaveWorkflowRequest;
import com.example.aikef.model.AiWorkflow;
import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.workflow.context.WorkflowContext;
import com.example.aikef.workflow.converter.ReactFlowToLiteflowConverter;
import com.example.aikef.workflow.service.AiWorkflowService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI 工作流 API 控制器
 */
@RestController
@RequestMapping("/api/v1/ai-workflows")
public class AiWorkflowController {

    private final AiWorkflowService workflowService;
    private final ReactFlowToLiteflowConverter converter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiWorkflowController(AiWorkflowService workflowService,
                                ReactFlowToLiteflowConverter converter) {
        this.workflowService = workflowService;
        this.converter = converter;
    }

    /**
     * 获取所有工作流
     */
    @GetMapping
    public List<AiWorkflowDto> getAllWorkflows() {
        return workflowService.getAllWorkflows().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 获取工作流详情
     */
    @GetMapping("/{workflowId}")
    public AiWorkflowDto getWorkflow(@PathVariable UUID workflowId) {
        return toDto(workflowService.getWorkflow(workflowId));
    }

    /**
     * 创建工作流
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AiWorkflowDto createWorkflow(
            @Valid @RequestBody SaveWorkflowRequest request,
            Authentication authentication) {
        UUID agentId = getAgentId(authentication);
        AiWorkflow workflow = workflowService.createWorkflow(request, agentId);
        return toDto(workflow);
    }

    /**
     * 更新工作流
     */
    @PutMapping("/{workflowId}")
    public AiWorkflowDto updateWorkflow(
            @PathVariable UUID workflowId,
            @Valid @RequestBody SaveWorkflowRequest request) {
        AiWorkflow workflow = workflowService.updateWorkflow(workflowId, request);
        return toDto(workflow);
    }

    /**
     * 删除工作流
     */
    @DeleteMapping("/{workflowId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkflow(@PathVariable UUID workflowId) {
        workflowService.deleteWorkflow(workflowId);
    }

    /**
     * 启用/禁用工作流
     */
    @PatchMapping("/{workflowId}/toggle")
    public AiWorkflowDto toggleWorkflow(
            @PathVariable UUID workflowId,
            @RequestParam boolean enabled) {
        return toDto(workflowService.toggleWorkflow(workflowId, enabled));
    }

    /**
     * 设置为默认工作流
     */
    @PostMapping("/{workflowId}/set-default")
    public AiWorkflowDto setDefaultWorkflow(@PathVariable UUID workflowId) {
        return toDto(workflowService.setDefaultWorkflow(workflowId));
    }

    /**
     * 执行工作流
     */
    @PostMapping("/{workflowId}/execute")
    public WorkflowExecutionResultDto executeWorkflow(
            @PathVariable UUID workflowId,
            @Valid @RequestBody ExecuteWorkflowRequest request) {
        
        AiWorkflowService.WorkflowExecutionResult result = workflowService.executeWorkflow(
                workflowId,
                request.sessionId(),
                request.userMessage(),
                request.variables()
        );

        return toResultDto(result);
    }

    /**
     * 测试执行工作流（不保存日志）
     */
    @PostMapping("/{workflowId}/test")
    public WorkflowExecutionResultDto testWorkflow(
            @PathVariable UUID workflowId,
            @Valid @RequestBody ExecuteWorkflowRequest request) {
        
        AiWorkflowService.WorkflowExecutionResult result = workflowService.testWorkflow(
                workflowId,
                request.userMessage(),
                request.variables()
        );

        return toResultDto(result);
    }

    /**
     * 为会话执行工作流（自动匹配）
     */
    @PostMapping("/execute-for-session")
    public WorkflowExecutionResultDto executeForSession(
            @RequestParam String sessionId,
            @RequestParam String userMessage) {
        
        AiWorkflowService.WorkflowExecutionResult result = 
                workflowService.executeForSession(UUID.fromString(sessionId), userMessage);

        return toResultDto(result);
    }

    /**
     * 验证工作流结构
     */
    @PostMapping("/validate")
    public ValidationResponse validateWorkflow(@RequestBody ValidateWorkflowRequest request) {
        var result = converter.validate(request.nodesJson(), request.edgesJson());
        return new ValidationResponse(result.valid(), result.errors());
    }

    /**
     * 预览 LiteFlow EL 表达式
     */
    @PostMapping("/preview-el")
    public PreviewElResponse previewEl(@RequestBody ValidateWorkflowRequest request) {
        try {
            String el = converter.convert(request.nodesJson(), request.edgesJson());
            return new PreviewElResponse(true, el, null);
        } catch (Exception e) {
            return new PreviewElResponse(false, null, e.getMessage());
        }
    }

    // ==================== 分类绑定 API ====================

    /**
     * 获取当前工作流可绑定的分类列表
     * 返回：未被其他工作流绑定的分类 + 当前工作流已绑定的分类
     */
    @GetMapping("/{workflowId}/available-categories")
    public List<CategoryDto> getAvailableCategoriesForWorkflow(@PathVariable UUID workflowId) {
        return workflowService.getAvailableCategoriesForWorkflow(workflowId).stream()
                .map(this::toCategoryDto)
                .toList();
    }

    /**
     * 获取所有可绑定的分类列表（用于新建工作流）
     * 返回：未被任何工作流绑定的分类
     */
    @GetMapping("/available-categories")
    public List<CategoryDto> getAvailableCategories() {
        return workflowService.getAvailableCategories().stream()
                .map(this::toCategoryDto)
                .toList();
    }

    /**
     * 获取工作流绑定的分类ID列表
     */
    @GetMapping("/{workflowId}/categories")
    public List<UUID> getWorkflowCategories(@PathVariable UUID workflowId) {
        return workflowService.getWorkflowCategoryIds(workflowId);
    }

    /**
     * 绑定工作流到分类
     */
    @PutMapping("/{workflowId}/categories")
    public void bindWorkflowToCategories(
            @PathVariable UUID workflowId,
            @RequestBody BindCategoriesRequest request) {
        workflowService.bindWorkflowToCategories(workflowId, request.categoryIds());
    }

    /**
     * 根据分类ID查找绑定的工作流
     */
    @GetMapping("/by-category/{categoryId}")
    public AiWorkflowDto getWorkflowByCategory(@PathVariable UUID categoryId) {
        return workflowService.findWorkflowByCategoryId(categoryId)
                .map(this::toDto)
                .orElse(null);
    }

    private CategoryDto toCategoryDto(com.example.aikef.model.SessionCategory category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getIcon(),
                category.getColor(),
                category.getSortOrder()
        );
    }

    // ==================== 辅助方法 ====================

    private UUID getAgentId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AgentPrincipal) {
            return ((AgentPrincipal) authentication.getPrincipal()).getId();
        }
        return null;
    }

    private AiWorkflowDto toDto(AiWorkflow workflow) {
        String agentName = null;
        UUID agentId = null;
        if (workflow.getCreatedByAgent() != null) {
            agentId = workflow.getCreatedByAgent().getId();
            agentName = workflow.getCreatedByAgent().getName();
        }

        // 获取绑定的分类
        List<UUID> categoryIds = workflowService.getWorkflowCategoryIds(workflow.getId());
        List<AiWorkflowDto.CategoryInfo> categories = workflowService.getWorkflowCategories(workflow.getId())
                .stream()
                .map(c -> new AiWorkflowDto.CategoryInfo(c.getId(), c.getName(), c.getColor(), c.getIcon()))
                .toList();

        return new AiWorkflowDto(
                workflow.getId(),
                workflow.getName(),
                workflow.getDescription(),
                workflow.getNodesJson(),
                workflow.getEdgesJson(),
                workflow.getLiteflowEl(),
                workflow.getVersion(),
                workflow.getEnabled(),
                workflow.getIsDefault(),
                agentId,
                agentName,
                workflow.getTriggerType(),
                workflow.getTriggerConfig(),
                categoryIds,
                categories,
                workflow.getCreatedAt(),
                workflow.getUpdatedAt()
        );
    }

    private WorkflowExecutionResultDto toResultDto(AiWorkflowService.WorkflowExecutionResult result) {
        List<WorkflowContext.NodeExecutionDetail> nodeDetails = null;
        if (result.nodeDetailsJson() != null) {
            try {
                nodeDetails = objectMapper.readValue(
                        result.nodeDetailsJson(),
                        new TypeReference<List<WorkflowContext.NodeExecutionDetail>>() {}
                );
            } catch (Exception e) {
                // 忽略解析错误
            }
        }

        return new WorkflowExecutionResultDto(
                result.success(),
                result.reply(),
                result.errorMessage(),
                result.needHumanTransfer(),
                nodeDetails
        );
    }

    // ==================== 请求/响应记录 ====================

    public record ValidateWorkflowRequest(String nodesJson, String edgesJson) {}
    
    public record ValidationResponse(boolean valid, List<String> errors) {}
    
    public record PreviewElResponse(boolean success, String el, String error) {}

    public record BindCategoriesRequest(List<UUID> categoryIds) {}

    public record CategoryDto(
            UUID id,
            String name,
            String description,
            String icon,
            String color,
            Integer sortOrder
    ) {}
}

