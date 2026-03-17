package com.example.aikef.tool.internal.impl;

import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.tool.annotation.AutoInjectTool;
import com.example.aikef.model.mongo.CustomerCoreMemory;
import com.example.aikef.repository.mongo.CustomerCoreMemoryRepository;
import com.example.aikef.workflow.context.WorkflowContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@AutoInjectTool
@RequiredArgsConstructor
public class CoreMemoryTools {
    private static final Pattern FACT_PREFIX_PATTERN = Pattern.compile("^(.{1,30}?是)\\s*.+$");

    private final CustomerCoreMemoryRepository coreMemoryRepository;
    private final LangChainChatService langChainChatService;
    private final ObjectMapper objectMapper;

    @Tool("Manage long-term agent memory in real time. Input is the customer's latest memory-related request. The tool loads existing memories and uses LLM to decide add/update/delete automatically.")
    public String manageLongTermMemory(
            @P(value = "Customer ID", required = false) String customerId,
            @P(value = "User memory request in natural language. Example: '用户是男性，不要频繁提醒我工作'", required = true) String userRequirement,
            @ToolMemoryId WorkflowContext ctx
    ) {
        try {
            String resolvedCustomerId = resolveCustomerId(customerId, ctx);
            if (resolvedCustomerId == null || resolvedCustomerId.isBlank()) {
                return "Error: Customer ID is required.";
            }

            if (userRequirement == null || userRequirement.isBlank()) {
                return "Error: userRequirement is required.";
            }
            log.info("Core memory manage start: customerId={}, requirementPreview={}", resolvedCustomerId, userRequirement.trim());

            CustomerCoreMemory memory = coreMemoryRepository.findByCustomerId(resolvedCustomerId)
                    .orElseGet(() -> {
                        CustomerCoreMemory m = new CustomerCoreMemory();
                        m.setCustomerId(resolvedCustomerId);
                        m.setProfile(new HashMap<>());
                        m.setMemories(new ArrayList<>());
                        m.setCreatedAt(LocalDateTime.now());
                        return m;
                    });

            List<String> existingMemories = memory.getMemories() == null ? new ArrayList<>() : new ArrayList<>(memory.getMemories());
            List<String> updatedMemories = generateUpdatedMemories(existingMemories, userRequirement);
            log.info("Core memory decision done: customerId={}, beforeCount={}, afterCount={}",
                    resolvedCustomerId, existingMemories.size(), updatedMemories.size());

            memory.setMemories(updatedMemories);
            memory.setLastUpdated(LocalDateTime.now());
            memory.setSummary(generateSummary(updatedMemories));
            if (memory.getSummary() == null || memory.getSummary().isBlank()) {
                log.warn("Core memory summary empty after generation: customerId={}, memoriesCount={}",
                        resolvedCustomerId, updatedMemories.size());
            }
            coreMemoryRepository.save(memory);
            log.info("Core memory saved: customerId={}, memoriesCount={}, summaryLength={}",
                    resolvedCustomerId, updatedMemories.size(), memory.getSummary() == null ? 0 : memory.getSummary().length());

            return "Memory updated. Count: " + updatedMemories.size();
        } catch (Exception e) {
            log.error("Failed to manage long-term memory", e);
            return "Error: " + e.getMessage();
        }
    }

    private String resolveCustomerId(String customerId, WorkflowContext ctx) {
        if (customerId != null && !customerId.isBlank()) {
            return customerId;
        }
        if (ctx != null && ctx.getCustomerId() != null) {
            return ctx.getCustomerId().toString();
        }
        return null;
    }

    private List<String> generateUpdatedMemories(List<String> existingMemories, String userRequirement) {
        try {
            String existingJson = objectMapper.writeValueAsString(existingMemories);
            String prompt = """
                    You are a strict memory editor for an AI agent.
                    Input includes existing memories and a new user request.
                    Decide how to update the memory list.
                    Rules:
                    1. Keep memory entries concise and in user's language.
                    2. Merge duplicates or semantically equivalent entries.
                    3. If request corrects old fact, update or replace old entry.
                    4. If request asks to avoid a behavior, store it as an instruction-style memory.
                    5. Return JSON only with this shape:
                    {"updated_memories":["...","..."],"operation":"ADD|UPDATE|DELETE|MERGE|NO_CHANGE","reason":"..."}
                    """;

            String response = langChainChatService.simpleChat(
                    prompt,
                    String.format("Existing memories:\n%s\n\nUser request:\n%s", existingJson, userRequirement)
            );

            int start = response.indexOf("{");
            int end = response.lastIndexOf("}") + 1;
            if (start < 0 || end <= start) {
                return fallbackAppend(existingMemories, userRequirement);
            }
            String json = response.substring(start, end);
            JsonNode root = objectMapper.readTree(json);
            JsonNode memoriesNode = root.get("updated_memories");
            if (memoriesNode == null || !memoriesNode.isArray()) {
                return fallbackAppend(existingMemories, userRequirement);
            }
            List<String> result = new ArrayList<>();
            for (JsonNode item : memoriesNode) {
                String text = item.asText();
                if (text != null && !text.isBlank()) {
                    result.add(text.trim());
                }
            }
            if (result.isEmpty()) {
                return fallbackAppend(existingMemories, userRequirement);
            }
            return sanitizeMemories(result);
        } catch (Exception e) {
            log.warn("Failed to generate updated memories with LLM, fallback append", e);
            return fallbackAppend(existingMemories, userRequirement);
        }
    }

    private String generateSummary(List<String> memories) {
        try {
            if (memories == null || memories.isEmpty()) {
                return "";
            }
            String memoriesJson = objectMapper.writeValueAsString(memories);
            String prompt = "Summarize these long-term memories into a concise paragraph for agent behavior alignment:\n" + memoriesJson;
            String summary = langChainChatService.simpleChat(prompt, "Generate a concise summary.");
            return summary == null ? "" : summary;
        } catch (Exception e) {
            log.warn("Failed to generate memory summary", e);
            return "";
        }
    }

    private List<String> fallbackAppend(List<String> existingMemories, String userRequirement) {
        List<String> result = sanitizeMemories(existingMemories);
        String normalized = userRequirement == null ? "" : userRequirement.trim();
        if (normalized.isBlank()) {
            return result;
        }

        String factPrefix = extractFactPrefix(normalized);
        if (factPrefix != null) {
            result.removeIf(item -> factPrefix.equals(extractFactPrefix(item)));
        }

        if (result.stream().noneMatch(v -> v.equalsIgnoreCase(normalized))) {
            result.add(normalized);
        }
        return result;
    }

    private List<String> sanitizeMemories(List<String> memories) {
        List<String> result = new ArrayList<>();
        if (memories == null || memories.isEmpty()) {
            return result;
        }

        for (String memory : memories) {
            if (memory == null || memory.isBlank()) {
                continue;
            }
            String normalized = memory.trim();
            if (result.stream().noneMatch(v -> v.equalsIgnoreCase(normalized))) {
                result.add(normalized);
            }
        }
        return result;
    }

    private String extractFactPrefix(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        if (normalized.isBlank()) {
            return null;
        }
        Matcher matcher = FACT_PREFIX_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return null;
        }
        return matcher.group(1).trim().toLowerCase(Locale.ROOT);
    }
}
