package com.example.aikef.workflow.service;

import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.model.LlmModel;
import com.example.aikef.repository.LlmModelRepository;
import com.example.aikef.service.WebSocketEventService;
import com.example.aikef.workflow.context.WorkflowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * 工作流状态流式传输服务
 */
@Service
public class WorkflowStatusService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowStatusService.class);

    private final LlmModelRepository llmModelRepository;
    private final LangChainChatService langChainChatService;
    private final WebSocketEventService webSocketEventService;

    public WorkflowStatusService(LlmModelRepository llmModelRepository,
                                LangChainChatService langChainChatService,
                                WebSocketEventService webSocketEventService) {
        this.llmModelRepository = llmModelRepository;
        this.langChainChatService = langChainChatService;
        this.webSocketEventService = webSocketEventService;
    }

    public enum StatusType {
        ANALYZING,         // 正在分析/思考
        INTENT_ANALYZING,  // 正在分析意图
        TOOL_CALLING,      // 正在调用工具
        COMPLETED          // 工作流完成（状态流结束）
    }

    /**
     * 更新并发送状态
     * 
     * @param sessionId 会话ID
     * @param type 状态类型
     * @param data 原始数据（如工具名称或思考上下文）
     * @param context 工作流上下文
     */
    @Async
    public void updateStatus(UUID sessionId, StatusType type, String data, WorkflowContext context) {
        if (context != null && !context.isStatusStreamingEnabled()) {
            return;
        }

        try {
            String userQuery = context != null ? context.getQuery() : null;
            String language = context != null ? context.getStreamingLanguage() : "en";
            String interpretedStatus = interpretStatus(type, data, language, userQuery);
            webSocketEventService.broadcastStatusToSession(sessionId, type.name(), interpretedStatus);
        } catch (Exception e) {
            log.error("Failed to update workflow status", e);
        }
    }

    /**
     * 发送完成状态（用于异常情况或兜底）
     */
    public void sendCompletedStatus(UUID sessionId) {
        try {
            webSocketEventService.broadcastStatusToSession(sessionId, StatusType.COMPLETED.name(), "");
        } catch (Exception e) {
            log.error("Failed to send completed status", e);
        }
    }

    /**
     * 使用小模型解释状态
     */
    private String interpretStatus(StatusType type, String data, String language, String userQuery) {
        // 查找状态解释专用的小模型
        LlmModel smallModel = llmModelRepository.findFirstByStatusExplanationTrueAndEnabledTrueOrderBySortOrderAsc()
                .orElse(null);

        if (smallModel == null) {
            // 如果没有配置小模型，返回默认描述
            return getDefaultDescription(type, data, language);
        }

        String prompt = buildPrompt(type, data, language, userQuery);
        try {
            // 使用小模型进行翻译和解释
            String reply = langChainChatService.chat(smallModel.getId(), 
                    "You are a helpful assistant that explains agent actions in simple terms.", 
                    prompt, null, 0.3, 500).reply();
            
            // 去除 <think> 标签内容
            if (reply != null) {
                reply = reply.replaceAll("(?s)<think>.*?</think>", "").trim();
            }
            return reply;
        } catch (Exception e) {
            log.warn("Failed to interpret status using small model, falling back to default", e);
            return getDefaultDescription(type, data, language);
        }
    }

    private String buildPrompt(StatusType type, String data, String language, String userQuery) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Explain the following agent action in a natural, friendly way.\n");
        
        if (userQuery != null && !userQuery.isBlank()) {
            prompt.append(String.format("The user's original message is: \"%s\". Please respond in the SAME LANGUAGE as the user's message.\n", userQuery));
        } else {
            prompt.append(String.format("Please respond in language '%s'.\n", language));
        }
        
        prompt.append("Keep it very short (max 15 words). Use a single emoji at the start.\n");
        prompt.append(String.format("Action Type: %s\n", type.name()));
        prompt.append(String.format("Action Data: %s\n", data != null ? data : "none"));
        prompt.append("Output only the explanation.");
        
        return prompt.toString();
    }

    private String getDefaultDescription(StatusType type, String data, String language) {
        boolean isZh = "zh".equalsIgnoreCase(language);
        return switch (type) {
            case ANALYZING -> isZh ? "⚙️ 正在思考如何帮您..." : "⚙️ Thinking how to help you...";
            case INTENT_ANALYZING -> isZh ? "🎯 正在分析您的意图..." : "🎯 Analyzing your intent...";
            case TOOL_CALLING -> isZh ? "🔍 正在执行任务: " + data : "🔍 Executing task: " + data;
            case COMPLETED -> isZh ? "✅ 完成" : "✅ Completed";
        };
    }
}
