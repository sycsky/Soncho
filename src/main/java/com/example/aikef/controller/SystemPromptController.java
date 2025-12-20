package com.example.aikef.controller;

import com.example.aikef.service.SystemPromptEnhancementService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * SystemPrompt 美化控制器
 */
@RestController
@RequestMapping("/api/system-prompt")
public class SystemPromptController {

    @Resource
    private SystemPromptEnhancementService enhancementService;

    /**
     * 美化 systemPrompt
     * 
     * @param request 美化请求
     * @return 美化后的 systemPrompt
     */
    @PostMapping("/enhance")
    public EnhanceSystemPromptResponse enhanceSystemPrompt(@RequestBody EnhanceSystemPromptRequest request) {
        String enhancedPrompt = enhancementService.enhanceSystemPrompt(
                request.nodeType(),
                request.toolIds(),
                request.userInput()
        );
        
        return new EnhanceSystemPromptResponse(enhancedPrompt);
    }

    /**
     * 美化 systemPrompt 请求
     */
    public record EnhanceSystemPromptRequest(
            String nodeType,           // 节点类型（如：llm, intent, parameter_extraction 等）
            List<UUID> toolIds,         // 节点使用的工具ID列表（可选）
            String userInput            // 用户输入（可选，用于理解上下文）
    ) {}

    /**
     * 美化 systemPrompt 响应
     */
    public record EnhanceSystemPromptResponse(
            String systemPrompt        // 美化后的 systemPrompt
    ) {}
}

