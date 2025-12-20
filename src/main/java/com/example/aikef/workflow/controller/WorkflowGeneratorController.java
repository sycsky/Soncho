package com.example.aikef.workflow.controller;

import com.example.aikef.workflow.service.WorkflowGeneratorService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 工作流生成控制器
 */
@RestController
@RequestMapping("/api/v1/workflow-generator")
public class WorkflowGeneratorController {

    private final WorkflowGeneratorService generatorService;

    public WorkflowGeneratorController(WorkflowGeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    /**
     * 根据用户提示生成或修改工作流
     * 
     * POST /api/v1/workflow-generator/generate
     * 
     * Request Body:
     * {
     *   "prompt": "用户提示",
     *   "modelId": "模型UUID（可选）",
     *   "existingNodesJson": "现有节点JSON（可选，用于修改现有工作流）",
     *   "existingEdgesJson": "现有边JSON（可选，用于修改现有工作流）"
     * }
     * 
     * Response:
     * {
     *   "nodesJson": "节点JSON字符串",
     *   "edgesJson": "边JSON字符串",
     *   "fullJson": "完整JSON字符串"
     * }
     */
    @PostMapping("/generate")
    public WorkflowGeneratorService.GeneratedWorkflow generateWorkflow(
            @RequestBody GenerateWorkflowRequest request) {
        return generatorService.generateWorkflow(
                request.prompt(),
                request.modelId(),
                request.existingNodesJson(),
                request.existingEdgesJson()
        );
    }

    /**
     * 生成工作流请求
     */
    public record GenerateWorkflowRequest(
            String prompt,
            UUID modelId,
            String existingNodesJson,
            String existingEdgesJson
    ) {}
}

