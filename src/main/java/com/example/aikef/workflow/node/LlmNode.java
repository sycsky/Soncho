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
import com.example.aikef.workflow.service.WorkflowPauseService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.Map;

/**
 * LLM 调用节点（支持工具调用）
 * 使用 LangChain4j 调用配置的大语言模型
 * 
 * 配置示例:
 * {
 *   "messages": [
 *     { "role": "user", "content": "{{sys.query}}" }
 *   ],
 *   "systemPrompt": "你是一个智能客服助手",
 *   "temperature": 0.8,
 *   "maxTokens": 2000,
 *   "modelId": "uuid",
 *   "useHistory": true,                        // 是否使用历史记录
 *   "readCount": 10,                           // 历史记录读取条数（默认10条）
 *   "tools": ["tool-uuid-1", "tool-uuid-2"],   // 绑定的工具ID列表
 *   "enableToolCall": true                     // 是否启用工具调用
 * }
 * 
 * 历史消息处理：
 * - 从数据库按 sessionId 查询会话历史消息
 * - 忽略 SYSTEM 类型的消息
 * - SenderType.USER 转换为用户消息（user role）
 * - SenderType.AGENT/AI 转换为客服消息（assistant role）
 * - 按时间正序排列，最新的消息在最后
 */
@LiteflowComponent("llm")
public class LlmNode extends BaseWorkflowNode {

    @Resource
    private LangChainChatService langChainChatService;

    @Resource
    private ToolCallProcessor toolCallProcessor;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private com.example.aikef.llm.LlmModelService llmModelService;

    @Resource
    private WorkflowPauseService pauseService;
    
    @Resource
    private HistoryMessageLoader historyMessageLoader;
    
    @Autowired
    private AiToolService aiToolService;

    @Override
    public void process() {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = getWorkflowContext();
        log.info("llm 开始调用:{}",ctx.getLastOutput());
        try {
            // 检查是否是从暂停状态恢复
//            ToolCallState toolState = ctx.getToolCallState();
//            toolState.setStatus(ToolCallState.Status.EXECUTING_TOOL);
//            if (toolState != null && toolState.getStatus() == ToolCallState.Status.WAITING_USER_INPUT) {
//                // 继续处理工具调用
//                handleToolCallContinuation(ctx, toolState, startTime);
//                return;
//            }

//            log.info("检测到 {} 个工具调用请求，将串行处理", toolRequests.size());
            log.info("config={}", getNodeConfig());
            // 正常的 LLM 调用流程
            processNormalLlmCall(ctx, startTime);
            
        } catch (WorkflowPausedException e1){
            log.info("流程暂停");
        } catch (Exception e) {
            log.error("LLM 调用失败", e);
            String errorResponse = "抱歉，我暂时无法处理您的请求，请稍后再试。";
            setOutput(errorResponse);
            recordExecution(ctx.getQuery(), errorResponse, startTime, false, e.getMessage());
        }
    }

    /**
     * 正常的 LLM 调用流程
     */
    private void processNormalLlmCall(WorkflowContext ctx, long startTime) throws Exception {
        // 获取节点配置
        JsonNode config = getNodeConfig();
        String modelIdStr = getConfigString("modelId", null);
        String systemPrompt = getConfigString("systemPrompt", "");
        Double temperature = getConfigDouble("temperature", null);
        Integer maxTokens = getConfigInt("maxTokens", null);
        Boolean useHistory = getConfigBoolean("useHistory", false);
//        Boolean enableToolCall = getConfigBoolean("enableToolCall", false);

        // 解析系统提示词中的变量
        systemPrompt = renderTemplate(systemPrompt);

        // 获取绑定的工具
        List<UUID> toolIds = getToolIds(config);
        List<ToolSpecification> toolSpecs = Collections.emptyList();
        
        if (!toolIds.isEmpty()) {
            toolSpecs = toolCallProcessor.buildToolSpecifications(toolIds);
            log.info("LLM 节点绑定了 {} 个工具", toolSpecs.size());
        }

        // 构建消息列表
        List<ChatMessage> messages = buildMessages(ctx, config, systemPrompt, useHistory);

        // 如果有工具，使用带工具的调用方式
        if (!toolSpecs.isEmpty()) {
            handleLlmCallWithTools(ctx, messages, toolSpecs, modelIdStr, temperature, maxTokens, startTime);
        } else {
            // 普通 LLM 调用
            handleNormalLlmCall(ctx, messages, modelIdStr, systemPrompt, temperature, maxTokens, startTime);
        }
    }

    /**
     * 处理带工具的 LLM 调用
     */
    private void handleLlmCallWithTools(
            WorkflowContext ctx,
            List<ChatMessage> messages,
            List<ToolSpecification> toolSpecs,
            String modelIdStr,
            Double temperature,
            Integer maxTokens,
            long startTime) throws Exception {

        // 增强系统提示词，引导 LLM 在识别到意图时强制调用工具
//        enhanceMessagesForToolCall(messages, toolSpecs);

        UUID modelId = parseModelId(modelIdStr);
        ChatResponse response = langChainChatService.chatWithTools(modelId, messages, toolSpecs, temperature, maxTokens);
        AiMessage aiMessage = response.aiMessage();

        log.info("LLM 响应: hasToolExecutionRequests={}, text={}", 
                aiMessage.hasToolExecutionRequests(),
                aiMessage.text() != null ? (aiMessage.text().length() > 50 ? aiMessage.text().substring(0, 50) + "..." : aiMessage.text()) : "null");

        // 检查是否有工具调用请求
        if (aiMessage.hasToolExecutionRequests()) {
            // 处理工具调用（注意：当 LLM 触发工具调用时，text() 通常为空，这是正常的）
            handleToolExecutionRequests(ctx, aiMessage, messages, toolSpecs, modelIdStr, startTime);
        } else {
            // 普通回复
            String reply = aiMessage.text();
            if (reply == null || reply.isBlank()) {
                reply = "抱歉，我暂时无法理解您的请求，请换个方式描述。";
            }
            setOutput(reply);
            recordExecution(buildInputInfo(messages), reply, startTime, true, null);
        }
    }

    /**
     * 处理工具执行请求
     */
    private void handleToolExecutionRequests(
            WorkflowContext ctx,
            AiMessage aiMessage,
            List<ChatMessage> messages,
            List<ToolSpecification> toolSpecs,
            String modelIdStr,
            long startTime) {

        List<ToolExecutionRequest> toolRequests = aiMessage.toolExecutionRequests();
        log.info("检测到 {} 个工具调用请求，将串行处理", toolRequests.size());
        log.info("config={}", getNodeConfig());
        // 初始化工具调用状态
        ToolCallState toolState = ctx.getOrCreateToolCallState();
        toolState.setStatus(ToolCallState.Status.TOOL_CALL_DETECTED);
        toolState.setLlmMessageWithToolCall(aiMessage.text());

        // 解析工具调用请求
        for (ToolExecutionRequest toolRequest : toolRequests) {
            String toolName = toolRequest.name();
            String arguments = toolRequest.arguments();
            String callId = toolRequest.id();

            log.info("工具调用: id={}, name={}, arguments={}", callId, toolName, arguments);

            // 解析参数
            Map<String, Object> params = new HashMap<>();
            try {
                if (arguments != null && !arguments.isEmpty()) {
                    params = objectMapper.readValue(arguments, new TypeReference<Map<String, Object>>() {});
                }
            } catch (Exception e) {
                log.warn("解析工具参数失败: {}", e.getMessage());
            }

            // 获取工具ID
            UUID toolId = toolCallProcessor.getToolIdByName(toolName);

            ToolCallState.ToolCallRequest request = new ToolCallState.ToolCallRequest(
                    callId, toolName, toolId, params
            );
            toolState.addToolCall(request);
        }

        // 处理第一个工具调用（不验证参数完整性，直接执行）
        if (!toolState.getPendingToolCalls().isEmpty()) {
            ToolCallState.ToolCallRequest firstRequest = toolState.getPendingToolCalls().get(0);
            toolState.setCurrentToolCall(firstRequest);
            toolState.setStatus(ToolCallState.Status.EXECUTING_TOOL);
            toolState.setToolId(firstRequest.getToolId());
            
            // 直接执行工具，不进行参数完整性检查
            // LLM 提取的参数直接使用，如果 bodyTemplate 为空则直接使用参数 JSON
            ToolCallProcessor.ToolCallProcessResult result = 
                    toolCallProcessor.executeToolDirectly(toolState, ctx.getSessionId());

            handleToolCallResult(ctx, result, messages, toolSpecs, modelIdStr, startTime);
        }
    }

    /**
     * 处理工具调用结果
     */
    private void handleToolCallResult(
            WorkflowContext ctx,
            ToolCallProcessor.ToolCallProcessResult result,
            List<ChatMessage> messages,
            List<ToolSpecification> toolSpecs,
            String modelIdStr,
            long startTime) {

        ToolCallState toolState = ctx.getToolCallState();

        switch (result.getType()) {
            case SUCCESS, FAILED -> {
                // 工具执行完成，将结果发送回 LLM
                toolState.addResult(result.getResult());
                toolState.setStatus(result.isSuccess() ? 
                        ToolCallState.Status.TOOL_COMPLETED : ToolCallState.Status.TOOL_FAILED);

                // 从待处理列表中移除当前工具调用
                toolState.getPendingToolCalls().removeIf(req -> 
                    req.getId().equals(toolState.getCurrentToolCall().getId()));

                // 检查是否还有待处理的工具调用
                if (!toolState.getPendingToolCalls().isEmpty()) {
                    // 还有待处理的工具调用，继续处理下一个
                    log.info("还有 {} 个待处理的工具调用，继续处理下一个", toolState.getPendingToolCalls().size());
                    processNextToolCall(ctx, messages, toolSpecs, modelIdStr, startTime);
                } else {
                    // 所有工具调用都完成了，将结果发送回 LLM
                sendToolResultToLlm(ctx, messages, toolSpecs, modelIdStr, startTime);
                }
            }

            case SKIPPED -> {
                // 跳过工具执行
                toolState.setStatus(ToolCallState.Status.SKIPPED);
                String skipMessage = "无法完成工具调用，" + result.getErrorMessage();
                setOutput(skipMessage);
                recordExecution(ctx.getQuery(), skipMessage, startTime, true, null);
            }

            case ERROR -> {
                // 错误
                toolState.setStatus(ToolCallState.Status.TOOL_FAILED);
                String errorMessage = "工具调用失败：" + result.getErrorMessage();
                setOutput(errorMessage);
                recordExecution(ctx.getQuery(), errorMessage, startTime, false, result.getErrorMessage());
            }
        }
    }

    /**
     * 继续处理工具调用（用户提供了参数后）
     */
    private void handleToolCallContinuation(WorkflowContext ctx, ToolCallState toolState, long startTime) {
        log.info("继续处理工具调用，用户输入: {}", ctx.getQuery());

        // 恢复工作流
        ctx.resumeWorkflow();

        // 检查是否从数据库恢复的暂停状态
        Boolean resumeFromPause = (Boolean) ctx.getVariable("_resumeFromPause");
        if (Boolean.TRUE.equals(resumeFromPause)) {
            // 恢复已收集的参数
            @SuppressWarnings("unchecked")
            Map<String, Object> savedParams = (Map<String, Object>) ctx.getVariable("_collectedParams");
            if (savedParams != null && !savedParams.isEmpty()) {
                toolState.getCollectedParams().putAll(savedParams);
                log.info("已恢复收集的参数: {}", savedParams.keySet());
            }
            
            // 恢复轮次信息
            Integer currentRound = (Integer) ctx.getVariable("_currentRound");
            Integer maxRounds = (Integer) ctx.getVariable("_maxRounds");
            if (currentRound != null) {
                toolState.setCurrentRound(currentRound);
            }
            if (maxRounds != null) {
                toolState.setMaxRounds(maxRounds);
            }
            
            // 清除恢复标记
            ctx.setVariable("_resumeFromPause", null);
        }

        // 获取模型ID
        String modelIdStr = getConfigString("modelId", null);
        UUID modelId = parseModelId(modelIdStr);

        // 继续参数收集
        ToolCallProcessor.ToolCallProcessResult result = 
                toolCallProcessor.continueParamCollection(toolState, ctx.getQuery(), modelId, ctx.getSessionId());

        // 重建消息列表（这里简化处理）
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(UserMessage.from(ctx.getQuery()));

        // 获取工具规格
        JsonNode config = getNodeConfig();
        List<UUID> toolIds = getToolIds(config);
        List<ToolSpecification> toolSpecs = toolCallProcessor.buildToolSpecifications(toolIds);

        handleToolCallResult(ctx, result, messages, toolSpecs, modelIdStr, startTime);
    }

    /**
     * 获取当前节点所在的子链ID
     */
    private String getSubChainId(WorkflowContext ctx) {
        // 从上下文中获取子链ID（由工作流服务在执行时设置）
        String subChainId = (String) ctx.getVariable("_currentSubChainId");
        if (subChainId != null) {
            return subChainId;
        }
        
        // 尝试从工作流配置中构建子链ID
        UUID workflowId = ctx.getWorkflowId();
        String llmNodeId = getActualNodeId();
        if (workflowId != null && llmNodeId != null) {
            // 使用约定的子链ID格式
            String workflowIdShort = workflowId.toString().replace("-", "").substring(0, 8);
            return String.format("subchain_%s_%s", workflowIdShort, llmNodeId);
        }
        
        return null;
    }

    /**
     * 处理下一个工具调用（串行处理多个工具调用）
     */
    private void processNextToolCall(
            WorkflowContext ctx,
            List<ChatMessage> messages,
            List<ToolSpecification> toolSpecs,
            String modelIdStr,
            long startTime) {
        
        ToolCallState toolState = ctx.getToolCallState();
        
        if (toolState.getPendingToolCalls().isEmpty()) {
            log.warn("没有待处理的工具调用");
            return;
        }
        
        // 获取下一个待处理的工具调用
        ToolCallState.ToolCallRequest nextRequest = toolState.getPendingToolCalls().get(0);
        toolState.setCurrentToolCall(nextRequest);
        toolState.setStatus(ToolCallState.Status.EXECUTING_TOOL);
        toolState.setToolId(nextRequest.getToolId());
        
        log.info("处理下一个工具调用: toolName={}, toolId={}", nextRequest.getToolName(), nextRequest.getToolId());
        
        // 直接执行工具，不进行参数完整性检查
        ToolCallProcessor.ToolCallProcessResult result = 
                toolCallProcessor.executeToolDirectly(toolState, ctx.getSessionId());
        
        handleToolCallResult(ctx, result, messages, toolSpecs, modelIdStr, startTime);
    }

    /**
     * 将工具执行结果发送回 LLM
     */
    private void sendToolResultToLlm(
            WorkflowContext ctx,
            List<ChatMessage> originalMessages,
            List<ToolSpecification> toolSpecs,
            String modelIdStr,
            long startTime) {

        try {
            ToolCallState toolState = ctx.getToolCallState();

            // 构建消息列表（包含工具执行结果）
            List<ChatMessage> messages = new ArrayList<>(originalMessages);

            // 添加 AI 消息（包含工具调用请求）
            // 注意：这里需要重建 AiMessage，实际实现可能需要存储原始 AiMessage
            String finalReply = "";
            // 添加工具执行结果
            for (ToolCallState.ToolCallResult result : toolState.getCompletedResults()) {

                ToolExecutionResultMessage resultMessage = ToolExecutionResultMessage.from(
                        result.getToolCallId(),
                        result.getToolName(),
                        result.isSuccess() ? result.getResult() : "执行失败: " + result.getErrorMessage()
                );

                AiTool tool = aiToolService.getTool(toolState.getToolId()).orElse(null);
                String toolName = result.getToolName();
                String resultDescription = tool.getResultDescription();
                String resultMetadata = tool.getResultMetadata();
                String resultBody = result.getResult();
                if(tool != null){
//                    finalReply += String.format("工具 %s 执行结果: %s\n", toolName, result.isSuccess() ? result.getResult() : "执行失败: " + result.getErrorMessage());
                    if(StringUtils.hasText(resultDescription)){
                        finalReply += String.format("工具 %s 执行结果描述: %s\n", toolName, resultDescription);
                    }

                    if(resultMetadata != null){
                        JSONArray resultJson = new JSONArray(resultMetadata);
                        finalReply += String.format(" %s 执行结果ResponsePath=>:%s 字段元数据:\n", toolName,tool.getApiResponsePath());
                        for (int i = 0; i < resultJson.size(); i++) {
                            JSONObject item = resultJson.getJSONObject(i);
                            finalReply += String.format("  %s=>%s\n", item.getStr("key"), item.getStr("description"));
                        }
                    }
                    try {
                        JSONObject rp = new JSONObject(resultBody);
                        finalReply += String.format("工具 %s 执行结果: %s\n", toolName, new JSONObject(resultBody).toJSONString(2));
                    }catch (Exception e){
                        finalReply += String.format("工具 %s 执行结果: %s\n", toolName, resultBody);
                    }

                    finalReply += "==================================================\n";
                }else {
                    finalReply += String.format("执行结果: %s\n", resultBody);
                }


            }



            // 所有工具调用都已完成，重置工具状态
            toolState.reset();

            setOutput(finalReply);
            recordExecution(buildInputInfo(messages), finalReply, startTime, true, null);

            log.info("工具调用完成，LLM 最终回复: {}", 
                    finalReply.length() > 100 ? finalReply.substring(0, 100) + "..." : finalReply);

        } catch (Exception e) {
            log.error("发送工具结果到 LLM 失败", e);
            setOutput("抱歉，处理工具调用结果时出错");
            recordExecution(ctx.getQuery(), "处理失败", startTime, false, e.getMessage());
        }
    }

    /**
     * 普通 LLM 调用（不带工具）
     */
    private void handleNormalLlmCall(
            WorkflowContext ctx,
            List<ChatMessage> messages,
            String modelIdStr,
            String systemPrompt,
            Double temperature,
            Integer maxTokens,
            long startTime) {

        // 转换消息格式
        List<LangChainChatService.ChatHistoryMessage> historyMessages = new ArrayList<>();
        for (ChatMessage msg : messages) {
            if (msg instanceof UserMessage) {
                historyMessages.add(new LangChainChatService.ChatHistoryMessage("user", ((UserMessage) msg).singleText()));
            } else if (msg instanceof AiMessage) {
                historyMessages.add(new LangChainChatService.ChatHistoryMessage("assistant", ((AiMessage) msg).text()));
            } else if (msg instanceof SystemMessage) {
                historyMessages.add(new LangChainChatService.ChatHistoryMessage("system", ((SystemMessage) msg).text()));
            }
        }

        // 调用 LLM
        UUID modelId = parseModelId(modelIdStr);
        LangChainChatService.LlmChatResponse response = langChainChatService.chatWithMessages(
                modelId,
                systemPrompt,
                historyMessages,
                temperature,
                maxTokens
        );

        if (!response.success()) {
            throw new RuntimeException("LLM 调用失败: " + response.errorMessage());
        }

        String reply = response.reply();

        log.info("LLM 调用成功: model={}, duration={}ms, tokens={}/{}",
                response.modelName(),
                response.durationMs(),
                response.inputTokens(),
                response.outputTokens());

        // 保存使用信息到上下文
        ctx.setVariable("lastLlmModelId", response.modelId());
        ctx.setVariable("lastLlmModelName", response.modelName());
        ctx.setVariable("lastLlmInputTokens", response.inputTokens());
        ctx.setVariable("lastLlmOutputTokens", response.outputTokens());

        setOutput(reply);
        recordExecution(buildInputInfo(messages), reply, startTime, true, null);
    }

    /**
     * 构建消息列表
     * 
     * @param ctx 工作流上下文
     * @param config 节点配置（包含 readCount 等参数）
     * @param systemPrompt 系统提示词
     * @param useHistory 是否使用历史记录
     */
    private List<ChatMessage> buildMessages(
            WorkflowContext ctx,
            JsonNode config,
            String systemPrompt,
            Boolean useHistory) {

        List<ChatMessage> messages = new ArrayList<>();

        // 系统消息
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(SystemMessage.from(systemPrompt));
        }

        // 检查是否从暂停状态恢复，优先使用保存的对话历史
        @SuppressWarnings("unchecked")
        List<Map<String, String>> savedChatHistory = (List<Map<String, String>>) ctx.getVariable("_savedChatHistory");

        if (savedChatHistory != null && !savedChatHistory.isEmpty()) {
            // 从暂停状态恢复的对话历史（只有 user 和 assistant，不含 system）
            for (Map<String, String> msg : savedChatHistory) {
                String role = msg.get("role");
                String content = msg.get("content");
                if (content != null && !content.isEmpty()) {
                    switch (role) {
                        case "user" -> messages.add(UserMessage.from(content));
                        case "assistant" -> messages.add(AiMessage.from(content));
                    }
                }
            }
            log.info("从暂停状态恢复了 {} 条对话历史", savedChatHistory.size());
            
            // 添加用户新消息
            String newQuery = ctx.getQuery();
            if (newQuery != null && !newQuery.isEmpty()) {
                messages.add(UserMessage.from(newQuery));
                log.info("添加用户新消息: {}", newQuery.length() > 50 ? newQuery.substring(0, 50) + "..." : newQuery);
            }

            ctx.setVariable("_savedChatHistory", null);
        } else {
            // 正常流程：从数据库读取历史消息
            if (useHistory && ctx.getSessionId() != null) {
                // 获取历史记录读取条数配置，默认10条
                int readCount = 10;
                if (config != null && config.has("readCount")) {
                    readCount = config.get("readCount").asInt(10);
                }
                
                if (readCount > 0) {
                    List<ChatMessage> historyMessages = loadHistoryFromDatabase(
                            ctx.getSessionId(), readCount, ctx.getMessageId());
                    messages.addAll(historyMessages);
                    log.debug("从数据库加载了 {} 条历史消息", historyMessages.size());
                }
            }

            // 配置中的 messages（模板消息）
            JsonNode messagesConfig = config != null ? config.get("messages") : null;
            if (messagesConfig != null && messagesConfig.isArray() && !messagesConfig.isEmpty()) {
                for (JsonNode msgNode : messagesConfig) {
                    String role = msgNode.has("role") ? msgNode.get("role").asText("user") : "user";
                    String content = msgNode.has("content") ? msgNode.get("content").asText("") : "";

                    content = renderTemplate(content);

                    if (!content.isEmpty()) {
                        if ("user".equals(role)) {
                            messages.add(UserMessage.from(content));
                        } else if ("assistant".equals(role)) {
                            messages.add(AiMessage.from(content));
                        } else if ("system".equals(role)) {
                            messages.add(SystemMessage.from(content));
                        }
                    }
                }
            }
        }

        return messages;
    }

    /**
     * 序列化 ChatMessage 列表为 JSON（不包含 system 消息）
     */
    private String serializeChatHistory(List<ChatMessage> messages) {
        try {
            List<Map<String, String>> historyList = new ArrayList<>();
            for (ChatMessage msg : messages) {
                // 跳过 system 消息，不保存到暂停状态
                if (msg instanceof SystemMessage) {
                    continue;
                }
                
                Map<String, String> msgMap = new HashMap<>();
                if (msg instanceof UserMessage userMsg) {
                    msgMap.put("role", "user");
                    msgMap.put("content", userMsg.singleText());
                } else if (msg instanceof AiMessage aiMsg) {
                    msgMap.put("role", "assistant");
                    msgMap.put("content", aiMsg.text());
                }
                if (msgMap.get("content") != null) {
                    historyList.add(msgMap);
                }
            }
            return objectMapper.writeValueAsString(historyList);
        } catch (Exception e) {
            log.error("序列化对话历史失败", e);
            return null;
        }
    }

    /**
     * 增强消息列表，添加工具调用引导
     * 让 LLM 在识别到用户意图时强制调用工具，即使参数不完整
     * 
     * @param messages 消息列表
     * @param toolSpecs 工具规格列表
     */
    private void enhanceMessagesForToolCall(List<ChatMessage> messages, List<ToolSpecification> toolSpecs) {
        if (toolSpecs == null || toolSpecs.isEmpty()) {
            return;
        }

        // 构建工具名称列表
        StringBuilder toolNames = new StringBuilder();
        for (int i = 0; i < toolSpecs.size(); i++) {
            if (i > 0) toolNames.append("、");
            toolNames.append(toolSpecs.get(i).name());
        }

        // 构建工具调用引导提示词
        String toolCallGuidance = String.format("""
            
            【重要：工具调用规则】
            你可以使用以下工具：%s
            
            当用户的消息表达了明确的意图（如查询、预订、办理业务等）时，你必须：
            1. 立即调用对应的工具（tool call）
            2. 即使用户没有提供完整的参数，也要调用工具
            3. 对于用户未提供的参数，使用 null 值
            4. 不要直接用文字回复询问用户参数，必须先触发工具调用
            5. 如果用户没法提供必填的参数，例如不知道，不清楚等情况则委婉拒绝用户的请求
            
            系统会自动处理参数收集，你只需要识别意图并调用工具即可。
            """, toolNames.toString());

        // 在消息列表开头添加或追加到现有系统消息
        boolean hasSystemMessage = false;
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            if (msg instanceof SystemMessage systemMsg) {
                // 追加到现有系统消息
                String enhancedContent = systemMsg.text() + toolCallGuidance;
                messages.set(i, SystemMessage.from(enhancedContent));
                hasSystemMessage = true;
                break;
            }
        }

        // 如果没有系统消息，在开头添加一个
        if (!hasSystemMessage) {
            messages.add(0, SystemMessage.from(toolCallGuidance.trim()));
        }

        log.debug("已添加工具调用引导提示词，工具数量: {}", toolSpecs.size());
    }
    
    /**
     * 从数据库加载会话历史消息
     * 忽略 SYSTEM 类型的消息
     * 
     * @param sessionId 会话ID
     * @param readCount 读取条数（排除 SYSTEM 消息后的数量）
     * @param messageId 触发工作流的消息ID（可为null，用于时间过滤）
     * @return 历史消息列表（按时间正序）
     */
    private List<ChatMessage> loadHistoryFromDatabase(UUID sessionId, int readCount, UUID messageId) {
        List<ChatMessage> historyMessages = new ArrayList<>();
        
        // 使用公共的历史消息加载器
        List<Message> dbMessages = historyMessageLoader.loadHistoryMessages(sessionId, readCount, messageId);
        
        // 转换为 ChatMessage 格式
        for (Message msg : dbMessages) {
            // SenderType.USER 为用户消息，其他为客服/AI消息
            if (msg.getSenderType() == SenderType.USER) {
                historyMessages.add(UserMessage.from(msg.getText()));
            } else {
                // AGENT, AI 作为 assistant 消息
                historyMessages.add(AiMessage.from(msg.getText()));
            }
        }
        
        return historyMessages;
    }

    /**
     * 获取绑定的工具ID列表
     */
    private List<UUID> getToolIds(JsonNode config) {
        List<UUID> toolIds = new ArrayList<>();

        if (config != null && config.has("tools")) {
            JsonNode toolsNode = config.get("tools");
            if (toolsNode.isArray()) {
                for (JsonNode toolNode : toolsNode) {
                    try {
                        toolIds.add(UUID.fromString(toolNode.asText()));
                    } catch (Exception e) {
                        log.warn("无效的工具ID: {}", toolNode.asText());
                    }
                }
            }
        }

        return toolIds;
    }

    /**
     * 构建输入信息（用于日志记录）
     */
    private String buildInputInfo(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : messages) {
            if (msg instanceof SystemMessage) {
                sb.append("[System] ").append(((SystemMessage) msg).text()).append("\n");
            } else if (msg instanceof UserMessage) {
                sb.append("[User] ").append(((UserMessage) msg).singleText()).append("\n");
            } else if (msg instanceof AiMessage) {
                sb.append("[Assistant] ").append(((AiMessage) msg).text()).append("\n");
            } else if (msg instanceof ToolExecutionResultMessage tem) {
                sb.append("[ToolResult] ").append(tem.toolName()).append(": ").append(tem.text()).append("\n");
            }
        }
        return sb.toString();
    }

    private UUID parseModelId(String modelIdStr) {
        if (modelIdStr == null || modelIdStr.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(modelIdStr);
        } catch (Exception e) {
            log.warn("无效的模型ID: {}", modelIdStr);
            return null;
        }
    }
}
