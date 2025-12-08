package com.example.aikef.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * LLM 模型配置实体
 * 存储可用的大语言模型配置信息
 */
@Entity
@Table(name = "llm_models")
public class LlmModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 模型名称（显示用）
     */
    @Column(nullable = false)
    private String name;

    /**
     * 模型编码（唯一标识）
     */
    @Column(nullable = false, unique = true)
    private String code;

    /**
     * 模型提供商
     * OPENAI, AZURE_OPENAI, OLLAMA, ZHIPU, CUSTOM
     */
    @Column(nullable = false)
    private String provider;

    /**
     * 模型类型: CHAT, EMBEDDING
     */
    @Column(name = "model_type", length = 20)
    @Enumerated(EnumType.STRING)
    private ModelType modelType = ModelType.CHAT;

    /**
     * 模型标识（API 中使用的模型名）
     * 如: gpt-4, gpt-3.5-turbo, qwen-plus, glm-4
     */
    @Column(name = "model_name", nullable = false)
    private String modelName;

    /**
     * API Base URL
     */
    @Column(name = "base_url")
    private String baseUrl;

    /**
     * API Key（加密存储）
     */
    @Column(name = "api_key", columnDefinition = "TEXT")
    private String apiKey;

    /**
     * Azure 部署名称（Azure OpenAI 专用）
     */
    @Column(name = "azure_deployment_name")
    private String azureDeploymentName;

    /**
     * 默认温度参数
     */
    @Column(name = "default_temperature")
    private Double defaultTemperature = 0.7;

    /**
     * 默认最大 Token 数
     */
    @Column(name = "default_max_tokens")
    private Integer defaultMaxTokens = 2000;

    /**
     * 上下文窗口大小
     */
    @Column(name = "context_window")
    private Integer contextWindow = 4096;

    /**
     * 每千 Token 输入价格（美元）
     */
    @Column(name = "input_price_per_1k")
    private Double inputPricePer1k;

    /**
     * 每千 Token 输出价格（美元）
     */
    @Column(name = "output_price_per_1k")
    private Double outputPricePer1k;

    /**
     * 是否支持函数调用
     */
    @Column(name = "supports_functions")
    private Boolean supportsFunctions = false;

    /**
     * 是否支持视觉（图片输入）
     */
    @Column(name = "supports_vision")
    private Boolean supportsVision = false;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    private Boolean enabled = true;

    /**
     * 是否为默认模型
     */
    @Column(name = "is_default")
    private Boolean isDefault = false;

    /**
     * 排序顺序
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /**
     * 描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 额外配置（JSON）
     */
    @Column(name = "extra_config", columnDefinition = "TEXT")
    private String extraConfig;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getAzureDeploymentName() {
        return azureDeploymentName;
    }

    public void setAzureDeploymentName(String azureDeploymentName) {
        this.azureDeploymentName = azureDeploymentName;
    }

    public Double getDefaultTemperature() {
        return defaultTemperature;
    }

    public void setDefaultTemperature(Double defaultTemperature) {
        this.defaultTemperature = defaultTemperature;
    }

    public Integer getDefaultMaxTokens() {
        return defaultMaxTokens;
    }

    public void setDefaultMaxTokens(Integer defaultMaxTokens) {
        this.defaultMaxTokens = defaultMaxTokens;
    }

    public Integer getContextWindow() {
        return contextWindow;
    }

    public void setContextWindow(Integer contextWindow) {
        this.contextWindow = contextWindow;
    }

    public Double getInputPricePer1k() {
        return inputPricePer1k;
    }

    public void setInputPricePer1k(Double inputPricePer1k) {
        this.inputPricePer1k = inputPricePer1k;
    }

    public Double getOutputPricePer1k() {
        return outputPricePer1k;
    }

    public void setOutputPricePer1k(Double outputPricePer1k) {
        this.outputPricePer1k = outputPricePer1k;
    }

    public Boolean getSupportsFunctions() {
        return supportsFunctions;
    }

    public void setSupportsFunctions(Boolean supportsFunctions) {
        this.supportsFunctions = supportsFunctions;
    }

    public Boolean getSupportsVision() {
        return supportsVision;
    }

    public void setSupportsVision(Boolean supportsVision) {
        this.supportsVision = supportsVision;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExtraConfig() {
        return extraConfig;
    }

    public void setExtraConfig(String extraConfig) {
        this.extraConfig = extraConfig;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ModelType getModelType() {
        return modelType;
    }

    public void setModelType(ModelType modelType) {
        this.modelType = modelType;
    }

    /**
     * 模型类型枚举
     */
    public enum ModelType {
        CHAT,      // 聊天/对话模型
        EMBEDDING  // 嵌入/向量化模型
    }
}

