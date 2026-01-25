package com.example.aikef.service;

import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.Message;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.repository.MessageRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 会话总结服务
 * 用于在 Resolve 会话时生成 AI 总结
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionSummaryService {

    private final MessageRepository messageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final LangChainChatService langChainChatService;
    private final SessionMessageGateway messageGateway;
    private final TranslationService translationService;

    private static final String SUMMARY_SYSTEM_PROMPT = """
            你是一个专业的客服对话总结助手。请使用 中文 生成一份结构清晰的会话总结。
            
            【输出格式要求】
            请严格按照以下格式输出，每个部分用空行分隔：
            
            📋 客户诉求
            简洁描述客户的主要问题或需求（1-2句话）
            
            💬 服务过程
            • 要点1：描述处理步骤或沟通内容
            • 要点2：描述处理步骤或沟通内容
            （根据实际情况列出2-4个要点）
            
            ✅ 处理结果
            说明最终的处理结果或解决方案（1-2句话）
            
            📌 后续事项
            如有需要跟进的事项或承诺，在此列出；如无则写"无"
            
            【注意事项】
            1. 每个部分都要有内容，不要省略
            2. 语言简洁专业，避免冗余
            3. 必须使用指定的语言输出整个总结内容
            4. 不要添加其他标题或前缀
            """;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    /**
     * 获取需要总结的消息范围
     * 规则：从上一条 SYSTEM 消息之后到当前时间，如果没有 SYSTEM 消息则获取所有消息
     *
     * @param sessionId 会话ID
     * @return 需要总结的消息列表
     */
    public List<Message> getMessagesToSummarize(UUID sessionId) {
        // 查找最后一条 SYSTEM 消息
        Message lastSystemMessage = messageRepository.findFirstBySession_IdAndSenderTypeOrderByCreatedAtDesc(
                sessionId, SenderType.SYSTEM);

        List<Message> messages;
        if (lastSystemMessage != null) {
            // 有 SYSTEM 消息，获取该消息之后的所有非 SYSTEM 消息
            messages = messageRepository.findBySession_IdAndCreatedAtAfterAndSenderTypeNotOrderByCreatedAtAsc(
                    sessionId, lastSystemMessage.getCreatedAt(), SenderType.SYSTEM);
            log.info("获取上次总结后的消息: sessionId={}, lastSystemAt={}, messageCount={}",
                    sessionId, lastSystemMessage.getCreatedAt(), messages.size());
        } else {
            // 没有 SYSTEM 消息，获取所有消息
            messages = messageRepository.findBySession_IdOrderByCreatedAtAsc(sessionId);
            log.info("获取所有会话消息: sessionId={}, messageCount={}", sessionId, messages.size());
        }

        return messages;
    }

    /**
     * 生成会话总结（不保存）
     * 用于预览总结内容
     *
     * @param sessionId 会话ID
     * @param languageCode 语言代码 (例如: "zh", "en", "ja")
     * @return 总结内容
     */
    public SummaryResult generateSummary(UUID sessionId, String languageCode) {
        // 验证会话存在
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("会话不存在"));

        // 获取需要总结的消息
        List<Message> messages = getMessagesToSummarize(sessionId);

        if (messages.isEmpty()) {
            return new SummaryResult(
                    true,
                    "暂无对话内容",
                    null,
                    0
            );
        }

        // 构建对话记录文本
        String chatHistory = buildChatHistoryText(messages);

        if (chatHistory.isBlank()) {
            return new SummaryResult(
                    true,
                    "暂无有效对话内容",
                    null,
                    messages.size()
            );
        }

        // 使用固定中文提示词
        String systemPrompt = SUMMARY_SYSTEM_PROMPT;

        // 调用 LLM 生成总结 (默认中文)
        try {
            String summary = langChainChatService.simpleChat(systemPrompt, chatHistory);
            
            // 如果目标语言不是中文，则使用 AWS Translate 进行翻译
            if (languageCode != null && !languageCode.isBlank() && !languageCode.toLowerCase().startsWith("zh")) {
                try {
                    String translatedSummary = translationService.translate(summary, "zh", languageCode);
                    log.info("总结翻译成功: {} -> {}", "zh", languageCode);
                    summary = translatedSummary;
                } catch (Exception e) {
                    log.error("总结翻译失败, 返回原文: {}", e.getMessage());
                    // 翻译失败降级为返回中文原文
                }
            }
            
            log.info("生成会话总结成功: sessionId={}, language={}, messageCount={}, summaryLength={}",
                    sessionId, languageCode, messages.size(), summary.length());

            return new SummaryResult(
                    true,
                    summary,
                    null,
                    messages.size()
            );
        } catch (Exception e) {
            log.error("生成会话总结失败: sessionId={}", sessionId, e);
            return new SummaryResult(
                    false,
                    null,
                    e.getMessage(),
                    messages.size()
            );
        }
    }

    /**
     * 生成会话总结并保存为 SYSTEM 消息
     * 用于 Resolve 会话时调用
     *
     * @param sessionId 会话ID
     * @param languageCode 语言代码
     * @param customSummary 自定义总结内容（可选）
     * @return 保存的总结消息
     */
    @Transactional
    public Message generateAndSaveSummary(UUID sessionId, String languageCode, String customSummary) {
        String summaryContent;

        if (customSummary != null && !customSummary.isBlank()) {
            // 使用自定义总结
            summaryContent = customSummary;
            log.info("使用自定义会话总结: sessionId={}", sessionId);
        } else {
            // 生成 AI 总结
            SummaryResult result = generateSummary(sessionId, languageCode);

            if (!result.success()) {
                throw new RuntimeException("生成总结失败: " + result.errorMessage());
            }
            summaryContent = result.summary();
        }

        // 保存为 SYSTEM 消息
        Message systemMessage = messageGateway.sendSystemMessage(sessionId, summaryContent);

        log.info("保存会话总结消息: sessionId={}, messageId={}", sessionId, systemMessage.getId());

        return systemMessage;
    }

    private String getLanguageName(String code) {
        if (code == null) return "中文";
        String lang = code.toLowerCase();
        if (lang.startsWith("en")) return "English";
        if (lang.startsWith("zh")) return "中文";
        if (lang.startsWith("ja")) return "Japanese";
        if (lang.startsWith("ko")) return "Korean";
        if (lang.startsWith("fr")) return "French";
        if (lang.startsWith("de")) return "German";
        if (lang.startsWith("es")) return "Spanish";
        if (lang.startsWith("pt")) return "Portuguese";
        if (lang.startsWith("ru")) return "Russian";
        if (lang.startsWith("ar")) return "Arabic";
        return "中文"; // 默认中文
    }

    /**
     * 构建对话记录文本
     */
    private String buildChatHistoryText(List<Message> messages) {
        return messages.stream()
                .filter(msg -> msg.getText() != null && !msg.getText().isBlank())
                .map(msg -> {
                    String time = TIME_FORMATTER.format(msg.getCreatedAt());
                    String role = getSenderRoleName(msg.getSenderType());
                    return String.format("[%s] %s: %s", time, role, msg.getText());
                })
                .collect(Collectors.joining("\n"));
    }

    /**
     * 获取发送者角色名称
     */
    private String getSenderRoleName(SenderType senderType) {
        return switch (senderType) {
            case USER -> "客户";
            case AGENT -> "客服";
            case AI -> "AI助手";
            case SYSTEM -> "系统";
            case TOOL -> "工具";
        };
    }

    /**
     * 总结结果
     */
    public record SummaryResult(
            boolean success,
            String summary,
            String errorMessage,
            int messageCount
    ) {}
}

