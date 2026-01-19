package com.example.aikef.workflow.node;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.model.Message;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.tool.model.AiTool;
import com.example.aikef.tool.service.AiToolService;
import com.example.aikef.workflow.context.WorkflowContext;
import com.example.aikef.workflow.exception.WorkflowPausedException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Advanced Agent Node
 * Autonomous agent capable of multi-step reasoning and tool usage (ReAct pattern).
 * 
 * Configuration:
 * {
 *   "goal": "User's goal or instruction",
 *   "modelId": "uuid",
 *   "tools": ["tool-uuid-1", "tool-uuid-2"],
 *   "maxIterations": 10,
 *   "useHistory": true
 * }
 */
@LiteflowComponent("agent")
public class AgentNode extends BaseWorkflowNode {

    private static final Logger log = LoggerFactory.getLogger(AgentNode.class);

    @Resource
    private LangChainChatService langChainChatService;

    @Resource
    private ToolCallProcessor toolCallProcessor;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private HistoryMessageLoader historyMessageLoader;
    
    @Resource
    private com.example.aikef.repository.MessageRepository messageRepository;

    @Resource
    private com.example.aikef.repository.ChatSessionRepository chatSessionRepository;

    @Resource
    private com.example.aikef.tool.repository.AiToolRepository aiToolRepository;

    @Autowired
    private AiToolService aiToolService;

    @Override
    public void process() {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = getWorkflowContext();
        log.info("Agent Node started: {}", ctx.getLastOutput());

        try {
            // Configuration
            JsonNode config = getNodeConfig();
            String modelIdStr = getConfigString("modelId", null);
            String goal = getConfigString("goal", ""); // Or use input if goal is empty
            if (goal.isEmpty()) {
                goal = getConfigString("systemPrompt", ""); // Fallback
            }
            // If goal is still empty, maybe use previous input?
            if (goal.isEmpty()) {
                goal = ctx.getQuery();
            }

            Integer maxIterations = getConfigInt("maxIterations", 10);
            Boolean useHistory = getConfigBoolean("useHistory", true);
            Double temperature = getConfigDouble("temperature", 0.7); // Default to creative for agents

            // Tools
            List<UUID> toolIds = getToolIds(config);

            // Auto-inject 'getWorkflowContext' tool
            try {
                aiToolRepository.findByName("getWorkflowContext").ifPresent(tool -> {
                    if (!toolIds.contains(tool.getId())) {
                        toolIds.add(tool.getId());
                        log.info("Auto-injected tool: getWorkflowContext ({})", tool.getId());
                    }
                });
            } catch (Exception e) {
                log.warn("Failed to auto-inject getWorkflowContext tool", e);
            }

            // Auto-inject 'transferToCustomerService' tool
            try {
                aiToolRepository.findByName("transferToCustomerService").ifPresent(tool -> {
                    if (!toolIds.contains(tool.getId())) {
                        toolIds.add(tool.getId());
                        log.info("Auto-injected tool: transferToCustomerService ({})", tool.getId());
                    }
                });
            } catch (Exception e) {
                log.warn("Failed to auto-inject transferToCustomerService tool", e);
            }

            List<ToolSpecification> toolSpecs = Collections.emptyList();
            if (!toolIds.isEmpty()) {
                toolSpecs = toolCallProcessor.buildToolSpecifications(toolIds);
            }

            // Build Initial Messages
            List<ChatMessage> messages = buildMessages(ctx, config, goal, useHistory);
            
            UUID modelId = parseModelId(modelIdStr);
            if (modelId == null) {
                throw new IllegalArgumentException("Model ID is required for Agent Node");
            }

            // Autonomous Loop
            int iterations = 0;
            String finalOutput = null;

            while (iterations < maxIterations) {
                iterations++;
                log.info("Agent Loop Iteration: {}", iterations);

                // Call LLM
                ChatResponse response = langChainChatService.chatWithTools(modelId, messages, toolSpecs, temperature, null);
                AiMessage aiMessage = ChatResponseThinkingExtractor.enrichAiMessage(response, objectMapper);
                messages.add(aiMessage); // Add AI response to history



                if (aiMessage.hasToolExecutionRequests()) {
                    // Execute Tools
                    List<ToolExecutionRequest> requests = aiMessage.toolExecutionRequests();
                    log.info("Agent decided to call tools: {}", requests.size());

                    List<ToolExecutionOutcome> outcomes = new ArrayList<>();
                    for (ToolExecutionRequest request : requests) {
                        log.info("Executing tool: {}", request.name());

                        ctx.setVariable(request.name()+"_ex", 1);
                        // Execute directly (simplified for autonomous agent)
                        // Note: Real Agent might need state management for parameters, but here we assume LLM provides args
                        ToolExecutionOutcome outcome = executeTool(request, ctx);
                        outcomes.add(outcome);
                        
                        // Add Result to history
                        messages.add(ToolExecutionResultMessage.from(request, outcome.resultText()));
                    }
                    saveToolBatchToDatabase(ctx, aiMessage, requests, outcomes);
                    // Loop continues with new history
                } else {
                    // No tools, just text -> Final Answer
                    finalOutput = aiMessage.text();
                    break;
                }
            }

            if (finalOutput == null) {
                finalOutput = "Agent reached maximum iterations without a final answer.";
            }

            setOutput(finalOutput);
            recordExecution(((SystemMessage)messages.get(0)).text(), finalOutput, startTime, true, null);

        } catch (Exception e) {
            log.error("Agent Node execution failed", e);
            setOutput("Error: " + e.getMessage());
            recordExecution(ctx.getQuery(), "Error", startTime, false, e.getMessage());
        }
    }

    private record ToolExecutionOutcome(boolean success, String resultText, String errorMessage) {
    }

    private ToolExecutionOutcome executeTool(ToolExecutionRequest request, WorkflowContext ctx) {
        long startTime = System.currentTimeMillis();
        String toolName = request.name();
        String arguments = request.arguments();
        
        try {
            UUID toolId = toolCallProcessor.getToolIdByName(toolName);

            // Parse arguments
            Map<String, Object> params = new HashMap<>();
            if (arguments != null && !arguments.isEmpty()) {
                 params = objectMapper.readValue(arguments, new TypeReference<Map<String, Object>>() {});
            }

            // Execute
            ToolCallProcessor.ToolCallProcessResult result = toolCallProcessor.executeToolDirectly(
                new ToolCallState.ToolCallRequest(
                    request.id(), 
                    request.name(), 
                    toolId, 
                    params
                ), 
                ctx
            );

            long duration = System.currentTimeMillis() - startTime;
            
            if (result.isSuccess()) {
                String output = result.getResult().getResult();
                ctx.addToolExecution(getActualNodeId(), "agent", toolName, arguments, output, null, duration, true);
                return new ToolExecutionOutcome(true, output, null);
            }
            
            String errorMessage = result.getResult().getErrorMessage();
            ctx.addToolExecution(getActualNodeId(), "agent", toolName, arguments, null, errorMessage, duration, false);
            return new ToolExecutionOutcome(false, "Tool Execution Failed: " + errorMessage, errorMessage);
        } catch (Exception e) {
            log.error("Tool execution error", e);
            long duration = System.currentTimeMillis() - startTime;
            ctx.addToolExecution(getActualNodeId(), "agent", toolName, arguments, null, e.getMessage(), duration, false);
            return new ToolExecutionOutcome(false, "Tool Execution Error: " + e.getMessage(), e.getMessage());
        }
    }

    private void saveAiMessageToDatabase(WorkflowContext ctx, AiMessage aiMessage) {
        try {
            // If the message has tool execution requests, we don't save it here as a separate message.
            // It will be saved combined with the tool result in saveToolResultToDatabase.
            if (aiMessage.hasToolExecutionRequests()) {
                return;
            }

            Message message = new Message();
            chatSessionRepository.findById(ctx.getSessionId()).ifPresent(message::setSession);
            
            message.setSenderType(SenderType.AI);
            
            message.setInternal(false);
            message.setText(aiMessage.text());
            if (StringUtils.hasText(aiMessage.thinking())) {
                Map<String, Object> toolData = new HashMap<>();
                toolData.put("thinking", aiMessage.thinking());
                message.setToolCallData(toolData);
            }
            
            messageRepository.save(message);
        } catch (Exception e) {
            log.error("Failed to save AI message to DB", e);
        }
    }

    private void saveToolBatchToDatabase(
            WorkflowContext ctx,
            AiMessage aiMessage,
            List<ToolExecutionRequest> requests,
            List<ToolExecutionOutcome> outcomes) {
        try {
            Message message = new Message();
            chatSessionRepository.findById(ctx.getSessionId()).ifPresent(message::setSession);
            message.setSenderType(SenderType.TOOL);
            message.setInternal(false);

            message.setText("TOOL_BATCH");

            Map<String, Object> toolData = new HashMap<>();
            toolData.put("schemaVersion", 2);

            Map<String, Object> aiMap = new HashMap<>();
            aiMap.put("text", aiMessage.text());
            if (StringUtils.hasText(aiMessage.thinking())) {
                aiMap.put("thinking", aiMessage.thinking());
            }

            List<Map<String, Object>> reqList = new ArrayList<>();
            if (requests != null) {
                for (ToolExecutionRequest request : requests) {
                    Map<String, Object> reqMap = new HashMap<>();
                    reqMap.put("id", request.id());
                    reqMap.put("name", request.name());
                    reqMap.put("arguments", request.arguments());
                    reqList.add(reqMap);
                }
            }
            aiMap.put("requests", reqList);
            toolData.put("aiMessage", aiMap);

            List<Map<String, Object>> resultList = new ArrayList<>();
            if (requests != null && outcomes != null) {
                int size = Math.min(requests.size(), outcomes.size());
                for (int i = 0; i < size; i++) {
                    ToolExecutionRequest request = requests.get(i);
                    ToolExecutionOutcome outcome = outcomes.get(i);
                    Map<String, Object> resMap = new HashMap<>();
                    resMap.put("toolCallId", request.id());
                    resMap.put("toolName", request.name());
                    resMap.put("success", outcome.success());
                    resMap.put("result", outcome.resultText());
                    if (outcome.errorMessage() != null) {
                        resMap.put("error", outcome.errorMessage());
                    }
                    resultList.add(resMap);
                }
            }
            toolData.put("results", resultList);

            message.setToolCallData(toolData);
            
            messageRepository.save(message);
        } catch (Exception e) {
            log.error("Failed to save tool result to DB", e);
        }
    }

    // Helper methods copied/adapted from LlmNode
    
    private List<ChatMessage> buildMessages(WorkflowContext ctx, JsonNode config, String systemPrompt, Boolean useHistory) {
        List<ChatMessage> messages = new ArrayList<>();

        // System Prompt / Goal
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            // Render template if needed
            systemPrompt = renderTemplate(systemPrompt);
            messages.add(SystemMessage.from(systemPrompt));
        }

        // History
        if (useHistory && ctx.getSessionId() != null) {
            int readCount = config != null && config.has("readCount") ? config.get("readCount").asInt(0) : 10;
            if (readCount > 0) {
                List<ChatMessage> historyMessages = historyMessageLoader.loadChatMessages(ctx.getSessionId(), readCount, ctx.getMessageId());
                messages.addAll(historyMessages);
            }
        }
        


        return messages;
    }

    private List<UUID> getToolIds(JsonNode config) {
        List<UUID> toolIds = new ArrayList<>();
        if (config != null && config.has("tools")) {
            JsonNode toolsNode = config.get("tools");
            if (toolsNode.isArray()) {
                for (JsonNode toolNode : toolsNode) {
                    try {
                        toolIds.add(UUID.fromString(toolNode.asText()));
                    } catch (Exception e) {
                        log.warn("Invalid Tool ID: {}", toolNode.asText());
                    }
                }
            }
        }
        return toolIds;
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
