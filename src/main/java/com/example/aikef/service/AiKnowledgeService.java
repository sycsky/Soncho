package com.example.aikef.service;

import com.example.aikef.dto.response.AiRewriteResponse;
import com.example.aikef.dto.response.AiSuggestTagsResponse;
import com.example.aikef.dto.response.AiSummaryResponse;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.Customer;
import com.example.aikef.model.Message;
import com.example.aikef.model.enums.SubscriptionPlan;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.repository.MessageRepository;
import com.example.aikef.util.TagHeuristics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import com.example.aikef.llm.LangChainChatService;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class AiKnowledgeService {

    private final MessageRepository messageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final CustomerTagService customerTagService;
    private final SubscriptionService subscriptionService;
    private final LangChainChatService langChainChatService;
    private final ObjectMapper objectMapper;

    public AiKnowledgeService(MessageRepository messageRepository,
                             ChatSessionRepository chatSessionRepository,
                             CustomerTagService customerTagService,
                             SubscriptionService subscriptionService,
                             LangChainChatService langChainChatService,
                             ObjectMapper objectMapper) {
        this.messageRepository = messageRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.customerTagService = customerTagService;
        this.subscriptionService = subscriptionService;
        this.langChainChatService = langChainChatService;
        this.objectMapper = objectMapper;
    }

    public AiSummaryResponse summarize(String sessionId) {
        UUID id = UUID.fromString(sessionId);
        List<Message> messages = messageRepository.findBySession_IdOrderByCreatedAtAsc(id);
        String summary = messages.stream()
                .map(message -> DateTimeFormatter.ISO_INSTANT.format(message.getCreatedAt()) + " - " + message.getText())
                .collect(Collectors.joining("\n"));
        if (summary.isBlank()) {
            summary = "暂无可用内容";
        }
        return new AiSummaryResponse(summary);
    }

    public AiRewriteResponse rewrite(String text, String tone, String sessionId) {
        String prompt = "You are a professional customer support agent. Rewrite the following draft reply to be more professional, polite, and empathetic.";
        if (tone != null && !tone.isBlank()) {
            prompt += " Tone: " + tone + ".";
        }
        
        prompt += "\n\nDraft: " + text;
        
        // Add history context if sessionId is provided
        if (sessionId != null && !sessionId.isBlank()) {
            try {
                UUID id = UUID.fromString(sessionId);
                // Fetch last 10 messages
                List<Message> history = messageRepository.findBySession_IdOrderByCreatedAtDesc(id, PageRequest.of(0, 10)).getContent();
                
                if (!history.isEmpty()) {
                    // Reverse to chronological order
                    Collections.reverse(history);
                    
                    String context = history.stream()
                        .map(m -> (m.getSenderType().name().equals("AGENT") ? "Agent: " : "Customer: ") + m.getText())
                        .collect(Collectors.joining("\n"));
                        
                    prompt = "Context (Last 10 messages):\n" + context + "\n\n" + prompt;
                }
            } catch (Exception e) {
                log.warn("Failed to fetch history for rewrite: {}", e.getMessage());
            }
        }
        
        try {
            String rewritten = langChainChatService.simpleChat("You are an expert copywriter for customer service.", prompt);
            return new AiRewriteResponse(rewritten);
        } catch (Exception e) {
            log.error("AI Rewrite failed", e);
            // Fallback
            return new AiRewriteResponse(text);
        }
    }

    @Async
    @Transactional
    public CompletableFuture<AiSuggestTagsResponse> suggestTags(String sessionId) {
        UUID id = UUID.fromString(sessionId);
        
        // 1. 获取最近 10 条会话消息
        List<Message> messages = new ArrayList<>(messageRepository.findBySession_IdOrderByCreatedAtDesc(id, PageRequest.of(0, 10)).getContent());
        // Reverse to chronological order
        if (!messages.isEmpty()) {
            Collections.reverse(messages);
        }
        
        String conversationText = messages.stream()
                .map(m -> (m.getSenderType().name().equals("AGENT") ? "Agent: " : "Customer: ") + m.getText())
                .collect(Collectors.joining("\n"));

        // Use AI to generate tags
        List<String> suggestedTags;
        try {
            String systemPrompt = "Analyze the conversation and suggest 3-5 tags that categorize the customer's intent or status. Examples: Refund, Shipping, VIP, Complaint, Inquiry.";
            String userPrompt = "Conversation:\n" + conversationText;
            
            LangChainChatService.StructuredOutputResponse response = langChainChatService.chatWithStructuredOutput(
                null, // Use default model
                systemPrompt,
                userPrompt,
                JsonObjectSchema.builder()
                    .addProperty("tags", JsonArraySchema.builder()
                        .items(JsonStringSchema.builder().build())
                        .build())
                    .required("tags")
                    .build(),
                "tags_suggestion",
                0.3
            );
            
            if (response.success()) {
                 // The result should be a JSON object like {"tags": ["Tag1", "Tag2"]}
                 String json = response.jsonResult();
                 try {
                     JsonNode rootNode = objectMapper.readTree(json);
                     if (rootNode.has("tags") && rootNode.get("tags").isArray()) {
                         suggestedTags = new java.util.ArrayList<>();
                         for (JsonNode node : rootNode.get("tags")) {
                             String tag = node.asText().trim();
                             if (!tag.isBlank()) {
                                 suggestedTags.add(tag);
                             }
                         }
                     } else {
                         suggestedTags = List.of();
                     }
                 } catch (Exception e) {
                     log.warn("Failed to parse tags JSON: {}", json, e);
                     suggestedTags = List.of();
                 }
            } else {
                log.warn("AI Tag generation failed: {}", response.errorMessage());
                // Fallback to heuristics
                suggestedTags = List.copyOf(TagHeuristics.suggest(conversationText));
            }
        } catch (Exception e) {
             log.error("Error in AI tagging", e);
             suggestedTags = List.copyOf(TagHeuristics.suggest(conversationText));
        }

        
        // 2. 查找会话及其关联的客户
        ChatSession session = chatSessionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Session not found: " + sessionId));

        // Check Subscription Plan
        if (session.getTenantId() != null) {
            SubscriptionPlan plan = subscriptionService.getPlan(session.getTenantId());
            if (!plan.isSupportAiTags()) {
                log.info("AI Tags feature skipped for session {} (Plan: {})", sessionId, plan);
                return CompletableFuture.completedFuture(new AiSuggestTagsResponse(List.of()));
            }
        }
        
        Customer customer = session.getCustomer();
        
        // 3. 如果客户存在，直接覆盖其 AI 标签
        if (customer != null && !suggestedTags.isEmpty()) {
            try {
                customerTagService.setAiTags(customer.getId(), suggestedTags);
                log.info("AI tags updated for customer {} in session {}: {}", 
                        customer.getId(), sessionId, suggestedTags);
            } catch (Exception e) {
                log.error("Failed to update AI tags for customer {} in session {}", 
                        customer.getId(), sessionId, e);
                // 即使更新失败，仍然返回建议的标签
            }
        } else {
            log.warn("No customer associated with session {}, AI tags not saved", sessionId);
        }
        
        return CompletableFuture.completedFuture(new AiSuggestTagsResponse(suggestedTags));
    }

    private static final class TagHeuristics {
        private TagHeuristics() {
        }

        static Set<String> suggest(String corpus) {
            if (corpus == null || corpus.isBlank()) {
                return Set.of("待分类");
            }
            Set<String> result = new java.util.LinkedHashSet<>();
            if (corpus.contains("退款") || corpus.contains("退货")) {
                result.add("售后");
            }
            if (corpus.contains("支付") || corpus.contains("账单")) {
                result.add("账单问题");
            }
            if (corpus.contains("物流") || corpus.contains("快递")) {
                result.add("物流");
            }
            if (result.isEmpty()) {
                result.add("一般咨询");
            }
            return result;
        }
    }
}
