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
                AiMessage aiMessage = response.aiMessage();
                messages.add(aiMessage); // Add AI response to history



                if (aiMessage.hasToolExecutionRequests()) {
                    // Save AI message to database (to support history)
                    saveAiMessageToDatabase(ctx, aiMessage);
                    // Execute Tools
                    List<ToolExecutionRequest> requests = aiMessage.toolExecutionRequests();
                    log.info("Agent decided to call tools: {}", requests.size());

                    for (ToolExecutionRequest request : requests) {
                        log.info("Executing tool: {}", request.name());

                        ctx.setVariable(request.name()+"_ex", 1);
                        // Execute directly (simplified for autonomous agent)
                        // Note: Real Agent might need state management for parameters, but here we assume LLM provides args
                        String result = executeTool(request, ctx);
                        
                        // Add Result to history
                        messages.add(ToolExecutionResultMessage.from(request, result));

                        // Save tool result to database
                        saveToolResultToDatabase(ctx, request, result);
                    }
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
            recordExecution(goal, finalOutput, startTime, true, null);

        } catch (Exception e) {
            log.error("Agent Node execution failed", e);
            setOutput("Error: " + e.getMessage());
            recordExecution(ctx.getQuery(), "Error", startTime, false, e.getMessage());
        }
    }

    private String executeTool(ToolExecutionRequest request, WorkflowContext ctx) {
        try {
            String toolName = request.name();
            UUID toolId = toolCallProcessor.getToolIdByName(toolName);


            // Create a temporary state for execution
            // ToolCallState state = new ToolCallState();
            // state.setToolId(toolId);
            
            // Parse arguments
            Map<String, Object> params = new HashMap<>();
            String arguments = request.arguments();
            if (arguments != null && !arguments.isEmpty()) {
                 params = objectMapper.readValue(arguments, new TypeReference<Map<String, Object>>() {});
            }
            // state.setCollectedParams(params);

            // Execute
            ToolCallProcessor.ToolCallProcessResult result = toolCallProcessor.executeToolDirectly(
                new ToolCallState.ToolCallRequest(
                    request.id(), 
                    request.name(), 
                    toolId, 
                    params
                ), 
                ctx.getSessionId()
            );
            
            if (result.isSuccess()) {
                return result.getResult().getResult();
            } else {
                return "Tool Execution Failed: " + result.getErrorMessage();
            }
        } catch (Exception e) {
            log.error("Tool execution error", e);
            return "Tool Execution Error: " + e.getMessage();
        }
    }

    private void saveAiMessageToDatabase(WorkflowContext ctx, AiMessage aiMessage) {
        try {
            Message message = new Message();
            chatSessionRepository.findById(ctx.getSessionId()).ifPresent(message::setSession);
            
            if (aiMessage.hasToolExecutionRequests()) {
                message.setSenderType(SenderType.AI_TOOL_REQUEST);
                List<Map<String, Object>> requests = new ArrayList<>();
                for (ToolExecutionRequest req : aiMessage.toolExecutionRequests()) {
                    Map<String, Object> reqMap = new HashMap<>();
                    reqMap.put("id", req.id());
                    reqMap.put("name", req.name());
                    reqMap.put("arguments", req.arguments());
                    requests.add(reqMap);
                }
                
                Map<String, Object> toolData = new HashMap<>();
                toolData.put("toolExecutionRequests", requests);
                message.setToolCallData(toolData);
            } else {
                message.setSenderType(SenderType.AI);
            }
            
            message.setInternal(false);
            message.setText(aiMessage.text());
            
            messageRepository.save(message);
        } catch (Exception e) {
            log.error("Failed to save AI message to DB", e);
        }
    }

    private void saveToolResultToDatabase(WorkflowContext ctx, ToolExecutionRequest request, String resultText) {
        try {
            Message message = new Message();
            chatSessionRepository.findById(ctx.getSessionId()).ifPresent(message::setSession);
            message.setSenderType(SenderType.TOOL);
            message.setInternal(false);
            message.setText(request.name() + "#TOOL#" + resultText);
            
            Map<String, Object> toolData = new HashMap<>();
            toolData.put("toolName", request.name());
            toolData.put("toolCallId", request.id());
            toolData.put("success", !resultText.startsWith("Tool Execution Error"));
            // AgentNode doesn't track duration as easily as LlmNode's processor result, 
            // unless we refactor executeTool to return a rich object.
            
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
            int readCount = config != null && config.has("readCount") ? config.get("readCount").asInt(10) : 10;
            if (readCount > 0) {
                List<ChatMessage> historyMessages = loadHistoryFromDatabase(ctx.getSessionId(), readCount, ctx.getMessageId());
                messages.addAll(historyMessages);
            }
        }
        


        return messages;
    }

    private List<ChatMessage> loadHistoryFromDatabase(UUID sessionId, int readCount, UUID messageId) {
        List<ChatMessage> historyMessages = new ArrayList<>();
        List<Message> dbMessages = historyMessageLoader.loadHistoryMessages(sessionId, readCount, messageId);
        
        for (Message msg : dbMessages) {
            if (msg.getSenderType() == SenderType.USER) {
                historyMessages.add(UserMessage.from(msg.getText()));
            } else if (msg.getSenderType() == SenderType.TOOL) {
                String text = msg.getText();
                String toolName = "UnknownTool";
                String toolResult = text;
                
                if (text != null && text.contains("#TOOL#")) {
                    String[] parts = text.split("#TOOL#", 2);
                    if (parts.length >= 2) {
                        toolName = parts[0];
                        toolResult = parts[1];
                    }
                }
                
                String toolCallId = "unknown_call_id";
                if (msg.getToolCallData() != null && msg.getToolCallData().containsKey("toolCallId")) {
                    Object idObj = msg.getToolCallData().get("toolCallId");
                    if (idObj != null) {
                        toolCallId = idObj.toString();
                    }
                }
                
                historyMessages.add(ToolExecutionResultMessage.from(toolCallId, toolName, toolResult));
            } else if (msg.getSenderType() == SenderType.AI_TOOL_REQUEST) {
                if (msg.getToolCallData() != null && msg.getToolCallData().containsKey("toolExecutionRequests")) {
                    List<ToolExecutionRequest> requests = new ArrayList<>();
                    Object reqsObj = msg.getToolCallData().get("toolExecutionRequests");
                    if (reqsObj instanceof List) {
                        List<?> reqsList = (List<?>) reqsObj;
                        for (Object r : reqsList) {
                            if (r instanceof Map) {
                                Map<?, ?> m = (Map<?, ?>) r;
                                String id = (String) m.get("id");
                                String name = (String) m.get("name");
                                String args = (String) m.get("arguments");
                                requests.add(ToolExecutionRequest.builder().id(id).name(name).arguments(args).build());
                            }
                        }
                    }
                    if (!requests.isEmpty()) {
                        if (msg.getText() != null && !msg.getText().isEmpty()) {
                            historyMessages.add(AiMessage.from(msg.getText(), requests));
                        } else {
                            historyMessages.add(AiMessage.from(requests));
                        }
                    } else {
                        historyMessages.add(AiMessage.from(msg.getText()));
                    }
                } else {
                    historyMessages.add(AiMessage.from(msg.getText()));
                }
            } else {
                historyMessages.add(AiMessage.from(msg.getText()));
            }
        }
        return historyMessages;
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
