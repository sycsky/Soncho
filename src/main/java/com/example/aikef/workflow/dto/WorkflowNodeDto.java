package com.example.aikef.workflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * ReactFlow 节点数据传输对象
 * 使用 @JsonIgnoreProperties 忽略 ReactFlow 内部字段（如 measured, selected, dragging 等）
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowNodeDto(
    /**
     * 节点ID
     */
    String id,
    
    /**
     * 节点类型
     * start - 开始节点
     * llm - LLM 调用节点
     * condition - 条件判断节点
     * reply - 回复节点
     * api - API 调用节点
     * knowledge - 知识库查询节点
     * intent - 意图识别节点
     * human_transfer - 转人工节点
     * delay - 延迟节点
     * end - 结束节点
     */
    String type,
    
    /**
     * 节点数据
     */
    NodeData data,
    
    /**
     * 节点位置
     */
    Position position
) {
    /**
     * 节点数据
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NodeData(
        /**
         * 节点显示标签
         */
        String label,
        
        /**
         * 节点配置
         */
        JsonNode config
    ) {}
    
    /**
     * 节点位置
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Position(
        double x,
        double y
    ) {}
}

