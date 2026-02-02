package com.example.aikef.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 保存 LLM 模型请求
 */
public record SaveLlmModelRequest(
    @NotBlank(message = "模型名称不能为空")
    String name,
    
    @NotBlank(message = "模型编码不能为空")
    String code,
    
    @NotBlank(message = "提供商不能为空")
    String provider,
    
    @NotBlank(message = "模型标识不能为空")
    String modelName,
    
    /**
     * 模型类型: CHAT（聊天模型）, EMBEDDING（嵌入模型）
     * 默认为 CHAT
     */
    String modelType,
    
    String baseUrl,
    
    String apiKey,
    
    String azureDeploymentName,
    
    Double defaultTemperature,
    
    Integer defaultMaxTokens,
    
    Integer contextWindow,
    
    Double inputPricePer1k,
    
    Double outputPricePer1k,
    
    Boolean supportsFunctions,
    
    Boolean supportsVision,
    
    Boolean enabled,
    
    /**
     * 是否设为默认模型
     * 如果设置为 true，会自动取消之前的默认模型
     * 注意：EMBEDDING 类型的模型不能设为默认
     */
    Boolean isDefault,
    
    Integer sortOrder,
    
    String description,
    
    String extraConfig,
    
    Boolean statusExplanation
) {}

