package com.example.aikef.workflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * ReactFlow 边数据传输对象
 * 使用 @JsonIgnoreProperties 忽略 ReactFlow 内部字段（如 selected, animated, style 等）
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowEdgeDto(
    /**
     * 边ID
     */
    String id,
    
    /**
     * 源节点ID
     */
    String source,
    
    /**
     * 目标节点ID
     */
    String target,
    
    /**
     * 源节点输出句柄（用于条件分支/意图路由）
     * 例如: "true", "false", 或意图ID如 "c1764337030732"
     */
    String sourceHandle,
    
    /**
     * 目标节点输入句柄
     */
    String targetHandle,
    
    /**
     * 边的标签（可选）
     */
    String label
) {}

