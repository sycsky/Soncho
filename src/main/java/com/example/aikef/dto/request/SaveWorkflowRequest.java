package com.example.aikef.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

/**
 * 保存工作流请求
 * 支持创建空模板（nodesJson 和 edgesJson 可为空或空数组）
 */
public record SaveWorkflowRequest(
    @NotBlank(message = "工作流名称不能为空")
    String name,
    
    String description,
    
    /**
     * 节点数据 JSON（可选，空模板时可为 null 或 "[]"）
     */
    String nodesJson,
    
    /**
     * 边数据 JSON（可选，空模板时可为 null 或 "[]"）
     */
    String edgesJson,
    
    /**
     * 触发条件类型
     * ALL - 所有会话
     * CATEGORY - 指定分类
     * KEYWORD - 关键词匹配
     */
    String triggerType,
    
    /**
     * 触发条件配置 (JSON)
     */
    String triggerConfig,
    
    /**
     * 绑定的会话分类ID列表
     * 一个工作流可以绑定多个分类，一个分类只能绑定一个工作流
     */
    List<UUID> categoryIds
) {}

