package com.example.aikef.service;

import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.llm.LlmModelService;
import com.example.aikef.model.LlmModel;
import com.example.aikef.tool.model.AiTool;
import com.example.aikef.tool.repository.AiToolRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SystemPrompt 美化服务
 * 根据节点类型、工具和用户输入来丰富 systemPrompt
 */
@Service
public class SystemPromptEnhancementService {

    private static final Logger log = LoggerFactory.getLogger(SystemPromptEnhancementService.class);

    @Resource
    private LangChainChatService langChainChatService;

    @Resource
    private LlmModelService llmModelService;

    @Resource
    private AiToolRepository toolRepository;

    /**
     * 美化 systemPrompt
     * 
     * @param nodeType 节点类型（如：llm, intent, parameter_extraction 等）
     * @param toolIds 节点使用的工具ID列表（可选）
     * @param userInput 用户输入（可选，用于理解上下文）
     * @return 美化后的 systemPrompt
     */
    public String enhanceSystemPrompt(String nodeType, List<UUID> toolIds, String userInput) {
        try {
            // 获取默认模型
            LlmModel defaultModel = llmModelService.getDefaultModel()
                    .orElseThrow(() -> new IllegalStateException("未配置默认模型"));

            // 构建工具信息
            String toolsInfo = buildToolsInfo(toolIds);

            // 构建系统提示词
            String systemPrompt = buildEnhancementSystemPrompt(nodeType, toolsInfo);

            // 构建用户提示词
            String userPrompt = buildEnhancementUserPrompt(nodeType, toolsInfo, userInput);

            // 使用 LLM 生成美化的 systemPrompt
            var response = langChainChatService.chat(
                    defaultModel.getId(),
                    systemPrompt,
                    userPrompt,
                    new ArrayList<>(),
                    0.7, // 使用中等温度，保证创造性和准确性平衡
                    null,
                    null
            );

            if (response == null || !response.success() || response.reply() == null) {
                log.warn("SystemPrompt 美化失败，返回原始提示词: {}", response != null ? response.errorMessage() : "null");
                return buildDefaultSystemPrompt(nodeType, toolsInfo);
            }

            String enhancedPrompt = response.reply().trim();
            
            // 清理可能的 markdown 代码块标记
            if (enhancedPrompt.startsWith("```")) {
                int start = enhancedPrompt.indexOf("\n");
                int end = enhancedPrompt.lastIndexOf("```");
                if (start >= 0 && end > start) {
                    enhancedPrompt = enhancedPrompt.substring(start + 1, end).trim();
                }
            }

            log.info("SystemPrompt 美化成功: nodeType={}, originalLength={}, enhancedLength={}", 
                    nodeType, userInput != null ? userInput.length() : 0, enhancedPrompt.length());

            return enhancedPrompt;

        } catch (Exception e) {
            log.error("SystemPrompt 美化异常", e);
            // 返回默认的 systemPrompt
            String toolsInfo = buildToolsInfo(toolIds);
            return buildDefaultSystemPrompt(nodeType, toolsInfo);
        }
    }

    /**
     * 构建工具信息
     */
    private String buildToolsInfo(List<UUID> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return "无可用工具";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("可用工具列表：\n\n");

        for (UUID toolId : toolIds) {
            try {
                AiTool tool = toolRepository.findByIdWithSchema(toolId).orElse(null);
                if (tool != null && tool.getEnabled()) {
                    sb.append("- 工具名称: ").append(tool.getName()).append("\n");
                    if (tool.getDisplayName() != null) {
                        sb.append("  显示名称: ").append(tool.getDisplayName()).append("\n");
                    }
                    if (tool.getDescription() != null) {
                        sb.append("  描述: ").append(tool.getDescription()).append("\n");
                    }
                    sb.append("\n");
                }
            } catch (Exception e) {
                log.warn("获取工具信息失败: toolId={}", toolId, e);
            }
        }

        return sb.toString();
    }

    /**
     * 构建美化任务的系统提示词
     */
    private String buildEnhancementSystemPrompt(String nodeType, String toolsInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个专业的 AI 提示词优化专家，擅长根据节点类型、可用工具和用户需求来编写高质量的系统提示词（systemPrompt）。\n\n");
        
        sb.append("## 节点类型说明\n\n");
        sb.append(getNodeTypeDescription(nodeType));
        sb.append("\n\n");
        
        sb.append("## 可用工具\n\n");
        sb.append(toolsInfo);
        sb.append("\n\n");
        
        sb.append("## 任务要求\n\n");
        sb.append("1. 根据节点类型的特点，编写专业、清晰、具体的系统提示词\n");
        sb.append("2. 如果节点可以使用工具，请在提示词中说明如何使用这些工具\n");
        sb.append("3. 提示词应该：\n");
        sb.append("   - 明确角色的定位和职责\n");
        sb.append("   - 说明工作流程和步骤\n");
        sb.append("   - 提供具体的输出格式要求（如果需要）\n");
        sb.append("   - 包含错误处理和边界情况说明\n");
        sb.append("   - 使用清晰、专业的语言\n");
        sb.append("4. 提示词长度适中，既要详细又要简洁（建议 200-500 字）\n");
        sb.append("5. 直接返回优化后的 systemPrompt，不要包含任何解释或说明文字\n");
        
        return sb.toString();
    }

    /**
     * 构建美化任务的用户提示词
     */
    private String buildEnhancementUserPrompt(String nodeType, String toolsInfo, String userInput) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为以下场景编写一个优化的 systemPrompt：\n\n");
        
        sb.append("**节点类型**: ").append(nodeType).append("\n\n");
        
        if (userInput != null && !userInput.trim().isEmpty()) {
            sb.append("**用户输入/需求**: ").append(userInput).append("\n\n");
            sb.append("请根据用户输入理解具体的使用场景，编写针对性的 systemPrompt。\n");
        } else {
            sb.append("**用户输入**: 未提供\n\n");
            sb.append("请编写一个通用的、高质量的 systemPrompt。\n");
        }
        
        return sb.toString();
    }

    /**
     * 获取节点类型描述
     */
    private String getNodeTypeDescription(String nodeType) {
        return switch (nodeType) {
            case "llm" -> """
                    **LLM 节点** - 大语言模型调用节点
                    - 功能：使用 LLM 生成文本回复、回答问题、进行对话等
                    - 特点：可以配置工具调用，支持历史对话记录
                    - 输出：文本回复
                    """;
            case "intent" -> """
                    **Intent 节点** - 意图识别节点
                    - 功能：识别用户输入的意图，进行分类
                    - 特点：Switch 类型节点，根据识别结果路由到不同分支
                    - 输出：意图 ID（用于路由）
                    """;
            case "parameter_extraction" -> """
                    **Parameter Extraction 节点** - 参数提取节点
                    - 功能：从用户对话中提取调用工具所需的参数
                    - 特点：Switch 类型节点，参数完整时返回 success，不完整时返回 incomplete
                    - 输出：提取的参数（设置到 toolsParams）或提示信息
                    """;
            case "setSessionMetadata" -> """
                    **Set Metadata 节点** - 会话元数据设置节点
                    - 功能：从对话中提取信息并保存到会话元数据
                    - 特点：使用 LLM 进行结构化提取
                    - 输出：更新后的元数据信息
                    """;
            case "imageTextSplit" -> """
                    **Image-Text Split 节点** - 图文分割节点
                    - 功能：识别并提取图文数据，将文本和图片配对
                    - 特点：输出结构化数据 {struct:[{img,content}], overview}
                    - 输出：结构化图文数据
                    """;
            default -> """
                    **通用节点**
                    - 功能：根据节点类型执行相应操作
                    - 特点：根据具体节点类型而定
                    """;
        };
    }

    /**
     * 构建默认的 systemPrompt（当美化失败时使用）
     */
    private String buildDefaultSystemPrompt(String nodeType, String toolsInfo) {
        StringBuilder sb = new StringBuilder();
        
        switch (nodeType) {
            case "llm" -> {
                sb.append("你是一个专业的AI助手，能够准确理解用户需求并提供帮助。\n\n");
                if (!toolsInfo.equals("无可用工具")) {
                    sb.append("你可以使用以下工具来帮助用户：\n");
                    sb.append(toolsInfo);
                    sb.append("\n当需要使用工具时，请根据用户需求选择合适的工具并调用。\n");
                }
                sb.append("\n请用友好、专业、准确的语言回复用户。");
            }
            case "intent" -> {
                sb.append("你是一个意图识别专家。请仔细分析用户输入，准确识别用户的意图。\n\n");
                sb.append("请只返回意图的ID，不要返回其他内容。");
            }
            case "parameter_extraction" -> {
                sb.append("你是一个参数提取专家。请从用户对话中准确提取调用工具所需的参数。\n\n");
                sb.append(toolsInfo);
                sb.append("\n请严格按照JSON格式返回提取到的参数值。如果某个参数无法提取，请返回空字符串。");
            }
            case "setSessionMetadata" -> {
                sb.append("你是一个信息提取专家。请从对话中准确提取所需的结构化信息。\n\n");
                sb.append("请严格按照JSON格式返回提取到的信息。");
            }
            case "imageTextSplit" -> {
                sb.append("你是一个图文分析专家。请识别并提取对话中的图文数据。\n\n");
                sb.append("请返回结构化数据，包含图片URL和对应的文本内容。");
            }
            default -> {
                sb.append("你是一个专业的AI助手，请根据用户需求提供准确、有用的帮助。");
            }
        }
        
        return sb.toString();
    }
}

