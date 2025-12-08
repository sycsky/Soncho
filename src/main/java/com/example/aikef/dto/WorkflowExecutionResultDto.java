package com.example.aikef.dto;

import com.example.aikef.workflow.context.WorkflowContext;

import java.util.List;

/**
 * 工作流执行结果 DTO
 */
public record WorkflowExecutionResultDto(
    /**
     * 执行是否成功
     */
    boolean success,
    
    /**
     * AI 回复内容
     */
    String reply,
    
    /**
     * 错误信息（如果失败）
     */
    String errorMessage,
    
    /**
     * 是否需要转人工
     */
    Boolean needHumanTransfer,
    
    /**
     * 节点执行详情（用于调试）
     */
    List<WorkflowContext.NodeExecutionDetail> nodeDetails
) {}

