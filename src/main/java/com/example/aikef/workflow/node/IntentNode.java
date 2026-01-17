package com.example.aikef.workflow.node;

import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.model.Message;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.workflow.context.WorkflowContext;
import com.example.aikef.workflow.util.HistoryMessageLoader;
import com.example.aikef.workflow.util.TemplateEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeSwitchComponent;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 意图识别+路由节点（Switch 类型）
 * 分析用户消息识别意图，并直接路由到对应的处理节点
 * 
 * 节点配置示例：
 * {
 *   "data": {
 *     "label": "My Intent",
 *     "config": {
 *       "modelId": "xxx",           // 可选：用于 LLM 识别的模型ID
 *       "recognitionType": "llm",   // 识别方式: llm / keyword
 *       "historyCount": 5,          // 可选：获取历史聊天记录条数，默认0（不使用历史记录）
 *       "customPrompt": "请特别注意用户的情绪和语气。当前用户输入：{{sys.query}}",  // 可选：自定义提示，会追加到系统提示词中，支持模板变量（如 {{sys.query}}, {{sys.intent}} 等）
 *       "Intents": [
 *         {
 *           "id": "c1764337030732",  // sourceHandle，用于路由
 *           "label": "用户要退款"     // 意图描述，用于 LLM 分析
 *         },
 *         {
 *           "id": "c1764337031100",
 *           "label": "用户要差评"
 *         }
 *       ],
 *       "defaultRouteId": "default" // 默认路由的 sourceHandle
 *     }
 *   }
 * }
 * 
 * 边配置示例：
 * [
 *   { "source": "intent_1", "target": "refund_llm", "sourceHandle": "c1764337030732" },
 *   { "source": "intent_1", "target": "complaint_llm", "sourceHandle": "c1764337031100" },
 *   { "source": "intent_1", "target": "default_llm", "sourceHandle": "default" }
 * ]
 * 
 * LiteFlow EL 表达式：
 * SWITCH(intent_1).TO(refund_llm, complaint_llm, default_llm)
 */
@LiteflowComponent("intent")
public class IntentNode extends NodeSwitchComponent {

    private static final Logger log = LoggerFactory.getLogger(IntentNode.class);

    @Resource
    private LangChainChatService langChainChatService;

    @Resource
    private HistoryMessageLoader historyMessageLoader;

    @Override
    public String processSwitch() throws Exception {

        log.info("开始执行意图识别节点");
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = this.getContextBean(WorkflowContext.class);
        String actualNodeId = BaseWorkflowNode.resolveActualNodeId(this.getTag(), this.getNodeId(), ctx);
        JsonNode config = ctx.getNodeConfig(actualNodeId);
        
        String userMessage = ctx.getQuery();
        ctx.setOutput(this.getTag(),ctx.getLastOutput());
        // 1. 获取意图配置列表
        JsonNode intentsConfig = config != null ? config.get("Intents") : null;
        if (intentsConfig == null) {
            intentsConfig = config != null ? config.get("intents") : null;
        }
        
        if (intentsConfig == null || !intentsConfig.isArray() || intentsConfig.isEmpty()) {
            log.warn("意图配置为空，使用默认路由");
            return getDefaultRouteId(config);
        }
        
        // 2. 获取历史聊天记录（如果配置了 historyCount）
        List<LangChainChatService.ChatHistoryMessage> chatHistory = new ArrayList<>();
        int historyCount = BaseWorkflowNode.readConfigInt(config, "historyCount", 0);
        boolean useHistoryOnly = historyCount >= 1; // 如果 historyCount >= 1，只使用历史记录
        if (historyCount > 0 && ctx.getSessionId() != null) {
            chatHistory = loadHistoryMessages(ctx.getSessionId(), historyCount, ctx.getMessageId());
            log.debug("加载了 {} 条历史消息用于意图识别", chatHistory.size());
            
            // 如果配置了使用历史记录但没有获取到历史消息，使用默认路由
            if (useHistoryOnly && chatHistory.isEmpty()) {
                log.warn("配置了 historyCount >= 1 但未获取到历史消息，使用默认路由");
                return getDefaultRouteId(config);
            }
        }
        
        // 如果 useHistoryOnly 为 false，将当前用户消息添加到历史记录中
//        if (!useHistoryOnly) {
//            chatHistory.add(new LangChainChatService.ChatHistoryMessage("user", userMessage));
//        }
        
        // 3. 识别意图，返回匹配的意图 id（即 sourceHandle）
        String recognitionType = BaseWorkflowNode.readConfigString(config, "recognitionType", "llm");
        String matchedIntentId;
        String matchedIntentLabel;
        double confidence;
        
        switch (recognitionType) {
            case "keyword" -> {
                var result = recognizeByKeyword(userMessage, chatHistory, useHistoryOnly, intentsConfig);
                matchedIntentId = result.id;
                matchedIntentLabel = result.label;
                confidence = result.confidence;
            }
            case "llm" -> {
                var result = recognizeByLlm(chatHistory, useHistoryOnly, intentsConfig, config, ctx);
                matchedIntentId = result.id;
                matchedIntentLabel = result.label;
                confidence = result.confidence;
            }
            default -> {
                var result = recognizeByLlm(chatHistory, useHistoryOnly, intentsConfig, config, ctx);
                matchedIntentId = result.id;
                matchedIntentLabel = result.label;
                confidence = result.confidence;
            }
        }
        
        // 4. 设置意图到上下文（供后续节点使用）
        ctx.setIntent(matchedIntentLabel);
        ctx.setIntentConfidence(confidence);
        ctx.setVariable("matchedIntentId", matchedIntentId);
        
        // 5. 从边数据获取路由映射（使用实际节点 ID）
        String routesKey = "__intent_routes_" + actualNodeId;
        @SuppressWarnings("unchecked")
        Map<String, String> routeKeyToNode = ctx.getVariable(routesKey);
        
        // 6. 根据意图 id 找到目标节点
        String targetNodeId = null;
        
        if (routeKeyToNode != null && !routeKeyToNode.isEmpty()) {
            targetNodeId = routeKeyToNode.get(matchedIntentId);
            
            // 如果找不到，使用默认路由
            if (targetNodeId == null) {
                String defaultRouteId = getDefaultRouteId(config);
                targetNodeId = routeKeyToNode.get(defaultRouteId);
            }
            
            // 最后兜底
            if (targetNodeId == null) {
                targetNodeId = routeKeyToNode.values().iterator().next();
            }
        }
        
        if (targetNodeId == null) {
            targetNodeId = "default";
        }
        
        log.info("意图识别+路由: userMessage={}, matchedIntent={}({}), confidence={}, targetNode={}", 
                userMessage, matchedIntentLabel, matchedIntentId, confidence, targetNodeId);
        
        // 记录执行详情
        BaseWorkflowNode.recordExecution(
                ctx,
                actualNodeId,
                this.getNodeId(),
                this.getName(),
                userMessage,
                Map.of(
                        "intentId", matchedIntentId,
                        "intentLabel", matchedIntentLabel,
                        "confidence", confidence,
                        "targetNode", targetNodeId
                ),
                startTime,
                true,
                null
        );
        
        // 返回 tag:targetNodeId 格式，LiteFlow SWITCH 会匹配 TO 列表中 tag 为 targetNodeId 的节点
        return "tag:" + targetNodeId;
    }

    /**
     * 使用 LLM 识别意图
     */
    private IntentMatchResult recognizeByLlm(List<LangChainChatService.ChatHistoryMessage> chatHistory, 
                                             boolean useHistoryOnly, JsonNode intentsConfig, 
                                             JsonNode config, WorkflowContext ctx) {
        // 构建意图列表描述
        StringBuilder intentDesc = new StringBuilder();
        Map<String, String> idToLabel = new HashMap<>();
        List<String> intentLabels = new ArrayList<>();
        
        for (JsonNode intent : intentsConfig) {
            String id = intent.has("id") ? intent.get("id").asText() : "";
            String label = intent.has("label") ? intent.get("label").asText() : "";
            
            if (!id.isEmpty() && !label.isEmpty()) {
                idToLabel.put(label, id);
                intentLabels.add(label);
                intentDesc.append("- ").append(label).append("\n");
            }
        }
        
        if (intentLabels.isEmpty()) {
            return new IntentMatchResult(getDefaultRouteId(config), "unknown", 0.0);
        }
        
        // 构建系统提示词
        String basePrompt;
        if (useHistoryOnly && !chatHistory.isEmpty()) {
            basePrompt = """
                你是一个意图分类器。根据历史对话记录，从以下意图中选择最匹配的一个。
                只返回意图名称，不要其他任何内容。如果无法匹配任何意图，返回 "unknown"。
                """ ;
        } else if (!chatHistory.isEmpty()) {
            basePrompt = """
                你是一个意图分类器。根据用户输入（包括历史对话记录和当前消息），从以下意图中选择最匹配的一个。
                只返回意图名称，不要其他任何内容。如果无法匹配任何意图，返回 "unknown"。
                """;
        } else {
            basePrompt = """
            你是一个意图分类器。根据用户输入，从以下意图中选择最匹配的一个。
            只返回意图名称，不要其他任何内容。如果无法匹配任何意图，返回 "unknown"。
            """;
        }


        String intentStr = """
                    根据上面对话记录，判断当前意图是哪个,只回答我给你的意图名称,不要其他任何内容。如果无法匹配任何意图，返回 "unknown"。
                    可选意图:
                       """+ intentDesc.toString();
        LangChainChatService.ChatHistoryMessage intentMessage = new LangChainChatService.ChatHistoryMessage("user", intentStr);
        chatHistory.add(intentMessage);
        
        // 合并自定义提示（如果配置了）
        String customPrompt = BaseWorkflowNode.readConfigString(config, "customPrompt", null);
        String systemPrompt;
        if (customPrompt != null && !customPrompt.trim().isEmpty()) {
            // 支持模板变量替换（如 {{sys.query}}, {{sys.intent}} 等）
            String renderedCustomPrompt = TemplateEngine.render(customPrompt.trim(), ctx);
            systemPrompt = basePrompt + "\n\n" + renderedCustomPrompt;
        } else {
            systemPrompt = basePrompt;
        }
        
        try {
            String modelIdStr = BaseWorkflowNode.readConfigString(config, "modelId", null);
            String modelCode = BaseWorkflowNode.readConfigString(config, "modelCode", null);
            
            LangChainChatService.LlmChatResponse response;
            
            if (modelIdStr != null && !modelIdStr.isEmpty()) {
                UUID modelId = BaseWorkflowNode.parseUuidValue(modelIdStr);
                if (modelId != null) {
                    response = langChainChatService.chatWithMessages(modelId, systemPrompt, chatHistory, 0.0, 1000);
                } else {
                    response = langChainChatService.chatWithMessages(null, systemPrompt, chatHistory, 0.0, 1000);
                }
            } else if (modelCode != null && !modelCode.isEmpty()) {
                response = langChainChatService.chatWithMessagesByCode(modelCode, systemPrompt, chatHistory, 0.0, 1000);
            } else {
                response = langChainChatService.chatWithMessages(null, systemPrompt, chatHistory, 0.0, 1000);
            }

            if (response.success()) {
                String matchedLabel = response.reply().trim();
                
                // 查找匹配的意图 id
                String matchedId = idToLabel.get(matchedLabel);
                if (matchedId != null) {
                    return new IntentMatchResult(matchedId, matchedLabel, 0.85);
                }
                
                // 模糊匹配（LLM 返回的可能不完全一致）
                for (Map.Entry<String, String> entry : idToLabel.entrySet()) {
                    if (matchedLabel.contains(entry.getKey()) || entry.getKey().contains(matchedLabel)) {
                        return new IntentMatchResult(entry.getValue(), entry.getKey(), 0.7);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("LLM 意图识别失败，回退到关键词匹配", e);
        }
        
        // 回退到关键词匹配（需要从 chatHistory 中提取最新的用户消息）
        String userMessage = "";
        if (!chatHistory.isEmpty()) {
            // 获取最后一条用户消息
            for (int i = chatHistory.size() - 1; i >= 0; i--) {
                LangChainChatService.ChatHistoryMessage msg = chatHistory.get(i);
                if ("user".equals(msg.role())) {
                    userMessage = msg.content();
                    break;
                }
            }
        }
        return recognizeByKeyword(userMessage, chatHistory, useHistoryOnly, intentsConfig);
    }

    /**
     * 使用关键词匹配识别意图
     */
    private IntentMatchResult recognizeByKeyword(String userMessage, List<LangChainChatService.ChatHistoryMessage> chatHistory, 
                                                 boolean useHistoryOnly, JsonNode intentsConfig) {
        // 构建用于匹配的文本
        String searchText;
        if (useHistoryOnly && !chatHistory.isEmpty()) {
            // 只使用历史记录，不包含当前消息
            searchText = chatHistory.stream()
                    .map(LangChainChatService.ChatHistoryMessage::content)
                    .collect(Collectors.joining(" "))
                    .toLowerCase();
        } else if (!chatHistory.isEmpty()) {
            // 合并当前消息和历史消息进行匹配
            String historyText = chatHistory.stream()
                    .map(LangChainChatService.ChatHistoryMessage::content)
                    .collect(Collectors.joining(" "));
            searchText = (userMessage + " " + historyText).toLowerCase();
        } else {
            // 只使用当前消息
            searchText = userMessage.toLowerCase();
        }
        
        String userMessageLower = userMessage.toLowerCase();
        
        for (JsonNode intent : intentsConfig) {
            String id = intent.has("id") ? intent.get("id").asText() : "";
            String label = intent.has("label") ? intent.get("label").asText() : "";
            
            if (!id.isEmpty() && !label.isEmpty()) {
                String labelLower = label.toLowerCase();
                
                if (useHistoryOnly && !chatHistory.isEmpty()) {
                    // 只使用历史记录匹配
                    if (searchText.contains(labelLower)) {
                        return new IntentMatchResult(id, label, 0.7);
                    }
                    
                    // 分词匹配
                    String[] keywords = label.split("[，,、\\s]+");
                    for (String keyword : keywords) {
                        if (keyword.length() >= 2 && searchText.contains(keyword.toLowerCase())) {
                            return new IntentMatchResult(id, label, 0.5);
                        }
                    }
                } else {
                    // 优先匹配当前消息，其次匹配历史消息
                    if (userMessageLower.contains(labelLower) || 
                        labelLower.contains(userMessageLower)) {
                    return new IntentMatchResult(id, label, 0.8);
                }
                    
                    // 在历史消息中匹配
                    if (searchText.contains(labelLower)) {
                        return new IntentMatchResult(id, label, 0.7);
                }
                
                // 分词匹配
                String[] keywords = label.split("[，,、\\s]+");
                for (String keyword : keywords) {
                        if (keyword.length() >= 2) {
                            if (userMessageLower.contains(keyword.toLowerCase())) {
                        return new IntentMatchResult(id, label, 0.6);
                            }
                            if (searchText.contains(keyword.toLowerCase())) {
                                return new IntentMatchResult(id, label, 0.5);
                            }
                        }
                    }
                }
            }
            
            // 检查是否有 keywords 配置
            JsonNode keywords = intent.get("keywords");
            if (keywords != null && keywords.isArray()) {
                for (JsonNode kw : keywords) {
                    String keyword = kw.asText().toLowerCase();
                    if (useHistoryOnly && !chatHistory.isEmpty()) {
                        // 只使用历史记录匹配
                        if (searchText.contains(keyword)) {
                            return new IntentMatchResult(id, label, 0.8);
                        }
                    } else {
                        // 优先匹配当前消息
                        if (userMessageLower.contains(keyword)) {
                        return new IntentMatchResult(id, label, 0.9);
                        }
                        if (searchText.contains(keyword)) {
                            return new IntentMatchResult(id, label, 0.8);
                        }
                    }
                }
            }
        }
        
        return new IntentMatchResult("default", "unknown", 0.0);
    }

    /**
     * 从数据库加载会话历史消息
     * 忽略 SYSTEM 类型的消息
     * 
     * @param sessionId 会话ID
     * @param readCount 读取条数（排除 SYSTEM 消息后的数量）
     * @param messageId 触发工作流的消息ID（可为null，用于时间过滤）
     * @return 历史消息列表（按时间正序，ChatHistoryMessage 格式）
     */
    private List<LangChainChatService.ChatHistoryMessage> loadHistoryMessages(UUID sessionId, int readCount, UUID messageId) {
        List<LangChainChatService.ChatHistoryMessage> historyMessages = new ArrayList<>();
        
        // 使用公共的历史消息加载器
        List<Message> dbMessages = historyMessageLoader.loadHistoryMessages(sessionId, readCount, messageId);
        
        // 转换为 ChatHistoryMessage 格式
        for (Message msg : dbMessages) {
            String role;
            if (msg.getSenderType() == SenderType.USER) {
                role = "user";
            } else if(SenderType.AGENT.equals(msg.getSenderType())) {
                role = "assistant";
            }else{
                continue;
            }
            
            historyMessages.add(new LangChainChatService.ChatHistoryMessage(role, msg.getText()));
        }
        
        return historyMessages;
    }

    private String getDefaultRouteId(JsonNode config) {
        return BaseWorkflowNode.readConfigString(config, "defaultRouteId", "default");
    }

    /**
     * 意图匹配结果
     */
    private record IntentMatchResult(String id, String label, double confidence) {}
}
