package com.example.aikef.tool.internal.impl;

import com.example.aikef.knowledge.KnowledgeBaseService;
import com.example.aikef.knowledge.VectorStoreService;
import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.llm.LangChainChatService.FieldSchemaDefinition;
import com.example.aikef.llm.LangChainChatService.FieldSchemaDefinition.FieldType;
import com.example.aikef.llm.LangChainChatService.StructuredOutputResponse;
import com.example.aikef.model.KnowledgeBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBaseTools {

    private final KnowledgeBaseService knowledgeBaseService;
    private final LangChainChatService langChainChatService;
    private final ObjectMapper objectMapper;

    @Tool("Search for knowledge base content by user question")
    public String searchKnowledgeBaseByKeyword(@P("user question") String userQuestion) {

        String keyword = userQuestion;

        log.info("Executing searchKnowledgeBaseByKeyword: {}", keyword);
        // 1. List all enabled knowledge bases
        List<KnowledgeBase> kbs = knowledgeBaseService.getEnabledKnowledgeBases();
        if (kbs.isEmpty()) {
            return "No matching results found";
        }

        // 2. Map name to ID
        Map<String, UUID> nameToId = kbs.stream()
                .collect(Collectors.toMap(KnowledgeBase::getName, KnowledgeBase::getId));
        
        List<String> kbNames = new ArrayList<>(nameToId.keySet());
        
        // 3. AI Selection: Match keyword to most likely knowledge bases
        String prompt = String.format("""
                Here are the available knowledge bases: %s.
                The user is searching for: '%s'.
                Which knowledge bases are most relevant?
                Return the names of the relevant knowledge bases.
                If none are relevant, return an empty list.
                """, kbNames, keyword);

        List<String> selectedKbNames = new ArrayList<>();
        try {
            // Define output schema: {"names": ["KB1", "KB2"]}
            FieldSchemaDefinition items = new FieldSchemaDefinition(
                    null, FieldType.STRING, "Knowledge Base Name", false, null, null, null
            );
            FieldSchemaDefinition namesField = new FieldSchemaDefinition(
                    "names", FieldType.ARRAY, "List of relevant knowledge base names", true, null, null, items
            );

            StructuredOutputResponse response = langChainChatService.chatWithFieldDefinitions(
                    null, // use default model
                    "You are a helpful assistant that selects relevant knowledge bases for a search query.",
                    prompt,
                    List.of(namesField),
                    "kb_selection",
                    0.0 // Low temperature for deterministic output
            );

            if (response.success()) {
                JsonNode root = objectMapper.readTree(response.jsonResult());
                if (root.has("names") && root.get("names").isArray()) {
                    for (JsonNode node : root.get("names")) {
                        selectedKbNames.add(node.asText());
                    }
                }
            } else {
                log.warn("Failed to select KBs using AI: {}", response.errorMessage());
            }
        } catch (Exception e) {
            log.error("Error during AI KB selection", e);
        }

        if (selectedKbNames.isEmpty()) {
            log.info("AI selected no knowledge bases for keyword: {}", keyword);
            return "No matching results found";
        }

        log.info("AI selected KBs: {}", selectedKbNames);

        // 4. Map selected names back to IDs
        List<UUID> kbIds = selectedKbNames.stream()
                .map(nameToId::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (kbIds.isEmpty()) {
            return "No matching results found";
        }

        // 5. Vector Search using selected KBs
        // Using default maxResults=5 and minScore=0.6 (adjustable)
        List<VectorStoreService.SearchResult> results = knowledgeBaseService.searchMultiple(kbIds, keyword, 5, 0.6);

        if (results.isEmpty()) {
            return "No matching results found";
        }

        // 6. Format and return results
        return results.stream()
                .map(r -> String.format("Source: %s\nContent: %s", r.getTitle(), r.getContent()))
                .collect(Collectors.joining("\n\n---\n\n"));
    }
}
