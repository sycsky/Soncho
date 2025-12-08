package com.example.aikef.workflow.node;

import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.workflow.context.WorkflowContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeSwitchComponent;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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

    /**
     * 获取实际的节点 ID（ReactFlow 节点 ID）
     * EL 表达式中使用 node("componentId").tag("instanceId") 格式
     * 通过 getTag() 获取 instanceId
     */
    private String getActualNodeId() {
        // 使用 tag 获取 ReactFlow 节点 ID
        String tag = this.getTag();
        if (tag != null && !tag.isEmpty()) {
            return tag;
        }
        // 回退到 nodeId
        return this.getNodeId();
    }

    @Override
    public String processSwitch() throws Exception {

        log.info("开始执行意图识别节点");
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = this.getContextBean(WorkflowContext.class);
        String actualNodeId = getActualNodeId();
        JsonNode config = ctx.getNodeConfig(actualNodeId);
        
        String userMessage = ctx.getQuery();
        
        // 1. 获取意图配置列表
        JsonNode intentsConfig = config != null ? config.get("Intents") : null;
        if (intentsConfig == null) {
            intentsConfig = config != null ? config.get("intents") : null;
        }
        
        if (intentsConfig == null || !intentsConfig.isArray() || intentsConfig.isEmpty()) {
            log.warn("意图配置为空，使用默认路由");
            return getDefaultRouteId(config);
        }
        
        // 2. 识别意图，返回匹配的意图 id（即 sourceHandle）
        String recognitionType = getConfigValue(config, "recognitionType", "llm");
        String matchedIntentId;
        String matchedIntentLabel;
        double confidence;
        
        switch (recognitionType) {
            case "keyword" -> {
                var result = recognizeByKeyword(userMessage, intentsConfig);
                matchedIntentId = result.id;
                matchedIntentLabel = result.label;
                confidence = result.confidence;
            }
            case "llm" -> {
                var result = recognizeByLlm(userMessage, intentsConfig, config);
                matchedIntentId = result.id;
                matchedIntentLabel = result.label;
                confidence = result.confidence;
            }
            default -> {
                var result = recognizeByLlm(userMessage, intentsConfig, config);
                matchedIntentId = result.id;
                matchedIntentLabel = result.label;
                confidence = result.confidence;
            }
        }
        
        // 3. 设置意图到上下文（供后续节点使用）
        ctx.setIntent(matchedIntentLabel);
        ctx.setIntentConfidence(confidence);
        ctx.setVariable("matchedIntentId", matchedIntentId);
        
        // 4. 从边数据获取路由映射（使用实际节点 ID）
        String routesKey = "__intent_routes_" + actualNodeId;
        @SuppressWarnings("unchecked")
        Map<String, String> routeKeyToNode = ctx.getVariable(routesKey);
        
        // 5. 根据意图 id 找到目标节点
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
        recordExecution(ctx, userMessage, matchedIntentId, matchedIntentLabel, confidence, targetNodeId, startTime);
        
        // 返回 tag:targetNodeId 格式，LiteFlow SWITCH 会匹配 TO 列表中 tag 为 targetNodeId 的节点
        return "tag:" + targetNodeId;
    }

    /**
     * 使用 LLM 识别意图
     */
    private IntentMatchResult recognizeByLlm(String userMessage, JsonNode intentsConfig, JsonNode config) {
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
        
        String systemPrompt = """
            你是一个意图分类器。根据用户输入，从以下意图中选择最匹配的一个。
            只返回意图名称，不要其他任何内容。如果无法匹配任何意图，返回 "unknown"。
            
            可选意图:
            """ + intentDesc.toString();
        
        try {
            String modelIdStr = getConfigValue(config, "modelId", null);
            String modelCode = getConfigValue(config, "modelCode", null);
            
            LangChainChatService.LlmChatResponse response;
            
            if (modelIdStr != null && !modelIdStr.isEmpty()) {
                UUID modelId = UUID.fromString(modelIdStr);
                response = langChainChatService.chat(modelId, systemPrompt, userMessage, null, 0.1, 100);
            } else if (modelCode != null && !modelCode.isEmpty()) {
                response = langChainChatService.chatByCode(modelCode, systemPrompt, userMessage, null, 0.1, 100);
            } else {
                response = langChainChatService.chat(null, systemPrompt, userMessage, null, 0.1, 100);
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
        
        // 回退到关键词匹配
        return recognizeByKeyword(userMessage, intentsConfig);
    }

    /**
     * 使用关键词匹配识别意图
     */
    private IntentMatchResult recognizeByKeyword(String userMessage, JsonNode intentsConfig) {
        String messageLower = userMessage.toLowerCase();
        
        for (JsonNode intent : intentsConfig) {
            String id = intent.has("id") ? intent.get("id").asText() : "";
            String label = intent.has("label") ? intent.get("label").asText() : "";
            
            if (!id.isEmpty() && !label.isEmpty()) {
                // 简单的关键词包含匹配
                if (messageLower.contains(label.toLowerCase()) || 
                    label.toLowerCase().contains(messageLower)) {
                    return new IntentMatchResult(id, label, 0.8);
                }
                
                // 分词匹配
                String[] keywords = label.split("[，,、\\s]+");
                for (String keyword : keywords) {
                    if (keyword.length() >= 2 && messageLower.contains(keyword.toLowerCase())) {
                        return new IntentMatchResult(id, label, 0.6);
                    }
                }
            }
            
            // 检查是否有 keywords 配置
            JsonNode keywords = intent.get("keywords");
            if (keywords != null && keywords.isArray()) {
                for (JsonNode kw : keywords) {
                    if (messageLower.contains(kw.asText().toLowerCase())) {
                        return new IntentMatchResult(id, label, 0.9);
                    }
                }
            }
        }
        
        return new IntentMatchResult("default", "unknown", 0.0);
    }

    private String getConfigValue(JsonNode config, String key, String defaultValue) {
        if (config != null && config.has(key) && !config.get(key).isNull()) {
            return config.get(key).asText(defaultValue);
        }
        return defaultValue;
    }

    private String getDefaultRouteId(JsonNode config) {
        return getConfigValue(config, "defaultRouteId", "default");
    }

    private void recordExecution(WorkflowContext ctx, String userMessage, String intentId, 
                                  String intentLabel, double confidence, String targetNode, long startTime) {
        WorkflowContext.NodeExecutionDetail detail = new WorkflowContext.NodeExecutionDetail();
        detail.setNodeId(this.getNodeId());
        detail.setNodeType("intent");
        detail.setNodeName(this.getName());
        detail.setInput(userMessage);
        detail.setOutput(Map.of(
            "intentId", intentId,
            "intentLabel", intentLabel,
            "confidence", confidence,
            "targetNode", targetNode
        ));
        detail.setStartTime(startTime);
        detail.setEndTime(System.currentTimeMillis());
        detail.setDurationMs(detail.getEndTime() - startTime);
        detail.setSuccess(true);
        ctx.addNodeExecutionDetail(detail);
    }

    /**
     * 意图匹配结果
     */
    private record IntentMatchResult(String id, String label, double confidence) {}
}


