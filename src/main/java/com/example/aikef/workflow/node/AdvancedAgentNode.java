package com.example.aikef.workflow.node;

import cn.hutool.core.util.StrUtil;
import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.model.mongo.CustomerCoreMemory;
import com.example.aikef.model.mongo.CustomerTool;
import com.example.aikef.repository.mongo.CustomerCoreMemoryRepository;
import com.example.aikef.repository.mongo.CustomerToolRepository;
import com.example.aikef.service.ChatMemoryService;
import com.example.aikef.service.LambdaToolAdapter;
import com.example.aikef.tool.internal.impl.ContextTools;
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

/**
 * Advanced Agent Node
 * Integrated with MongoDB (Core Memory, Custom Tools) and PGVector (Episodic Memory).
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
    private ChatMemoryService chatMemoryService;

    @Resource
    private LambdaToolAdapter lambdaToolAdapter;

    @Resource
    private WorkflowStatusService workflowStatusService;

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

            // === STATE MACHINE LOGIC ===
            // Check if we are in Task Building Mode
            boolean isTaskBuildingMode = ctx.getVariables().containsKey("active_task_builder_context");
            
            // 1. Build Dynamic Context (Memory)
            List<ChatMessage> messages = buildDynamicContext(ctx, config, isTaskBuildingMode);

            // 2. Assemble Tools (System + Customer)
            List<ToolSpecification> toolSpecs = assembleTools(ctx);

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

    private List<ChatMessage> buildDynamicContext(WorkflowContext ctx, JsonNode config, boolean isTaskMode) {
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
        
        // Inject Core Memory (User Profile)
        if (ctx.getCustomerId() != null) {
            coreMemoryRepository.findByCustomerId(customerId).ifPresent(memory -> {
                if (StrUtil.isNotBlank(memory.getSummary())) {
                    systemPrompt.append("\n\n# USER PROFILE\n").append(memory.getSummary());
                }
            });
        }
        
        // Inject Goal/Config System Prompt
        String configPrompt = getConfigString("systemPrompt", "");
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

        // 3. Short-term History (Recent Messages)
        if (ctx.getSessionId() != null) {
            int readCount = config != null && config.has("readCount") ? config.get("readCount").asInt(10) : 10;
            if (readCount > 0) {
                List<ChatMessage> recentMessages = historyMessageLoader.loadChatMessages(ctx.getSessionId(), readCount, ctx.getMessageId());
                messages.addAll(recentMessages);
            }
        }

        return messages;
    }

    private List<ToolSpecification> assembleTools(WorkflowContext ctx) {
        List<ToolSpecification> specs = new ArrayList<>();
        String customerId = ctx.getCustomerId() != null ? ctx.getCustomerId().toString() : null;

        // 1. System Tools
        JsonNode config = getNodeConfig();
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

        // 2. Dynamic Tool Retrieval (RAG)
        // Instead of just using the current query, we use the recent conversation history to find relevant tools.
        // This handles multi-turn scenarios where the intent might be in previous messages.
        StringBuilder retrievalQuery = new StringBuilder();
        if (StrUtil.isNotBlank(ctx.getQuery())) {
            retrievalQuery.append(ctx.getQuery());
        }
        
        // Append last 2 user messages to context for better retrieval
        if (ctx.getSessionId() != null) {
            try {
                List<ChatMessage> recentMsgs = historyMessageLoader.loadChatMessages(ctx.getSessionId(), 4, ctx.getMessageId());
                for (ChatMessage msg : recentMsgs) {
                    if (msg instanceof UserMessage) {
                        retrievalQuery.append(" ").append(((UserMessage) msg).singleText());
                    }
                }
            } catch (Exception ignored) {}
        }

        String effectiveQuery = retrievalQuery.toString().trim();
        if (StrUtil.isNotBlank(effectiveQuery)) {
            // Retrieve top 5 relevant tools
            List<AiTool> relevantTools = aiToolService.searchToolsBySemantic(effectiveQuery, 5);
            for (AiTool tool : relevantTools) {
                 if (!toolIds.contains(tool.getId())) {
                    toolIds.add(tool.getId());
                    log.info("RAG-retrieved tool: {} ({}) for query context: {}", tool.getName(), tool.getId(), effectiveQuery);
                }
            }
        }

        // 3. Assemble Specifications
        if (!toolIds.isEmpty()) {
            specs.addAll(toolCallProcessor.buildToolSpecifications(toolIds));
        }
        
        // 2. Customer Custom Tools
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
}
