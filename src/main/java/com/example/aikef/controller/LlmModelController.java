package com.example.aikef.controller;

import com.example.aikef.dto.LlmModelDto;
import com.example.aikef.dto.request.SaveLlmModelRequest;
import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.llm.LlmModelService;
import com.example.aikef.model.LlmModel;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * LLM 模型管理 API 控制器
 */
@RestController
@RequestMapping("/api/v1/llm-models")
public class LlmModelController {

    private final LlmModelService llmModelService;
    private final LangChainChatService langChainChatService;

    public LlmModelController(LlmModelService llmModelService,
                             LangChainChatService langChainChatService) {
        this.llmModelService = llmModelService;
        this.langChainChatService = langChainChatService;
    }

    /**
     * 获取所有模型
     */
    @GetMapping
    public List<LlmModelDto> getAllModels() {
        return llmModelService.getAllModels().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有启用的模型
     */
    @GetMapping("/enabled")
    public List<LlmModelDto> getEnabledModels() {
        return llmModelService.getEnabledModels().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 获取模型详情
     */
    @GetMapping("/{modelId}")
    public LlmModelDto getModel(@PathVariable UUID modelId) {
        return toDto(llmModelService.getModel(modelId));
    }

    /**
     * 创建模型
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LlmModelDto createModel(@Valid @RequestBody SaveLlmModelRequest request) {
        LlmModel model = llmModelService.createModel(request);
        return toDto(model);
    }

    /**
     * 更新模型
     */
    @PutMapping("/{modelId}")
    public LlmModelDto updateModel(
            @PathVariable UUID modelId,
            @Valid @RequestBody SaveLlmModelRequest request) {
        LlmModel model = llmModelService.updateModel(modelId, request);
        // 清除缓存
        langChainChatService.clearModelCache(modelId);
        return toDto(model);
    }

    /**
     * 删除模型
     */
    @DeleteMapping("/{modelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteModel(@PathVariable UUID modelId) {
        langChainChatService.clearModelCache(modelId);
        llmModelService.deleteModel(modelId);
    }

    /**
     * 启用/禁用模型
     */
    @PatchMapping("/{modelId}/toggle")
    public LlmModelDto toggleModel(
            @PathVariable UUID modelId,
            @RequestParam boolean enabled) {
        LlmModel model = llmModelService.toggleModel(modelId, enabled);
        if (!enabled) {
            langChainChatService.clearModelCache(modelId);
        }
        return toDto(model);
    }

    /**
     * 设置默认模型
     */
    @PostMapping("/{modelId}/set-default")
    public LlmModelDto setDefaultModel(@PathVariable UUID modelId) {
        return toDto(llmModelService.setDefaultModel(modelId));
    }

    /**
     * 获取可用的提供商列表
     */
    @GetMapping("/providers")
    public List<LlmModelService.ProviderInfo> getProviders() {
        return llmModelService.getAvailableProviders();
    }

    /**
     * 测试模型连接
     */
    @PostMapping("/{modelId}/test")
    public TestResult testModel(@PathVariable UUID modelId) {
        try {
            LangChainChatService.LlmChatResponse response = langChainChatService.chat(
                    modelId,
                    "你是一个测试助手。",
                    "你好，请回复 测试成功",
                    null,
                    1.0,
                    50
            );
            
            if (response.success()) {
                return new TestResult(true, response.reply(), response.durationMs(), null);
            } else {
                return new TestResult(false, null, 0, response.errorMessage());
            }
        } catch (Exception e) {
            return new TestResult(false, null, 0, e.getMessage());
        }
    }

    /**
     * 清除所有模型缓存
     */
    @PostMapping("/clear-cache")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCache() {
        langChainChatService.clearAllModelCache();
    }

    // ==================== 辅助方法 ====================

    private LlmModelDto toDto(LlmModel model) {
        return new LlmModelDto(
                model.getId(),
                model.getName(),
                model.getCode(),
                model.getProvider(),
                model.getModelName(),
                model.getBaseUrl(),
                model.getAzureDeploymentName(),
                model.getDefaultTemperature(),
                model.getDefaultMaxTokens(),
                model.getContextWindow(),
                model.getInputPricePer1k(),
                model.getOutputPricePer1k(),
                model.getSupportsFunctions(),
                model.getSupportsVision(),
                model.getEnabled(),
                model.getIsDefault(),
                model.getSortOrder(),
                model.getDescription(),
                model.getCreatedAt(),
                model.getUpdatedAt(),
                model.getModelType().name(),
                model.getStatusExplanation()
        );
    }

    /**
     * 测试结果
     */
    public record TestResult(
            boolean success,
            String response,
            long durationMs,
            String error
    ) {}
}

