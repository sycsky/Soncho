package com.example.aikef.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * LLM 模型 DTO
 */
public record LlmModelDto(
    UUID id,
    String name,
    String code,
    String provider,
    String modelName,
    String baseUrl,
    // 注意：apiKey 不返回给前端
    String azureDeploymentName,
    Double defaultTemperature,
    Integer defaultMaxTokens,
    Integer contextWindow,
    Double inputPricePer1k,
    Double outputPricePer1k,
    Boolean supportsFunctions,
    Boolean supportsVision,
    Boolean enabled,
    Boolean isDefault,
    Integer sortOrder,
    String description,
    Instant createdAt,
    Instant updatedAt,
    String modelType,
    Boolean statusExplanation
) {}

