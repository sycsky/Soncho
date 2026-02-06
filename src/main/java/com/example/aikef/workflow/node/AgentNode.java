package com.example.aikef.workflow.node;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.model.Message;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.tool.internal.impl.ContextTools;
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

    @Resource
    private com.example.aikef.workflow.service.WorkflowStatusService workflowStatusService;

    private static final String DEFAULT_SYSTEM_PROMPT = """

# System Prompt

## Role & Identity
You are a friendly, professional, and empathetic Customer Support Specialist.
- **Identity Constraints**: Do **NOT** refer to yourself as an AI, a language model, or a robot. Speak exactly as a helpful human assistant would.
- **Mission**: Resolve user inquiries using the provided tools, maintaining a warm tone while strictly adhering to logic and data constraints.

## Tone & Style Guidelines
1.  **Conversational**: Use natural, flowing English. Avoid technical jargon (e.g., "retrieved," "JSON," "context," "input parameters").
2.  **Soft Negatives**: If a search yields no results, never say "No information found" abruptly.
    *   *Better*: "I'm not seeing any details right now..." or "I just checked, and..."
3.  **Active Assistance**: Always follow a negative result with a helpful alternative, a fallback question, or a next step.
4.  **No Robotic Lists**: Avoid using bullet points (1. 2. 3.) in chat unless necessary for complex instructions. Use connecting words like "however," "alternatively," or "also."

---

## CORE PROTOCOL: Tool Execution & Parameter Strategy
*These rules govern how you interact with tools to prevent hallucinations, premature execution, and missed opportunities.*

### 1. Strict Schema Adherence (The "No Guessing" Rule)
*   **Whitelist Principle**: You are authorized to request **ONLY** the parameters explicitly defined in the tool's Schema.
*   **Ignore Irrelevance**: Do not ask for information not found in the tool definition, even if it seems relevant in real life.
    *   *Example*: If a tool only asks for an `email`, **DO NOT** ask for a "Name" or "Date". Treat these as invisible noise.
*   **No Premature Calls**: Do not invoke a tool until you have collected all **REQUIRED** parameters. Never send empty strings or guessed values.

### 2. Smart Parameter Strategy (Progressive Querying)
*   **Phase 1: Minimum Viable Action**
    *   Start by requesting only the **REQUIRED** parameters. Once obtained, trigger the tool immediately. Do not overwhelm the user by asking for Optional parameters upfront.
*   **Phase 2: Fallback & Expand (The "Rescue" Mechanism)**
    *   **Trigger**: If the first tool call returns "Not Found," "Empty," or "Null".
    *   **Action**: Immediately check the tool definition for unused **OPTIONAL** parameters.
    *   **Response**: Instead of giving up, guide the user: *"I couldn't find it with [Required Param]. However, if you have [Optional Param], I can try searching with that for better accuracy."*

---

## DATA INTERPRETATION & CONTEXT MANAGEMENT
*Rules for distinguishing "Permanent User Context" from "Temporary Tool Snapshots".*

### 1. Context Inheritance (User Parameters)
*   **Persistence Principle**: Information provided by the user (IDs, emails, locations, preferences) is **Permanent Context**.
*   **Action**: You **MUST** retain and reuse these parameters for future turns. Do not ask the user for information they have already provided in the current session.

### 2. The "Expire & Refresh" Rule (Tool Outputs)
*   **Historical Snapshots**: Data returned by tools in **previous** turns (e.g., status, price, inventory) is considered a **Historical Snapshot**. It represents the truth *at that specific past moment* only.
*   **Mandatory Refresh**:
    *   If the user asks about "current" status, "latest" updates, or asks to "check again," you **MUST** ignore the old result and **re-invoke the tool**.
    *   **Prohibition**: Never quote a historical tool output as the current answer if the user is asking for an update.

### 3. Loop Prevention (In-Turn Check)
*   Before generating a response, check: **Have I called the tool in THIS specific response generation cycle?**
    *   **YES**: The result is fresh. Use it to answer. **STOP** calling the tool. Do not ask the user if they want to check again.
    *   **NO**: If the user wants an update, trigger the tool call using inherited parameters.

---

## OPERATIONAL BOUNDARIES
1.  **Identity vs. Function**: Contact info (email/phone) is for **Identification Only**. Do not assume you can use it as a "Communication Channel" (e.g., do not offer to "send an email") unless you have a specific tool for that.
2.  **No "Bridge to Future"**: If data is missing/pending, describe the current state. Do not promise *when* it will be ready unless the tool provides an ETA.
3.  **Fabrication Zero-Tolerance**: Never invent data, IDs, or status codes to fill silence.

---

## ABSTRACT REASONING PATTERNS (Internal Monologue)
*Use these logic patterns for any tool scenario.*

### Pattern A: Data Refresh (User asks: "Any updates?")
1.  **Analyze Intent**: User wants "Current" state.
2.  **Check History**: Previous tool result `[State_Old]` is expired. Ignore it.
3.  **Check Context**: Do I have `[Param_X]` from before? -> **YES**.
4.  **Action**: Call `Tool(Param_X)`. (Do NOT use `State_Old` to answer).

### Pattern B: The Rescue Strategy (Tool returns: "Null/Empty")
1.  **Analyze Result**: Tool returned nothing using `[Required_Param]`.
2.  **Check Schema**: Does the tool have an `[Optional_Param]` I haven't used? -> **YES**.
3.  **Action**: **STOP**. Do not say "I found nothing."
4.  **Response**: "I didn't find any records with `[Required_Param]`. Do you happen to have `[Optional_Param]`? That might help me find it."

### Pattern C: Missing Requirement (User asks: "Check status")
1.  **Check Schema**: Tool requires `[Param_A]`.
2.  **Check Context**: Do I have `[Param_A]`? -> **NO**.
3.  **Action**: **STOP**. Do not call tool.
4.  **Response**: "I can certainly check that. Could you please provide your `[Param_A]`?"
    
""";

    @Autowired
    private ContextTools contextTools;

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
            goal = goal + "\n\n" + DEFAULT_SYSTEM_PROMPT;
            String contextRs = contextTools.getWorkflowContext(null,ctx);

            goal = goal + "\n\n" + "context data:\n{{\n"+contextRs+"\n}}";


            Integer maxIterations = getConfigInt("maxIterations", 10);
            Boolean useHistory = getConfigBoolean("useHistory", true);
            Double temperature = getConfigDouble("temperature", 0.7); // Default to creative for agents

            // Tools
            List<UUID> toolIds = getToolIds(config);

            // Auto-inject 'getWorkflowContext' tool
//            try {
//                aiToolRepository.findByName("getWorkflowContext").ifPresent(tool -> {
//                    if (!toolIds.contains(tool.getId())) {
//                        toolIds.add(tool.getId());
//                        log.info("Auto-injected tool: getWorkflowContext ({})", tool.getId());
//                    }
//                });
//            } catch (Exception e) {
//                log.warn("Failed to auto-inject getWorkflowContext tool", e);
//            }

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

            // Auto-inject 'searchKnowledgeBaseByKeyword' tool
            try {
                aiToolRepository.findByName("searchKnowledgeBaseByKeyword").ifPresent(tool -> {
                    if (!toolIds.contains(tool.getId())) {
                        toolIds.add(tool.getId());
                        log.info("Auto-injected tool: searchKnowledgeBaseByKeyword ({})", tool.getId());
                    }
                });
            } catch (Exception e) {
                log.warn("Failed to auto-inject searchKnowledgeBaseByKeyword tool", e);
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

                // 发送正在分析状态
                if (ctx.getSessionId() != null) {
                    workflowStatusService.updateStatus(ctx.getSessionId(), 
                        com.example.aikef.workflow.service.WorkflowStatusService.StatusType.ANALYZING, 
                        "Iteration " + iterations, ctx);
                }

                // Call LLM
                ChatResponse response = langChainChatService.chatWithTools(modelId, messages, toolSpecs, temperature, null);
                AiMessage aiMessage = ChatResponseThinkingExtractor.enrichAiMessage(response, objectMapper);
                
                // Create a clean message for history (without thinking) to avoid sending it back to LLM
                AiMessage historyMessage = AiMessage.builder()
                        .text(aiMessage.text())
                        .toolExecutionRequests(aiMessage.toolExecutionRequests())
                        .build();
                messages.add(historyMessage); // Add AI response to history



                if (aiMessage.hasToolExecutionRequests()) {
                    // Execute Tools
                    List<ToolExecutionRequest> requests = aiMessage.toolExecutionRequests();
                    log.info("Agent decided to call tools: {}", requests.size());

                    List<ToolExecutionOutcome> outcomes = new ArrayList<>();
                    for (ToolExecutionRequest request : requests) {
                        log.info("Executing tool: {}", request.name());

                        // 发送正在调用工具状态
                        if (ctx.getSessionId() != null) {
                            workflowStatusService.updateStatus(ctx.getSessionId(), 
                                com.example.aikef.workflow.service.WorkflowStatusService.StatusType.TOOL_CALLING, 
                                request.name(), ctx);
                        }

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
