package com.example.aikef.tool.internal.impl;

import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.tool.annotation.AutoInjectTool;
import com.example.aikef.model.mongo.ProceduralMemory;
import com.example.aikef.repository.mongo.ProceduralMemoryRepository;
import com.example.aikef.workflow.context.WorkflowContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@AutoInjectTool
@RequiredArgsConstructor
public class ProceduralMemoryTools {

    private final ProceduralMemoryRepository proceduralMemoryRepository;
    private final LangChainChatService langChainChatService;
    private final ObjectMapper objectMapper;

    @Tool("Manage long-term procedural memory. Use this when user describes, changes, or completes a task workflow. This tool updates reusable experience memory for future similar tasks.")
    public String manageProceduralMemory(
            @P(value = "Customer ID", required = false) String customerId,
            @P(value = "User requirement in natural language", required = true) String userRequirement,
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
            log.info("Procedural memory manage start: customerId={}, requirementPreview={}", resolvedCustomerId, safeText(userRequirement));

            List<ProceduralMemory> recent = proceduralMemoryRepository.findTop5ByCustomerIdAndArchivedFalseOrderByLastUpdatedDesc(resolvedCustomerId);
            log.info("Procedural memory recent loaded: customerId={}, size={}", resolvedCustomerId, recent == null ? 0 : recent.size());
            JsonNode decision = buildDecision(recent, userRequirement);
            String decisionSource = "llm";
            if (decision == null || decision.isMissingNode()) {
                log.warn("Procedural memory LLM decision missing, fallback enabled: customerId={}", resolvedCustomerId);
                decision = buildFallbackDecision(recent, userRequirement);
                decisionSource = "fallback";
            }
            if (decision == null || decision.isMissingNode()) {
                return "Error: Failed to build procedural memory decision.";
            }
            log.info("Procedural memory decision ready: customerId={}, source={}, decision={}",
                    resolvedCustomerId, decisionSource, safeText(decision.toString()));

            String scenarioKey = text(decision, "scenario_key");
            if (scenarioKey == null || scenarioKey.isBlank()) {
                scenarioKey = "general_task";
            }
            String finalScenarioKey = scenarioKey;

            ProceduralMemory memory = proceduralMemoryRepository
                    .findTopByCustomerIdAndScenarioKeyAndArchivedFalseOrderByLastUpdatedDesc(resolvedCustomerId, finalScenarioKey)
                    .orElseGet(() -> createDraftMemory(resolvedCustomerId, finalScenarioKey));
            log.info("Procedural memory target selected: customerId={}, scenarioKey={}, existingRecord={}",
                    resolvedCustomerId, finalScenarioKey, memory.getId() != null);

            applyDecision(memory, decision, userRequirement);

            if (memory.getArchived() == null) {
                memory.setArchived(false);
            }
            if (memory.getVersion() == null) {
                memory.setVersion(0L);
            }
            memory.setVersion(memory.getVersion() + 1);
            memory.setLastUpdated(LocalDateTime.now());
            memory.setLastUsedAt(LocalDateTime.now());
            proceduralMemoryRepository.save(memory);
            log.info("Procedural memory saved: customerId={}, scenarioKey={}, status={}, reusable={}, steps={}, version={}",
                    resolvedCustomerId, memory.getScenarioKey(), memory.getStatus(), Boolean.TRUE.equals(memory.getReusable()),
                    size(memory.getSteps()), memory.getVersion());

            return "Procedural memory updated. scenario=" + memory.getScenarioKey() + ", status=" + memory.getStatus() + ", reusable=" + Boolean.TRUE.equals(memory.getReusable()) + ", steps=" + size(memory.getSteps());
        } catch (Exception e) {
            log.error("Failed to manage procedural memory", e);
            return "Error: " + e.getMessage();
        }
    }

    @Tool("Get latest procedural memory for current customer.")
    public String getLatestProceduralMemory(
            @P(value = "Customer ID", required = false) String customerId,
            @ToolMemoryId WorkflowContext ctx
    ) {
        try {
            String resolvedCustomerId = resolveCustomerId(customerId, ctx);
            if (resolvedCustomerId == null || resolvedCustomerId.isBlank()) {
                return "Error: Customer ID is required.";
            }
            Optional<ProceduralMemory> latest = proceduralMemoryRepository.findTopByCustomerIdAndArchivedFalseOrderByLastUpdatedDesc(resolvedCustomerId);
            if (latest.isEmpty()) {
                return "{}";
            }
            return objectMapper.writeValueAsString(latest.get());
        } catch (Exception e) {
            log.error("Failed to get latest procedural memory", e);
            return "Error: " + e.getMessage();
        }
    }

    private ProceduralMemory createDraftMemory(String customerId, String scenarioKey) {
        ProceduralMemory memory = new ProceduralMemory();
        memory.setCustomerId(customerId);
        memory.setScenarioKey(scenarioKey);
        memory.setStatus("ACTIVE");
        memory.setMemoryKind("DRAFT");
        memory.setReusable(false);
        memory.setArchived(false);
        memory.setVersion(0L);
        memory.setSuccessCount(0);
        memory.setCurrentStep(1);
        memory.setSlots(new HashMap<>());
        memory.setSteps(new ArrayList<>());
        memory.setTriggerPhrases(new ArrayList<>());
        memory.setCreatedAt(LocalDateTime.now());
        return memory;
    }

    private void applyDecision(ProceduralMemory memory, JsonNode decision, String userRequirement) {
        String status = text(decision, "status");
        String memoryKind = text(decision, "memory_kind");
        String title = text(decision, "title");
        String summary = text(decision, "summary");

        if (title != null && !title.isBlank()) {
            memory.setTitle(title);
        } else if (memory.getTitle() == null || memory.getTitle().isBlank()) {
            memory.setTitle("Procedural Task");
        }
        if (status != null && !status.isBlank()) {
            memory.setStatus(status);
        } else if (memory.getStatus() == null || memory.getStatus().isBlank()) {
            memory.setStatus("ACTIVE");
        }
        if (summary != null) {
            memory.setSummary(summary);
        }
        if (memoryKind != null && !memoryKind.isBlank()) {
            memory.setMemoryKind(memoryKind.toUpperCase());
        } else if (memory.getMemoryKind() == null || memory.getMemoryKind().isBlank()) {
            memory.setMemoryKind("DRAFT");
        }

        JsonNode currentStepNode = decision.get("current_step");
        if (currentStepNode != null && currentStepNode.isInt()) {
            memory.setCurrentStep(currentStepNode.asInt());
        }

        JsonNode slotsNode = decision.get("slots");
        if (slotsNode != null && slotsNode.isObject()) {
            Map<String, Object> slots = objectMapper.convertValue(slotsNode, Map.class);
            memory.setSlots(slots);
        }

        JsonNode triggersNode = decision.get("trigger_phrases");
        if (triggersNode != null && triggersNode.isArray()) {
            List<String> triggerPhrases = new ArrayList<>();
            for (JsonNode trigger : triggersNode) {
                String text = trigger.asText();
                if (text != null && !text.isBlank()) {
                    triggerPhrases.add(text.trim());
                }
            }
            memory.setTriggerPhrases(triggerPhrases);
        }

        JsonNode stepsNode = decision.get("steps");
        if (stepsNode != null && stepsNode.isArray()) {
            List<ProceduralMemory.Step> steps = new ArrayList<>();
            for (JsonNode stepNode : stepsNode) {
                ProceduralMemory.Step step = new ProceduralMemory.Step();
                step.setIndex(intValue(stepNode, "index"));
                step.setGoal(text(stepNode, "goal"));
                step.setActionType(text(stepNode, "action_type"));
                step.setAction(text(stepNode, "action"));
                step.setResultSummary(text(stepNode, "result_summary"));
                step.setState(text(stepNode, "state"));
                steps.add(step);
            }
            memory.setSteps(steps);
        } else if (memory.getSteps() == null || memory.getSteps().isEmpty()) {
            ProceduralMemory.Step step = new ProceduralMemory.Step();
            step.setIndex(1);
            step.setGoal(userRequirement);
            step.setActionType("ask_user");
            step.setAction(userRequirement);
            step.setResultSummary("");
            step.setState("PENDING");
            memory.setSteps(List.of(step));
        }

        if ("COMPLETED".equalsIgnoreCase(memory.getStatus())) {
            memory.setReusable(true);
            memory.setMemoryKind("PLAYBOOK");
            memory.setCompletedAt(LocalDateTime.now());
            Integer successCount = memory.getSuccessCount() == null ? 0 : memory.getSuccessCount();
            memory.setSuccessCount(successCount + 1);
            if (memory.getCurrentStep() == null || memory.getCurrentStep() <= 0) {
                memory.setCurrentStep(size(memory.getSteps()));
            }
        } else if (memory.getReusable() == null) {
            memory.setReusable(false);
        }
    }

    private JsonNode buildDecision(List<ProceduralMemory> recentMemories, String userRequirement) {
        try {
            String memoriesJson = objectMapper.writeValueAsString(recentMemories == null ? Collections.emptyList() : recentMemories);
            String prompt = """
                    You are a long-term procedural memory planner for an AI assistant.
                    Based on existing procedural memories and the latest user requirement, decide whether to create/update/replan/abort/complete a workflow memory.
                    Users usually do not explicitly ask for memory. Infer task completion from natural conversation and tool status signals.
                    Return strict JSON only:
                    {
                      "operation":"CREATE|UPDATE|REPLAN|ABORT|RESUME|COMPLETE|NO_CHANGE",
                      "scenario_key":"snake_case_identifier",
                      "title":"string",
                      "memory_kind":"DRAFT|PLAYBOOK",
                      "status":"ACTIVE|ABORTED|COMPLETED|BLOCKED",
                      "summary":"string",
                      "trigger_phrases":["short phrase","short phrase"],
                      "current_step":1,
                      "slots":{"k":"v"},
                      "steps":[
                        {"index":1,"goal":"...","action_type":"ask_user|call_tool|confirm","action":"...","result_summary":"...","state":"PENDING|DONE|FAILED|DEPRECATED"}
                      ]
                    }
                    Existing memories:
                    %s
                    Latest requirement:
                    %s
                    """;
            String response = langChainChatService.simpleChat(String.format(prompt, memoriesJson, userRequirement), "");
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}") + 1;
            if (start < 0 || end <= start) {
                log.warn("Procedural memory decision parse failed: no json block, responsePreview={}", safeText(response));
                return null;
            }
            String json = response.substring(start, end);
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("Failed to build procedural memory decision", e);
            return null;
        }
    }

    private JsonNode buildFallbackDecision(List<ProceduralMemory> recentMemories, String userRequirement) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            String scenarioKey = extractScenarioKey(userRequirement);
            if (scenarioKey == null || scenarioKey.isBlank()) {
                scenarioKey = pickRecentScenarioKey(recentMemories);
            }
            if (scenarioKey == null || scenarioKey.isBlank()) {
                scenarioKey = "general_task";
            }
            String status = hasCompletionSignal(userRequirement) ? "COMPLETED" : "ACTIVE";
            root.put("operation", "UPDATE");
            root.put("scenario_key", scenarioKey);
            root.put("title", "Procedural Task");
            root.put("memory_kind", "COMPLETED".equals(status) ? "PLAYBOOK" : "DRAFT");
            root.put("status", status);
            root.put("summary", safeText(userRequirement));
            root.put("current_step", 1);

            ObjectNode slots = objectMapper.createObjectNode();
            slots.put("source", "fallback");
            slots.put("generated_at", LocalDateTime.now().toString());
            root.set("slots", slots);

            ArrayNode triggers = objectMapper.createArrayNode();
            String queryText = extractQueryText(userRequirement);
            if (queryText != null && !queryText.isBlank()) {
                triggers.add(queryText);
            } else {
                triggers.add(safeText(userRequirement));
            }
            root.set("trigger_phrases", triggers);

            ArrayNode steps = objectMapper.createArrayNode();
            ObjectNode step = objectMapper.createObjectNode();
            step.put("index", 1);
            step.put("goal", safeText(queryText == null || queryText.isBlank() ? userRequirement : queryText));
            step.put("action_type", "confirm");
            step.put("action", safeText(userRequirement));
            step.put("result_summary", "");
            step.put("state", "COMPLETED".equals(status) ? "DONE" : "PENDING");
            steps.add(step);
            root.set("steps", steps);
            log.info("Procedural memory fallback decision built: scenarioKey={}, status={}, trigger={}",
                    scenarioKey, status, safeText(queryText == null ? userRequirement : queryText));
            return root;
        } catch (Exception e) {
            log.warn("Failed to build fallback procedural memory decision", e);
            return null;
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

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private Integer intValue(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.canConvertToInt()) {
            return null;
        }
        return value.asInt();
    }

    private int size(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private String pickRecentScenarioKey(List<ProceduralMemory> recentMemories) {
        if (recentMemories == null || recentMemories.isEmpty()) {
            return null;
        }
        for (ProceduralMemory memory : recentMemories) {
            if (memory != null && memory.getScenarioKey() != null && !memory.getScenarioKey().isBlank()) {
                return memory.getScenarioKey();
            }
        }
        return null;
    }

    private String extractScenarioKey(String userRequirement) {
        if (userRequirement == null) {
            return null;
        }
        String marker = "scenario_key=";
        int start = userRequirement.indexOf(marker);
        if (start < 0) {
            return null;
        }
        int from = start + marker.length();
        int end = userRequirement.indexOf(";", from);
        if (end < 0) {
            end = userRequirement.length();
        }
        String value = userRequirement.substring(from, end).trim();
        return value.isBlank() ? null : value;
    }

    private String extractQueryText(String userRequirement) {
        if (userRequirement == null) {
            return null;
        }
        String marker = "user_query=";
        int start = userRequirement.indexOf(marker);
        if (start < 0) {
            return null;
        }
        int from = start + marker.length();
        int end = userRequirement.indexOf(";", from);
        if (end < 0) {
            end = userRequirement.length();
        }
        String value = userRequirement.substring(from, end).trim();
        return value.isBlank() ? null : value;
    }

    private boolean hasCompletionSignal(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase();
        return lower.contains("done")
                || lower.contains("completed")
                || lower.contains("approved")
                || lower.contains("finished")
                || lower.contains("status=done")
                || lower.contains("status=approved")
                || lower.contains("status=completed")
                || text.contains("完成")
                || text.contains("办好了")
                || text.contains("通过")
                || text.contains("结束")
                || text.contains("就按这个办")
                || text.contains("可以了");
    }

    private String safeText(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= 500) {
            return trimmed;
        }
        return trimmed.substring(0, 500);
    }
}
