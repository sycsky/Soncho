package com.example.aikef.llm;

import com.example.aikef.dto.LlmModelDto;
import com.example.aikef.dto.request.SaveLlmModelRequest;
import com.example.aikef.model.LlmModel;
import com.example.aikef.model.enums.LlmProvider;
import com.example.aikef.repository.LlmModelRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * LLM 模型管理服务
 */
@Service
@Transactional(readOnly = true)
public class LlmModelService {

    private static final Logger log = LoggerFactory.getLogger(LlmModelService.class);

    private final LlmModelRepository llmModelRepository;

    public LlmModelService(LlmModelRepository llmModelRepository) {
        this.llmModelRepository = llmModelRepository;
    }

    // ==================== CRUD 操作 ====================

    /**
     * 创建模型配置
     */
    @Transactional
    public LlmModel createModel(SaveLlmModelRequest request) {
        // 检查编码唯一性
        if (llmModelRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("模型编码已存在: " + request.code());
        }

        LlmModel model = new LlmModel();
        updateModelFromRequest(model, request);
        
        return llmModelRepository.save(model);
    }

    /**
     * 更新模型配置
     */
    @Transactional
    public LlmModel updateModel(UUID modelId, SaveLlmModelRequest request) {
        LlmModel model = llmModelRepository.findById(modelId)
                .orElseThrow(() -> new EntityNotFoundException("模型不存在"));

        // 检查编码是否与其他模型冲突
        llmModelRepository.findByCode(request.code())
                .filter(m -> !m.getId().equals(modelId))
                .ifPresent(m -> {
                    throw new IllegalArgumentException("模型编码已存在: " + request.code());
                });

        updateModelFromRequest(model, request);
        return llmModelRepository.save(model);
    }

    /**
     * 删除模型配置
     */
    @Transactional
    public void deleteModel(UUID modelId) {
        if (!llmModelRepository.existsById(modelId)) {
            throw new EntityNotFoundException("模型不存在");
        }
        llmModelRepository.deleteById(modelId);
    }

    /**
     * 获取模型详情
     */
    public LlmModel getModel(UUID modelId) {
        return llmModelRepository.findById(modelId)
                .orElseThrow(() -> new EntityNotFoundException("模型不存在"));
    }

    /**
     * 根据编码获取模型
     */
    public LlmModel getModelByCode(String code) {
        return llmModelRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("模型不存在: " + code));
    }

    /**
     * 获取所有模型
     */
    public List<LlmModel> getAllModels() {
        return llmModelRepository.findAllByOrderBySortOrderAsc();
    }

    /**
     * 获取所有启用的模型
     */
    public List<LlmModel> getEnabledModels() {
        return llmModelRepository.findByEnabledTrueOrderBySortOrderAsc();
    }

    /**
     * 根据提供商获取模型
     */
    public List<LlmModel> getModelsByProvider(String provider) {
        return llmModelRepository.findByProviderAndEnabledTrueOrderBySortOrderAsc(provider);
    }

    /**
     * 获取默认模型
     */
    public Optional<LlmModel> getDefaultModel() {
        return llmModelRepository.findByIsDefaultTrueAndEnabledTrue();
    }

    /**
     * 启用/禁用模型
     */
    @Transactional
    public LlmModel toggleModel(UUID modelId, boolean enabled) {
        LlmModel model = getModel(modelId);
        model.setEnabled(enabled);
        return llmModelRepository.save(model);
    }

    /**
     * 设置默认模型
     * 注意：默认模型只能是 CHAT 类型，不能是 EMBEDDING 类型
     */
    @Transactional
    public LlmModel setDefaultModel(UUID modelId) {
        LlmModel model = getModel(modelId);
        
        // 检查模型类型，EMBEDDING 类型不能设为默认模型
        if (model.getModelType() == LlmModel.ModelType.EMBEDDING) {
            throw new IllegalArgumentException("EMBEDDING 类型的模型不能设为默认模型，只有 CHAT 类型可以");
        }
        
        // 取消原有默认模型
        llmModelRepository.findByIsDefaultTrueAndEnabledTrue()
                .ifPresent(m -> {
                    m.setIsDefault(false);
                    llmModelRepository.save(m);
                });

        model.setIsDefault(true);
        model.setEnabled(true);
        return llmModelRepository.save(model);
    }

    /**
     * 获取可用的提供商列表
     */
    public List<ProviderInfo> getAvailableProviders() {
        return List.of(
                new ProviderInfo(LlmProvider.OPENAI.name(), LlmProvider.OPENAI.getDisplayName(), LlmProvider.OPENAI.getDefaultBaseUrl()),
                new ProviderInfo(LlmProvider.AZURE_OPENAI.name(), LlmProvider.AZURE_OPENAI.getDisplayName(), null),
                new ProviderInfo(LlmProvider.OLLAMA.name(), LlmProvider.OLLAMA.getDisplayName(), LlmProvider.OLLAMA.getDefaultBaseUrl()),
                new ProviderInfo(LlmProvider.ZHIPU.name(), LlmProvider.ZHIPU.getDisplayName(), LlmProvider.ZHIPU.getDefaultBaseUrl()),
                new ProviderInfo(LlmProvider.DASHSCOPE.name(), LlmProvider.DASHSCOPE.getDisplayName(), LlmProvider.DASHSCOPE.getDefaultBaseUrl()),
                new ProviderInfo(LlmProvider.MOONSHOT.name(), LlmProvider.MOONSHOT.getDisplayName(), LlmProvider.MOONSHOT.getDefaultBaseUrl()),
                new ProviderInfo(LlmProvider.DEEPSEEK.name(), LlmProvider.DEEPSEEK.getDisplayName(), LlmProvider.DEEPSEEK.getDefaultBaseUrl()),
                new ProviderInfo(LlmProvider.CUSTOM.name(), LlmProvider.CUSTOM.getDisplayName(), null)
        );
    }

    // ==================== 辅助方法 ====================

    private void updateModelFromRequest(LlmModel model, SaveLlmModelRequest request) {
        model.setName(request.name());
        model.setCode(request.code());
        model.setProvider(request.provider());
        model.setModelName(request.modelName());
        model.setBaseUrl(request.baseUrl());
        model.setApiKey(request.apiKey());
        model.setAzureDeploymentName(request.azureDeploymentName());
        
        // 设置模型类型
        if (request.modelType() != null && !request.modelType().isBlank()) {
            try {
                model.setModelType(LlmModel.ModelType.valueOf(request.modelType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("无效的模型类型: " + request.modelType() + "，可选值: CHAT, EMBEDDING");
            }
        }
        
        if (request.defaultTemperature() != null) {
            model.setDefaultTemperature(request.defaultTemperature());
        }
        if (request.defaultMaxTokens() != null) {
            model.setDefaultMaxTokens(request.defaultMaxTokens());
        }
        if (request.contextWindow() != null) {
            model.setContextWindow(request.contextWindow());
        }
        if (request.inputPricePer1k() != null) {
            model.setInputPricePer1k(request.inputPricePer1k());
        }
        if (request.outputPricePer1k() != null) {
            model.setOutputPricePer1k(request.outputPricePer1k());
        }
        if (request.supportsFunctions() != null) {
            model.setSupportsFunctions(request.supportsFunctions());
        }
        if (request.supportsVision() != null) {
            model.setSupportsVision(request.supportsVision());
        }
        if (request.enabled() != null) {
            model.setEnabled(request.enabled());
        }
        
        // 处理默认模型设置
        if (request.isDefault() != null && request.isDefault()) {
            // 检查模型类型，EMBEDDING 类型不能设为默认模型
            LlmModel.ModelType modelType = model.getModelType();
            if (modelType == LlmModel.ModelType.EMBEDDING) {
                throw new IllegalArgumentException("EMBEDDING 类型的模型不能设为默认模型，只有 CHAT 类型可以");
            }
            
            // 取消原有默认模型
            llmModelRepository.findByIsDefaultTrueAndEnabledTrue()
                    .filter(m -> !m.getId().equals(model.getId()))  // 排除当前模型
                    .ifPresent(m -> {
                        m.setIsDefault(false);
                        llmModelRepository.save(m);
                        log.info("取消原默认模型: id={}, name={}", m.getId(), m.getName());
                    });
            
            model.setIsDefault(true);
            model.setEnabled(true);  // 默认模型必须启用
            log.info("设置新默认模型: id={}, name={}", model.getId(), model.getName());
        } else if (request.isDefault() != null && !request.isDefault()) {
            model.setIsDefault(false);
        }
        
        if (request.sortOrder() != null) {
            model.setSortOrder(request.sortOrder());
        }
        model.setDescription(request.description());
        model.setExtraConfig(request.extraConfig());
    }

    /**
     * 提供商信息
     */
    public record ProviderInfo(String code, String name, String defaultBaseUrl) {}
}

