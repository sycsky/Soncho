package com.example.aikef.service;

import com.example.aikef.event.MessageSentEvent;
import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.model.Message;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.model.mongo.CustomerCoreMemory;
import com.example.aikef.repository.mongo.CustomerCoreMemoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service to consolidate chat history into core memory (user profile).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryConsolidationService {

    private final CustomerCoreMemoryRepository coreMemoryRepository;
    private final LangChainChatService langChainChatService;
    private final ObjectMapper objectMapper;
    private final MessageService messageService;

    @Async
    @EventListener
    public void onMessageSent(MessageSentEvent event) {
        return;
    }

    @Resource
    private com.example.aikef.repository.MessageRepository messageRepository;

    public void consolidateMemory(UUID sessionId, String customerId) {
        log.info("Starting memory consolidation for customer: {}", customerId);

        try {
            // 1. Fetch recent messages (Last 20)
            List<Message> messages = messageRepository.findBySession_IdAndSenderTypeNotInOrderByCreatedAtDesc(
                    sessionId, 
                    List.of(SenderType.TOOL), 
                    org.springframework.data.domain.PageRequest.of(0, 20)
            ).getContent();
            
            // Reverse to chronological order
            List<Message> chronologicalMessages = new java.util.ArrayList<>(messages);
            java.util.Collections.reverse(chronologicalMessages);
            
            StringBuilder conversationBuilder = new StringBuilder();
            for (Message msg : chronologicalMessages) {
                String sender = msg.getSenderType() == SenderType.USER ? "User" : "Agent";
                conversationBuilder.append(sender).append(": ").append(msg.getText()).append("\n");
            }
            String conversationText = conversationBuilder.toString();

            // 2. Fetch existing profile
            CustomerCoreMemory memory = coreMemoryRepository.findByCustomerId(customerId)
                    .orElseGet(() -> {
                        CustomerCoreMemory m = new CustomerCoreMemory();
                        m.setCustomerId(customerId);
                        m.setProfile(new HashMap<>());
                        m.setCreatedAt(LocalDateTime.now());
                        return m;
                    });

            Map<String, Object> currentProfile = memory.getProfile();
            String currentProfileJson = objectMapper.writeValueAsString(currentProfile);

            // 3. Prompt LLM to update profile
            // Construct Prompt
            String systemPrompt = """
                    You are a Memory Manager AI. Your goal is to update the User Profile based on the recent conversation.
                    
                    # Current Profile (JSON):
                    %s
                    
                    # Instructions:
                    1. Analyze the conversation for new facts, preferences, or changes about the user.
                    2. Update the profile JSON.
                    3. Output ONLY the updated JSON.
                    4. If no changes, output the original JSON.
                    """;

            String prompt = String.format(systemPrompt, currentProfileJson);
            
            // Call LLM (using structured output if possible, or just JSON mode)
            // Using a simple model or the default one.
            String responseJson = langChainChatService.simpleChat(prompt, "Conversation:\n" + conversationText);
            
            // 4. Parse and Save
            if (responseJson != null && responseJson.contains("{")) {
                // Extract JSON
                int start = responseJson.indexOf("{");
                int end = responseJson.lastIndexOf("}") + 1;
                String json = responseJson.substring(start, end);
                
                Map<String, Object> newProfile = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                
                memory.setProfile(newProfile);
                
                // Generate Summary
                String summaryPrompt = "Summarize this profile into a concise paragraph for an AI assistant to understand the user:\n" + json;
                String summary = langChainChatService.simpleChat(summaryPrompt, "");
                memory.setSummary(summary);
                
                memory.setLastUpdated(LocalDateTime.now());
                coreMemoryRepository.save(memory);
                
                log.info("Memory consolidated successfully for customer: {}", customerId);
            }

        } catch (Exception e) {
            log.error("Memory consolidation failed", e);
        }
    }
}
