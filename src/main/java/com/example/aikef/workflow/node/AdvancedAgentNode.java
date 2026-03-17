package com.example.aikef.workflow.node;

import cn.hutool.core.util.StrUtil;
import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.model.mongo.CustomerCoreMemory;
import com.example.aikef.model.mongo.ProceduralMemory;
import com.example.aikef.model.mongo.CustomerTool;
import com.example.aikef.repository.mongo.CustomerCoreMemoryRepository;
import com.example.aikef.repository.mongo.ProceduralMemoryRepository;
import com.example.aikef.repository.mongo.CustomerToolRepository;
import com.example.aikef.service.ChatMemoryService;
import com.example.aikef.service.LambdaToolAdapter;
import com.example.aikef.tool.internal.impl.ContextTools;
import com.example.aikef.tool.internal.impl.ProceduralMemoryTools;
import com.example.aikef.tool.model.AiTool;
import com.example.aikef.tool.service.AiToolService;
import com.example.aikef.workflow.context.WorkflowContext;
import com.example.aikef.workflow.service.WorkflowStatusService;
import com.example.aikef.workflow.tool.ToolCallProcessor;
import com.example.aikef.workflow.tool.ToolCallState;
import com.example.aikef.workflow.util.ChatResponseThinkingExtractor;
import com.example.aikef.workflow.util.HistoryMessageLoader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import com.example.aikef.llm.LangChainChatService.StructuredOutputResponse;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

/**
 * Advanced Agent Node
 * Integrated with MongoDB (Core Memory, Custom Tools) and Tool Agent (Dynamic Tool Selection).
 * Supports "Task Building Mode" via State Machine.
 */
@Slf4j
@LiteflowComponent("advancedAgent")
public class AdvancedAgentNode extends BaseWorkflowNode {

    @Resource
    private LangChainChatService langChainChatService;

    @Resource
    private ToolCallProcessor toolCallProcessor;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private HistoryMessageLoader historyMessageLoader;

    @Resource
    private CustomerCoreMemoryRepository coreMemoryRepository;

    @Resource
    private CustomerToolRepository customerToolRepository;

    @Resource
    private ProceduralMemoryRepository proceduralMemoryRepository;

    @Resource
    private ChatMemoryService chatMemoryService;

    @Resource
    private LambdaToolAdapter lambdaToolAdapter;

    @Resource
    private WorkflowStatusService workflowStatusService;

    @Resource
    private ProceduralMemoryTools proceduralMemoryTools;

    @Autowired
    private ContextTools contextTools;

    @Autowired
    private AiToolService aiToolService;

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are an advanced AI assistant dedicated to helping the user.
            You have access to a set of tools and a memory of past interactions.
            
            # CORE INSTRUCTIONS
            1. Use the provided tools whenever necessary to fulfill the user's request.
            2. If you need to perform a task that requires a new capability, check if you can create a tool for it.
            3. Always maintain a helpful and professional tone.
            4. Memory Management: You have long-term memory for user facts, preferences, and agent behavior instructions. If the user provides or corrects such memory, call manageLongTermMemory immediately.
            5. Procedural Memory: You have long-term reusable task playbooks. If user starts, changes, or completes a multi-step task, call manageProceduralMemory immediately so future similar tasks can be executed quickly. Do this even when completion is only implied by natural conversation or tool results.
            6. Never invent tool parameters. If conversation context does not explicitly contain tool-related required values, ask the user first.
            7. For any state-changing tool call, missing or uncertain parameters must trigger clarification before tool execution.
            
            # CONTEXT
            Current Time: %s
            Session ID: %s
            Customer ID: %s
            """;

    private static final String TASK_BUILDER_SYSTEM_PROMPT = """
            You are a specialized Task Builder Agent. Your goal is to clarify the user's request and build a robust task/tool configuration.
            
            # MODE: TASK BUILDING
            1. You are NOT chatting casually. You are gathering requirements.
            2. If the user's request is vague (e.g., missing location, time), ASK CLARIFYING QUESTIONS.
            3. Once you have all necessary information, use the available tools to CREATE or SCHEDULE the task.
            4. After scheduling, inform the user and switch back to normal mode.
            
            # CONTEXT
            Current Time: %s
            """;

    @Resource
    private com.example.aikef.tool.repository.AiToolRepository aiToolRepository;

    @Override
    public void process() {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = getWorkflowContext();
        log.info("Advanced Agent Node started for customer: {}", ctx.getCustomerId());

        try {
            JsonNode config = getNodeConfig();
            String modelIdStr = getConfigString("modelId", null);
            UUID modelId = parseModelId(modelIdStr);
            if (modelId == null) {
                throw new IllegalArgumentException("Model ID is required for Advanced Agent Node");
            }

            Integer maxIterations = getConfigInt("maxIterations", 10);
            Double temperature = getConfigDouble("temperature", 0.7);
            String configPrompt = getConfigString("systemPrompt", "");
            UUID toolAgentModelId = parseModelId(getConfigString("toolAgentModelId", null));
            autoPersistProceduralMemoryFromTopicShift(ctx);

            // === STATE MACHINE LOGIC ===
            // Check if we are in Task Building Mode
            boolean isTaskBuildingMode = ctx.getVariables().containsKey("active_task_builder_context");
            
            CompletableFuture<List<ChatMessage>> contextFuture = CompletableFuture
                    .supplyAsync(() -> buildDynamicContext(ctx, config, isTaskBuildingMode, modelId, configPrompt));
            CompletableFuture<List<ToolSpecification>> toolsFuture = CompletableFuture
                    .supplyAsync(() -> assembleTools(ctx, config, modelId, toolAgentModelId));

            List<ChatMessage> messages = contextFuture.join();
            List<ToolSpecification> toolSpecs = toolsFuture.join();

            // 3. Autonomous Loop
            int iterations = 0;
            String finalOutput = null;

            while (iterations < maxIterations) {
                iterations++;
                log.info("Agent Loop Iteration: {}", iterations);

                if (ctx.getSessionId() != null) {
                    workflowStatusService.updateStatus(ctx.getSessionId(),
                            WorkflowStatusService.StatusType.ANALYZING,
                            "Thinking (Iter " + iterations + ")", ctx);
                }

                // Call LLM
                ChatResponse response = langChainChatService.chatWithTools(modelId, messages, toolSpecs, temperature, null);
                AiMessage aiMessage = ChatResponseThinkingExtractor.enrichAiMessage(response, objectMapper);
                
                // Add AI response to history
                messages.add(AiMessage.builder()
                        .text(aiMessage.text())
                        .toolExecutionRequests(aiMessage.toolExecutionRequests())
                        .build());

                if (aiMessage.hasToolExecutionRequests()) {
                    List<ToolExecutionRequest> requests = aiMessage.toolExecutionRequests();
                    log.info("Agent decided to call tools: {}", requests.size());

                    for (ToolExecutionRequest request : requests) {
                        log.info("Executing tool: {}", request.name());
                        
                        if (ctx.getSessionId() != null) {
                            workflowStatusService.updateStatus(ctx.getSessionId(),
                                    WorkflowStatusService.StatusType.TOOL_CALLING,
                                    request.name(), ctx);
                        }

                        // Execute Tool
                        String result = executeTool(request, ctx);
                        
                        // Add Result to history
                        messages.add(ToolExecutionResultMessage.from(request, result));
                        autoPersistProceduralMemoryFromTool(ctx, request, result);
                        
                        // STATE TRANSITION CHECK
                        // If we successfully scheduled a task or created a tool, we might want to exit Task Mode
                        if (isTaskBuildingMode && (request.name().contains("scheduleTask") || request.name().contains("createTool"))) {
                             if (!result.startsWith("Error")) {
                                 // Task completed successfully, exit Task Mode
                                 // Note: We need a way to persist this state change back to the session/context
                                 // For now, we just log it. The session state management needs to be handled via SetSessionMetadataNode or similar mechanism
                                 // or we return a special signal.
                                 // Let's assume we can modify context variables.
                                 ctx.getVariables().remove("active_task_builder_context");
                                 log.info("Task completed. Exiting Task Building Mode.");
                             }
                        }
                    }
                } else {
                    // Final Answer
                    finalOutput = aiMessage.text();
                    autoPersistProceduralMemoryFromFinalAnswer(ctx, finalOutput);
                    
                    // HEURISTIC MODE SWITCHING
                    // If normal mode, but AI asks a clarifying question about a task, maybe we should enter Task Mode?
                    // This is complex to detect purely from output text.
                    // Ideally, the AI should call a "SwitchModeTool" or similar.
                    // For this iteration, we rely on the explicit "active_task_builder_context" variable set by previous turns or router.
                    
                    break;
                }
            }

            if (finalOutput == null) {
                finalOutput = "I'm sorry, I couldn't complete the task within the limit.";
            }

            setOutput(finalOutput);
            recordExecution(ctx.getQuery(), finalOutput, startTime, true, null);

        } catch (Exception e) {
            log.error("Advanced Agent Node execution failed", e);
            setOutput("Error: " + e.getMessage());
            recordExecution(ctx.getQuery(), "Error", startTime, false, e.getMessage());
        }
    }

    private List<ChatMessage> buildDynamicContext(WorkflowContext ctx,
                                                  JsonNode config,
                                                  boolean isTaskMode,
                                                  UUID modelId,
                                                  String configPrompt) {
        List<ChatMessage> messages = new ArrayList<>();
        String customerId = ctx.getCustomerId() != null ? ctx.getCustomerId().toString() : "Unknown";
        String sessionId = ctx.getSessionId() != null ? ctx.getSessionId().toString() : "Unknown";
        String now = LocalDateTime.now().toString();

        // 1. System Prompt
        String basePrompt = isTaskMode ? TASK_BUILDER_SYSTEM_PROMPT : DEFAULT_SYSTEM_PROMPT;
        String formattedPrompt = isTaskMode 
                ? String.format(basePrompt, now) 
                : String.format(basePrompt, now, sessionId, customerId);
        
        StringBuilder systemPrompt = new StringBuilder(formattedPrompt);
        
        // Inject Core Memory
        if (ctx.getCustomerId() != null) {
            Optional<CustomerCoreMemory> coreMemory = coreMemoryRepository.findByCustomerId(customerId);
            if (coreMemory.isEmpty()) {
                log.info("Core memory not found for customerId={}", customerId);
            } else {
                String coreMemorySummary = buildCoreMemoryInjectionText(coreMemory.get());
                if (StrUtil.isNotBlank(coreMemorySummary)) {
                    systemPrompt.append("\n\n# LONG_TERM_MEMORY\n").append(coreMemorySummary);
                    log.info("Core memory injected for customerId={}, textLength={}, memoryCount={}",
                            customerId,
                            coreMemorySummary.length(),
                            coreMemory.get().getMemories() == null ? 0 : coreMemory.get().getMemories().size());
                } else {
                    log.info("Core memory found but empty for customerId={}", customerId);
                }
            }
        }

        if (ctx.getCustomerId() != null) {
            ProceduralMemory proceduralMemory = selectRelevantProceduralMemory(customerId, ctx.getQuery(), modelId);
            if (proceduralMemory != null) {
                String proceduralSummary = buildProceduralSummary(proceduralMemory);
                if (StrUtil.isNotBlank(proceduralSummary)) {
                    systemPrompt.append("\n\n# PROCEDURAL_MEMORY\n").append(proceduralSummary);
                }
            }
        }
        
        // Inject Goal/Config System Prompt
        if (StrUtil.isNotBlank(configPrompt)) {
            systemPrompt.append("\n\n# ADDITIONAL INSTRUCTIONS\n").append(configPrompt);
        }

        messages.add(SystemMessage.from(systemPrompt.toString()));

        // 2. Relevant History (RAG from PGVector)
        // In Task Mode, we might want to focus only on recent context or specific task history
        if (customerId != null && StrUtil.isNotBlank(ctx.getQuery())) {
            List<String> relevantHistory = chatMemoryService.searchRelevantHistory(customerId, ctx.getQuery(), 5);
            if (!relevantHistory.isEmpty()) {
                StringBuilder historyBlock = new StringBuilder("\n\n# RELEVANT PAST CONVERSATIONS\n");
                for (String hist : relevantHistory) {
                    historyBlock.append("- ").append(hist).append("\n");
                }
                messages.add(SystemMessage.from(historyBlock.toString()));
            }
        }

        if (Boolean.TRUE.equals(ctx.getVariable("isScheduledTask"))) {
            String taskDesc = (String) ctx.getVariable("taskDescription");
            String reminderType = (String) ctx.getVariable("reminderType");
            String taskPrompt = String.format("""
                    
                    # SYSTEM TRIGGERED TASK
                    This is a scheduled system trigger, NOT a user message.
                    Reminder Type: %s
                    Task Content: %s
                    
                    YOUR INSTRUCTION:
                    1. Directly output the reminder message to the user based on the 'Task Content'.
                    2. Do NOT ask for customer information or any other details. The task is confirmed.
                    3. Do NOT start with "I can help you with that" or similar chatter. Just say the reminder.
                    4. If Reminder Type is MAIN, use present tense (e.g., "It's time to...").
                    5. If Reminder Type is INTERMEDIATE, use future tense (e.g., "In 5 minutes...").
                    """, reminderType, taskDesc);
            messages.add(SystemMessage.from(taskPrompt));
        }

        // 3. Short-term History (Recent Messages)
        if (ctx.getSessionId() != null && !Boolean.TRUE.equals(ctx.getVariable("isScheduledTask"))) {
            int readCount = config != null && config.has("readCount") ? config.get("readCount").asInt(10) : 10;
            if (readCount > 0) {
                List<ChatMessage> recentMessages = historyMessageLoader.loadChatMessages(ctx.getSessionId(), readCount, ctx.getMessageId());
                messages.addAll(recentMessages);
            }
        }

        return messages;
    }

    private List<ToolSpecification> assembleTools(WorkflowContext ctx,
                                                  JsonNode config,
                                                  UUID modelId,
                                                  UUID toolAgentModelId) {
        List<ToolSpecification> specs = new ArrayList<>();
        String customerId = ctx.getCustomerId() != null ? ctx.getCustomerId().toString() : null;

        // 1. System Tools
        List<UUID> toolIds = new ArrayList<>();
        if (config != null && config.has("tools")) {
            JsonNode toolsNode = config.get("tools");
            if (toolsNode.isArray()) {
                for (JsonNode node : toolsNode) {
                    try {
                        toolIds.add(UUID.fromString(node.asText()));
                    } catch (Exception ignored) {}
                }
            }
        }

        // 2. Dynamic Tool Retrieval (Tool Agent)
        // Use a dedicated Tool Agent to select relevant tools based on conversation context
        // This replaces the previous RAG-based search
        if (StrUtil.isNotBlank(ctx.getQuery())) {
            // Allow overriding the model for tool selection
            if (toolAgentModelId == null) {
                toolAgentModelId = modelId;
            }

            if (toolAgentModelId != null) {
                List<UUID> selectedToolIds = selectToolsWithAgent(ctx, toolAgentModelId);
                for (UUID id : selectedToolIds) {
                    if (!toolIds.contains(id)) {
                        toolIds.add(id);
                        log.info("Tool Agent injected tool: {}", id);
                    }
                }
            }
        }

        // 3. Auto Inject Tools (Annotation based)
        List<AiTool> autoInjectTools = aiToolService.getAutoInjectTools();
        for (AiTool tool : autoInjectTools) {
            if (!toolIds.contains(tool.getId())) {
                toolIds.add(tool.getId());
                log.info("Auto-Injected tool: {}", tool.getName());
            }
        }

        // 4. Assemble Specifications
        if (!toolIds.isEmpty()) {
            specs.addAll(toolCallProcessor.buildToolSpecifications(toolIds));
        }
        
        // 5. Customer Custom Tools
        if (customerId != null) {
            List<CustomerTool> customerTools = customerToolRepository.findByCustomerId(customerId);
            for (CustomerTool tool : customerTools) {
                try {
                    ToolSpecification.Builder builder = ToolSpecification.builder()
                            .name(tool.getToolName())
                            .description(tool.getDescription());
                    specs.add(builder.build());
                } catch (Exception e) {
                    log.warn("Failed to build spec for customer tool: {}", tool.getToolName(), e);
                }
            }
        }

        return specs;
    }

    /**
     * Use LLM to select relevant tools from all available tools
     */
    private List<UUID> selectToolsWithAgent(WorkflowContext ctx, UUID modelId) {
        List<AiTool> allTools = aiToolService.getEnabledTools();
        if (allTools.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Build System Prompt with Tool Definitions
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert Tool Selector. Your goal is to analyze the conversation and select the most relevant tools from the available list to fulfill the user's request.\n\n");
        prompt.append("### AVAILABLE TOOLS ###\n");
        for (AiTool tool : allTools) {
            prompt.append("- Name: ").append(tool.getName()).append("\n");
            prompt.append("  Description: ").append(tool.getDescription()).append("\n");
        }
        prompt.append("\n### INSTRUCTIONS ###\n");
        prompt.append("1. Analyze the user's latest request and the conversation history.\n");
        prompt.append("2. Select ONLY the tools that are strictly necessary to answer the request.\n");
        prompt.append("3. If no tools are needed, return an empty list.\n");
        prompt.append("4. Return the result as a JSON object with a 'tool_names' array.\n");
        
        // 注入定时任务上下文到 Prompt
        if (Boolean.TRUE.equals(ctx.getVariable("isScheduledTask"))) {
            String taskDesc = (String) ctx.getVariable("taskDescription");
            prompt.append("\n### CURRENT TASK CONTEXT ###\n");
            prompt.append("This is a scheduled system task trigger.\n");
            prompt.append("Task Content: ").append(taskDesc).append("\n");
            prompt.append("Instruction: Select tools needed to complete this specific task (e.g. EmailTool for sending emails).\n");
        }

        // Inject Core Memory for Tool Selection
        if (ctx.getCustomerId() != null) {
            coreMemoryRepository.findByCustomerId(ctx.getCustomerId().toString())
                    .map(CustomerCoreMemory::getSummary)
                    .filter(StrUtil::isNotBlank)
                    .ifPresent(summary -> {
                        prompt.append("\n### USER PREFERENCES (MEMORY) ###\n");
                        prompt.append(summary).append("\n");
                        prompt.append("Instruction: Consider user preferences when selecting tools (e.g., preferred communication channels).\n");
                    });
        }

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(prompt.toString()));

        // 2. Add History
        if (ctx.getSessionId() != null && !Boolean.TRUE.equals(ctx.getVariable("isScheduledTask"))) {
            // Load recent history (e.g. last 4 messages)
            try {
                List<ChatMessage> history = historyMessageLoader.loadChatMessages(ctx.getSessionId(), 4, ctx.getMessageId());
                messages.addAll(history);
            } catch (Exception ignored) {
                log.warn("Failed to load history for Tool Agent", ignored);
            }
        }
        
        // 3. Add Current Input (if not scheduled task or as fallback)
        if (!Boolean.TRUE.equals(ctx.getVariable("isScheduledTask"))) {
             if (StrUtil.isNotBlank(ctx.getQuery())) {
                 // Check if last message is already the user query to avoid duplication
                 boolean lastIsUser = !messages.isEmpty() && (messages.get(messages.size() - 1) instanceof dev.langchain4j.data.message.UserMessage);
                 if (!lastIsUser) {
                    messages.add(dev.langchain4j.data.message.UserMessage.from(ctx.getQuery()));
                 }
             }
        } else {
             // For scheduled task, add a dummy user trigger to ensure LLM has something to respond to
             messages.add(dev.langchain4j.data.message.UserMessage.from("Execute the scheduled task described in system context."));
        }

        // 3. Define JSON Schema for Structured Output
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addProperty("tool_names", JsonArraySchema.builder()
                        .items(JsonStringSchema.builder().build())
                        .description("List of tool names to select")
                        .build())
                .required("tool_names")
                .build();

        // 4. Call LLM
        try {
            StructuredOutputResponse response = langChainChatService.chatWithStructuredOutputMessages(
                    modelId,
                    messages,
                    schema,
                    "tool_selection",
                    0.0 // Low temperature for deterministic selection
            );

            if (response.success()) {
                JsonNode root = objectMapper.readTree(response.jsonResult());
                JsonNode namesNode = root.get("tool_names");
                List<UUID> selectedIds = new ArrayList<>();
                
                if (namesNode != null && namesNode.isArray()) {
                    for (JsonNode nameNode : namesNode) {
                        String name = nameNode.asText();
                        allTools.stream()
                                .filter(t -> t.getName().equalsIgnoreCase(name))
                                .findFirst()
                                .ifPresent(t -> selectedIds.add(t.getId()));
                    }
                }
                log.info("Tool Agent selected {} tools: {}", selectedIds.size(), selectedIds);
                return selectedIds;
            } else {
                log.warn("Tool Agent failed: {}", response.errorMessage());
            }
        } catch (Exception e) {
            log.error("Error during Tool Agent selection", e);
        }

        return Collections.emptyList();
    }

    private String executeTool(ToolExecutionRequest request, WorkflowContext ctx) {
        String toolName = request.name();
        String arguments = request.arguments();
        
        // 1. Try System Tool
        try {
            UUID toolId = toolCallProcessor.getToolIdByName(toolName);
            if (toolId != null) {
                Map<String, Object> params = new HashMap<>();
                if (arguments != null && !arguments.isEmpty()) {
                    params = objectMapper.readValue(arguments, new TypeReference<Map<String, Object>>() {});
                }
                
                ToolCallProcessor.ToolCallProcessResult result = toolCallProcessor.executeToolDirectly(
                    new ToolCallState.ToolCallRequest(request.id(), request.name(), toolId, params), 
                    ctx
                );
                
                if (result.isSuccess()) {
                    return result.getResult().getResult();
                } else {
                    return "Error: " + result.getResult().getErrorMessage();
                }
            }
        } catch (Exception ignored) {}

        // 2. Try Customer Tool (Lambda)
        if (ctx.getCustomerId() != null) {
            String customerId = ctx.getCustomerId().toString();
            Optional<CustomerTool> customTool = customerToolRepository.findByCustomerIdAndToolName(customerId, toolName);
            if (customTool.isPresent()) {
                try {
                    Map<String, Object> args = new HashMap<>();
                    if (arguments != null && !arguments.isEmpty()) {
                        args = objectMapper.readValue(arguments, new TypeReference<Map<String, Object>>() {});
                    }
                    return lambdaToolAdapter.execute(customTool.get().getLambdaArn(), args);
                } catch (Exception e) {
                    return "Error executing custom tool: " + e.getMessage();
                }
            }
        }

        return "Error: Tool not found: " + toolName;
    }

    private UUID parseModelId(String modelIdStr) {
        if (modelIdStr == null || modelIdStr.isBlank()) return null;
        try {
            return UUID.fromString(modelIdStr);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildProceduralSummary(ProceduralMemory memory) {
        StringBuilder builder = new StringBuilder();
        if (StrUtil.isNotBlank(memory.getTitle())) {
            builder.append("Title: ").append(memory.getTitle()).append("\n");
        }
        if (StrUtil.isNotBlank(memory.getScenarioKey())) {
            builder.append("ScenarioKey: ").append(memory.getScenarioKey()).append("\n");
        }
        if (StrUtil.isNotBlank(memory.getStatus())) {
            builder.append("Status: ").append(memory.getStatus()).append("\n");
        }
        if (memory.getCurrentStep() != null) {
            builder.append("CurrentStep: ").append(memory.getCurrentStep()).append("\n");
        }
        if (StrUtil.isNotBlank(memory.getSummary())) {
            builder.append("Summary: ").append(memory.getSummary()).append("\n");
        }
        if (memory.getSteps() != null && !memory.getSteps().isEmpty()) {
            builder.append("Steps:\n");
            for (ProceduralMemory.Step step : memory.getSteps()) {
                builder.append("- [")
                        .append(step.getIndex() == null ? "" : step.getIndex())
                        .append("] ")
                        .append(step.getGoal() == null ? "" : step.getGoal())
                        .append(" | ")
                        .append(step.getState() == null ? "" : step.getState())
                        .append("\n");
            }
        }
        return builder.toString().trim();
    }

    private ProceduralMemory selectRelevantProceduralMemory(String customerId, String query, UUID modelId) {
        List<ProceduralMemory> candidates = proceduralMemoryRepository.findTop20ByCustomerIdAndArchivedFalseOrderByLastUpdatedDesc(customerId);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (StrUtil.isBlank(query)) {
            return candidates.stream()
                    .filter(m -> Boolean.TRUE.equals(m.getReusable()) || "COMPLETED".equalsIgnoreCase(m.getStatus()))
                    .findFirst()
                    .orElse(candidates.get(0));
        }
        try {
            String candidateJson = objectMapper.writeValueAsString(candidates.stream().map(this::toSelectorCandidate).toList());
            String prompt = """
                    You are selecting one best matching procedural memory for the current user query.
                    Return strict JSON only: {"index": number}
                    Rules:
                    1. Prefer reusable/completed playbooks that match user intent.
                    2. If no good match, return {"index": -1}.
                    Candidates:
                    %s
                    Query:
                    %s
                    """;
            String response = langChainChatService.simpleChat(String.format(prompt, candidateJson, query), "");
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}") + 1;
            if (start >= 0 && end > start) {
                JsonNode root = objectMapper.readTree(response.substring(start, end));
                JsonNode indexNode = root.get("index");
                if (indexNode != null && indexNode.canConvertToInt()) {
                    int index = indexNode.asInt();
                    if (index >= 0 && index < candidates.size()) {
                        return candidates.get(index);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to select procedural memory with LLM", e);
        }
        return candidates.stream()
                .filter(m -> Boolean.TRUE.equals(m.getReusable()) || "COMPLETED".equalsIgnoreCase(m.getStatus()))
                .findFirst()
                .orElse(candidates.get(0));
    }

    private Map<String, Object> toSelectorCandidate(ProceduralMemory memory) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", memory.getTitle());
        result.put("scenarioKey", memory.getScenarioKey());
        result.put("status", memory.getStatus());
        result.put("reusable", memory.getReusable());
        result.put("summary", memory.getSummary());
        result.put("triggerPhrases", memory.getTriggerPhrases());
        List<String> stepGoals = new ArrayList<>();
        if (memory.getSteps() != null) {
            for (ProceduralMemory.Step step : memory.getSteps()) {
                if (step != null && StrUtil.isNotBlank(step.getGoal())) {
                    stepGoals.add(step.getGoal());
                }
            }
        }
        result.put("stepGoals", stepGoals);
        return result;
    }

    private void autoPersistProceduralMemoryFromTool(WorkflowContext ctx, ToolExecutionRequest request, String result) {
        if (ctx == null || ctx.getCustomerId() == null) {
            log.debug("Procedural memory skip from tool: missing context or customerId");
            return;
        }
        if (request == null || StrUtil.isBlank(request.name())) {
            log.debug("Procedural memory skip from tool: empty tool request");
            return;
        }
        String toolName = request.name();
        if ("manageProceduralMemory".equalsIgnoreCase(toolName) || "getLatestProceduralMemory".equalsIgnoreCase(toolName)) {
            log.debug("Procedural memory skip from tool: avoid recursive tool {}", toolName);
            return;
        }
        if (result != null && result.startsWith("Error")) {
            log.debug("Procedural memory skip from tool: tool result error, tool={}", toolName);
            return;
        }
        String query = ctx.getQuery();
        if (StrUtil.isBlank(query)) {
            log.debug("Procedural memory skip from tool: blank query, tool={}", toolName);
            return;
        }
        boolean completionSignal = hasCompletionSignal(result) || hasCompletionSignal(query);
        String requirement;
        if (completionSignal) {
            requirement = "Task completed signal. user_query=" + query + "; tool=" + toolName + "; tool_result=" + safeText(result) + "; please consolidate this into reusable long-term procedural memory.";
        } else {
            requirement = "Task progress update. user_query=" + query + "; tool=" + toolName + "; tool_result=" + safeText(result) + "; keep procedural memory draft updated for future reuse.";
        }
        log.info("Procedural memory trigger from tool: customerId={}, tool={}, completionSignal={}, requirementPreview={}",
                ctx.getCustomerId(), toolName, completionSignal, safeText(requirement));
        try {
            String manageResult = proceduralMemoryTools.manageProceduralMemory(ctx.getCustomerId().toString(), requirement, ctx);
            log.info("Procedural memory result from tool trigger: customerId={}, tool={}, result={}",
                    ctx.getCustomerId(), toolName, safeText(manageResult));
        } catch (Exception e) {
            log.warn("Failed auto persistence for procedural memory from tool", e);
        }
    }

    private void autoPersistProceduralMemoryFromFinalAnswer(WorkflowContext ctx, String finalOutput) {
        if (ctx == null || ctx.getCustomerId() == null) {
            log.debug("Procedural memory skip from final answer: missing context or customerId");
            return;
        }
        String query = ctx.getQuery();
        if (StrUtil.isBlank(query) || StrUtil.isBlank(finalOutput)) {
            log.debug("Procedural memory skip from final answer: blank query or output");
            return;
        }
        boolean completionSignal = hasCompletionSignal(query) || hasCompletionSignal(finalOutput);
        if (!completionSignal) {
            log.debug("Procedural memory skip from final answer: no completion signal");
            return;
        }
        String requirement = "Conversation completion signal. user_query=" + query + "; assistant_output=" + safeText(finalOutput) + "; consolidate procedural memory as reusable playbook when applicable.";
        log.info("Procedural memory trigger from final answer: customerId={}, completionSignal={}, requirementPreview={}",
                ctx.getCustomerId(), completionSignal, safeText(requirement));
        try {
            String manageResult = proceduralMemoryTools.manageProceduralMemory(ctx.getCustomerId().toString(), requirement, ctx);
            log.info("Procedural memory result from final-answer trigger: customerId={}, result={}",
                    ctx.getCustomerId(), safeText(manageResult));
        } catch (Exception e) {
            log.warn("Failed auto persistence for procedural memory from final answer", e);
        }
    }

    private boolean hasCompletionSignal(String text) {
        if (StrUtil.isBlank(text)) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
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

    private String safeText(String input) {
        if (input == null) {
            return "";
        }
        String trimmed = input.trim();
        if (trimmed.length() <= 500) {
            return trimmed;
        }
        return trimmed.substring(0, 500);
    }

    private String buildCoreMemoryInjectionText(CustomerCoreMemory memory) {
        if (memory == null) {
            return "";
        }
        if (StrUtil.isNotBlank(memory.getSummary())) {
            return memory.getSummary().trim();
        }
        List<String> memories = memory.getMemories();
        if (memories == null || memories.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int max = Math.min(memories.size(), 20);
        for (int i = 0; i < max; i++) {
            String item = memories.get(i);
            if (StrUtil.isNotBlank(item)) {
                builder.append("- ").append(item.trim()).append("\n");
            }
        }
        return builder.toString().trim();
    }

    private void autoPersistProceduralMemoryFromTopicShift(WorkflowContext ctx) {
        if (ctx == null || ctx.getCustomerId() == null || StrUtil.isBlank(ctx.getQuery())) {
            log.debug("Procedural memory topic-shift skip: missing context/customer/query");
            return;
        }
        if (hasCompletionSignal(ctx.getQuery())) {
            log.debug("Procedural memory topic-shift skip: query already has completion signal");
            return;
        }
        Optional<ProceduralMemory> activeMemory = proceduralMemoryRepository
                .findTopByCustomerIdAndStatusAndArchivedFalseOrderByLastUpdatedDesc(ctx.getCustomerId().toString(), "ACTIVE");
        if (activeMemory.isEmpty()) {
            log.debug("Procedural memory topic-shift skip: no active memory for customer={}", ctx.getCustomerId());
            return;
        }
        ProceduralMemory memory = activeMemory.get();
        boolean topicShift = isTopicShiftFromActiveMemory(memory, ctx.getQuery());
        log.info("Procedural memory topic-shift check: customerId={}, activeScenario={}, topicShift={}",
                ctx.getCustomerId(), memory.getScenarioKey(), topicShift);
        if (!topicShift) {
            return;
        }
        String requirement = "User switched topic without explicit completion. scenario_key=" + safeText(memory.getScenarioKey())
                + "; active_title=" + safeText(memory.getTitle())
                + "; active_summary=" + safeText(memory.getSummary())
                + "; new_query=" + safeText(ctx.getQuery())
                + "; please archive current progress as BLOCKED draft and keep it reusable for future similar tasks, but do not mark as COMPLETED.";
        log.info("Procedural memory trigger from topic-shift: customerId={}, activeScenario={}, requirementPreview={}",
                ctx.getCustomerId(), memory.getScenarioKey(), safeText(requirement));
        try {
            String manageResult = proceduralMemoryTools.manageProceduralMemory(ctx.getCustomerId().toString(), requirement, ctx);
            log.info("Procedural memory result from topic-shift trigger: customerId={}, activeScenario={}, result={}",
                    ctx.getCustomerId(), memory.getScenarioKey(), safeText(manageResult));
        } catch (Exception e) {
            log.warn("Failed auto persistence for procedural memory from topic shift", e);
        }
    }

    private boolean isTopicShiftFromActiveMemory(ProceduralMemory memory, String query) {
        if (memory == null || StrUtil.isBlank(query)) {
            return false;
        }
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        if (containsToken(lowerQuery, memory.getScenarioKey())
                || containsToken(lowerQuery, memory.getTitle())
                || containsAnyToken(lowerQuery, memory.getTriggerPhrases())
                || containsStepGoalToken(lowerQuery, memory.getSteps())) {
            log.debug("Topic-shift quick reject by lexical overlap: scenarioKey={}", memory.getScenarioKey());
            return false;
        }
        try {
            Map<String, Object> active = new LinkedHashMap<>();
            active.put("scenarioKey", memory.getScenarioKey());
            active.put("title", memory.getTitle());
            active.put("summary", memory.getSummary());
            active.put("triggerPhrases", memory.getTriggerPhrases());
            List<String> stepGoals = new ArrayList<>();
            if (memory.getSteps() != null) {
                for (ProceduralMemory.Step step : memory.getSteps()) {
                    if (step != null && StrUtil.isNotBlank(step.getGoal())) {
                        stepGoals.add(step.getGoal());
                    }
                }
            }
            active.put("stepGoals", stepGoals);
            String prompt = """
                    Determine whether user switched to a new topic unrelated to the active workflow.
                    Return strict JSON only: {"topic_shift":true|false}
                    Active workflow:
                    %s
                    User query:
                    %s
                    """;
            String response = langChainChatService.simpleChat(
                    String.format(prompt, objectMapper.writeValueAsString(active), query), "");
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}") + 1;
            if (start >= 0 && end > start) {
                JsonNode root = objectMapper.readTree(response.substring(start, end));
                JsonNode shiftNode = root.get("topic_shift");
                if (shiftNode != null && shiftNode.isBoolean()) {
                    log.debug("Topic-shift LLM classification result: scenarioKey={}, value={}", memory.getScenarioKey(), shiftNode.asBoolean());
                    return shiftNode.asBoolean();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to classify topic shift", e);
        }
        log.debug("Topic-shift classification fallback to true: scenarioKey={}", memory.getScenarioKey());
        return true;
    }

    private boolean containsAnyToken(String query, List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return false;
        }
        for (String token : tokens) {
            if (containsToken(query, token)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsStepGoalToken(String query, List<ProceduralMemory.Step> steps) {
        if (steps == null || steps.isEmpty()) {
            return false;
        }
        for (ProceduralMemory.Step step : steps) {
            if (step != null && containsToken(query, step.getGoal())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsToken(String query, String token) {
        if (StrUtil.isBlank(query) || StrUtil.isBlank(token)) {
            return false;
        }
        String normalized = token.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() < 2) {
            return false;
        }
        return query.contains(normalized);
    }

}
