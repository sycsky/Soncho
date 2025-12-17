package com.example.aikef.workflow.node;

import com.example.aikef.extraction.model.FieldDefinition;
import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.llm.LlmModelService;
import com.example.aikef.model.Message;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.repository.LlmModelRepository;
import com.example.aikef.tool.model.AiTool;
import com.example.aikef.tool.repository.AiToolRepository;
import com.example.aikef.workflow.context.WorkflowContext;
import com.example.aikef.workflow.util.HistoryMessageLoader;
import com.example.aikef.workflow.util.TemplateEngine;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeSwitchComponent;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 参数提取节点（Switch 类型）
 * 需要绑定工具，配置提取的参数（包括参数说明）
 * 类似 LLM 节点，需要配置模型、可读取的聊天记录条数
 * 类似意图节点，有两条固定流转路径：
 * - "tag:success": 参数全部满足时（将提取的参数设置到 toolsParams，key 是工具 name）
 * - "tag:fail": 未满足参数时（生成提示提醒用户需要提供什么）
 * 
 * 配置示例:
 * {
 *   "toolId": "uuid-of-tool",
 *   "toolName": "tool_name",  // 可选，如果提供则优先使用 toolName 查找工具
 *   "modelId": "uuid-of-model",  // 可选，用于参数提取的模型ID
 *   "modelCode": "gpt-4",  // 可选，模型代码
 *   "historyCount": 10,  // 历史聊天记录条数（默认0，不使用历史记录）
 *   "extractParams": ["param1", "param2"],  // 可选，指定要提取的参数列表，如果不提供则提取所有必填参数
 *   "systemPrompt": "请从对话中提取以下信息：{{sys.query}}",  // 可选，系统提示词（支持模板变量）
 *   "messages": [  // 可选，自定义消息列表（支持模板变量）
 *     { "role": "user", "content": "{{sys.query}}" },
 *     { "role": "assistant", "content": "{{sys.lastOutput}}" }
 *   ]
 * }
 */
@LiteflowComponent("parameter_extraction")
public class ParamExtractNode extends NodeSwitchComponent {

    private static final Logger log = LoggerFactory.getLogger(ParamExtractNode.class);

    @Resource
    private AiToolRepository toolRepository;

    @Resource
    private LangChainChatService chatService;

    @Resource
    private LlmModelService llmModelService;

    @Resource
    private LlmModelRepository llmModelRepository;

    @Resource
    private HistoryMessageLoader historyMessageLoader;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public String processSwitch() throws Exception {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = this.getContextBean(WorkflowContext.class);
        String actualNodeId = getActualNodeId();
        JsonNode config = ctx.getNodeConfig(actualNodeId);

        // 获取工具ID或工具名称
        String toolIdStr = getConfigValue(config, "toolId", null);
        String toolName = getConfigValue(config, "toolName", null);

        if (toolIdStr == null && toolName == null) {
            log.error("参数提取节点未配置 toolId 或 toolName");
            recordExecution(ctx, actualNodeId, null, "error", startTime, false, "toolId or toolName not configured");
            return "tag:fail";
        }

        // 查找工具
        AiTool tool = null;
        if (toolName != null && !toolName.isEmpty()) {
            tool = toolRepository.findByNameWithSchema(toolName).orElse(null);
        }
        if (tool == null && toolIdStr != null && !toolIdStr.isEmpty()) {
            tool = toolRepository.findByIdWithSchema(UUID.fromString(toolIdStr)).orElse(null);
        }

        if (tool == null) {
            log.error("工具不存在: toolId={}, toolName={}", toolIdStr, toolName);
            recordExecution(ctx, actualNodeId, null, "error", startTime, false, "tool not found");
            return "tag:fail";
        }

        if (tool.getSchema() == null) {
            log.error("工具没有参数定义: toolName={}", tool.getName());
            recordExecution(ctx, actualNodeId, tool.getName(), "error", startTime, false, "tool has no schema");
            return "tag:fail";
        }

        // 获取工具参数定义
        List<FieldDefinition> allParamDefs = getToolParameters(tool);
        if (allParamDefs.isEmpty()) {
            log.info("工具没有参数，直接返回 success: toolName={}", tool.getName());
            // 没有参数，直接设置空参数并返回 success
            ctx.setToolParams(tool.getName(), new HashMap<>());
            recordExecution(ctx, actualNodeId, tool.getName(), "tag:success", startTime, true, null);
            return "tag:success";
        }

        // 获取要提取的参数列表（如果配置了 extractParams，则只提取指定的参数）
        List<String> extractParamNamesList = null;
        JsonNode extractParamsNode = config.get("extractParams");
        if (extractParamsNode != null && extractParamsNode.isArray()) {
            extractParamNamesList = new ArrayList<>();
            for (JsonNode paramNode : extractParamsNode) {
                extractParamNamesList.add(paramNode.asText());
            }
        }

        // 确定要提取的参数
        // 创建 final 变量供 lambda 使用
        final List<String> extractParamNames = extractParamNamesList;
        List<FieldDefinition> targetParamDefs;
        if (extractParamNames != null && !extractParamNames.isEmpty()) {
            // 只提取指定的参数
            targetParamDefs = allParamDefs.stream()
                    .filter(f -> extractParamNames.contains(f.getName()))
                    .collect(Collectors.toList());
        } else {
            // 提取所有必填参数
            targetParamDefs = allParamDefs.stream()
                    .filter(FieldDefinition::isRequired)
                    .collect(Collectors.toList());
        }

        if (targetParamDefs.isEmpty()) {
            log.info("没有需要提取的参数: toolName={}", tool.getName());
            ctx.setToolParams(tool.getName(), new HashMap<>());
            recordExecution(ctx, actualNodeId, tool.getName(), "tag:success", startTime, true, null);
            return "tag:success";
        }

        // 获取历史聊天记录
        List<LangChainChatService.ChatHistoryMessage> chatHistory = new ArrayList<>();
        int historyCount = getConfigInt(config, "readCount", 0);
        if (historyCount > 0 && ctx.getSessionId() != null) {
            chatHistory = loadHistoryMessages(ctx.getSessionId(), historyCount, ctx.getMessageId());
            log.debug("加载了 {} 条历史消息用于参数提取", chatHistory.size());
        }

        // 获取当前用户消息
        String userMessage = ctx.getQuery();
        if (userMessage == null || userMessage.isEmpty()) {
            userMessage = "";
        }

        // 构建系统提示词（支持模板变量）
        String customPrompt = getConfigValue(config, "systemPrompt", null);
//        String systemPrompt = buildExtractPrompt(targetParamDefs, customPrompt, chatHistory, userMessage, ctx);

        // 获取模型ID
        String modelIdStr = getConfigValue(config, "modelId", null);
        String modelCode = getConfigValue(config, "modelCode", null);
        UUID modelId = null;
        if (modelIdStr != null && !modelIdStr.isEmpty()) {
            modelId = UUID.fromString(modelIdStr);
        } else if (modelCode != null && !modelCode.isEmpty()) {
            // 通过 modelCode 查找模型
            var modelOpt = llmModelRepository.findByCode(modelCode);
            if (modelOpt.isPresent()) {
                modelId = modelOpt.get().getId();
            }
        }
        if (modelId == null) {
            // 使用默认模型
            var defaultModel = llmModelService.getDefaultModel();
            if (defaultModel.isPresent()) {
                modelId = defaultModel.get().getId();
            }
        }

        // 构建系统提示词（包含参数说明）
        String systemPrompt = buildSystemPrompt(customPrompt, targetParamDefs, ctx);
        
        // 构建消息列表（支持模板变量注入）
        List<LangChainChatService.ChatHistoryMessage> chatHistoryMessages = new ArrayList<>();
        
        // 1. 添加配置中的 messages（自定义消息，支持模板变量注入）
        JsonNode messagesConfig = config != null ? config.get("messages") : null;
        if (messagesConfig != null && messagesConfig.isArray() && !messagesConfig.isEmpty()) {
            for (JsonNode msgNode : messagesConfig) {
                String role = msgNode.has("role") ? msgNode.get("role").asText("user") : "user";
                String content = msgNode.has("content") ? msgNode.get("content").asText("") : "";

                // 使用模板引擎处理消息内容，支持环境变量注入
                content = renderTemplate(content);

                if (!content.isEmpty()) {
                    chatHistoryMessages.add(new LangChainChatService.ChatHistoryMessage(role, content));
                }
            }
        }
        
        // 2. 添加历史消息（从数据库加载的）
        for (LangChainChatService.ChatHistoryMessage historyMsg : chatHistory) {
            chatHistoryMessages.add(historyMsg);
        }
        
        // 3. 添加当前用户消息（支持模板变量注入）
        if (userMessage != null && !userMessage.isEmpty()) {
            String renderedUserMessage = renderTemplate(userMessage);
            if (!renderedUserMessage.isEmpty()) {
                chatHistoryMessages.add(new LangChainChatService.ChatHistoryMessage("user", renderedUserMessage));
            }
        }

        // 执行参数提取（使用纯文本方式）
        try {
            log.info("开始提取参数: toolName={}, targetParams={}, modelId={}, historyCount={}", 
                    tool.getName(), 
                    targetParamDefs.stream().map(FieldDefinition::getName).collect(Collectors.toList()),
                    modelId,
                    chatHistoryMessages.size());

            // 使用纯文本聊天方式，不使用结构化输出
            LangChainChatService.LlmChatResponse response = chatService.chatWithMessages(
                    modelId,
                    systemPrompt,
                    chatHistoryMessages,
                    null, // temperature
                    null  // maxTokens
            );
            
            if (!response.success() || response.reply() == null || response.reply().trim().isEmpty()) {
                log.warn("参数提取返回失败或为空: toolName={}, error={}", 
                        tool.getName(), response.errorMessage());
                String errorMsg = response.errorMessage() != null ? response.errorMessage() : "参数提取失败：返回结果为空";
                setOutput(ctx, actualNodeId, errorMsg);
                recordExecution(ctx, actualNodeId, tool.getName(), "tag:fail", startTime, true, 
                        "extraction returned empty or failed");
                return "tag:fail";
            }
            
            String extractedResult = response.reply().trim();
            
            // 检查返回的是否是 JSON 格式
            boolean isJsonFormat = extractedResult.contains("{") && extractedResult.contains("}");
            
            if (!isJsonFormat) {
                // 纯文本，代表参数不满足，使用返回的文本作为输出
                log.info("参数提取返回纯文本（非JSON），视为参数不满足: toolName={}, content={}", 
                        tool.getName(), 
                        extractedResult.length() > 200 ? extractedResult.substring(0, 200) + "..." : extractedResult);
                setOutput(ctx, actualNodeId, extractedResult);
                recordExecution(ctx, actualNodeId, tool.getName(), "tag:fail", startTime, true, 
                        "parameters not satisfied, returned prompt text");
                return "tag:fail";
            }
            
            // 尝试解析提取的JSON
            Map<String, Object> extractedParams = new HashMap<>();
            try {
                // 提取 JSON 部分（可能包含其他文本）
                int start = extractedResult.indexOf("{");
                int end = extractedResult.lastIndexOf("}") + 1;
                if (start >= 0 && end > start) {
                    String json = extractedResult.substring(start, end);
                    extractedParams = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                } else {
                    // 无法找到有效的 JSON 结构，视为纯文本
                    log.info("无法从返回结果中提取有效JSON，视为参数不满足: toolName={}, content={}", 
                            tool.getName(), 
                            extractedResult.length() > 200 ? extractedResult.substring(0, 200) + "..." : extractedResult);
                    setOutput(ctx, actualNodeId, extractedResult);
                    recordExecution(ctx, actualNodeId, tool.getName(), "tag:fail", startTime, true, 
                            "invalid JSON structure");
                    return "tag:fail";
                }
            } catch (Exception parseException) {
                // JSON 解析失败，视为纯文本
                log.info("JSON解析失败，视为参数不满足: toolName={}, error={}, content={}", 
                        tool.getName(), 
                        parseException.getMessage(),
                        extractedResult.length() > 200 ? extractedResult.substring(0, 200) + "..." : extractedResult);
                setOutput(ctx, actualNodeId, extractedResult);
                recordExecution(ctx, actualNodeId, tool.getName(), "tag:fail", startTime, true, 
                        "JSON parse failed: " + parseException.getMessage());
                return "tag:fail";
            }

            log.info("提取的参数: toolName={}, params={}", tool.getName(), extractedParams);

            // 检查必填参数是否都已提取
            List<String> requiredParams = targetParamDefs.stream()
                    .filter(FieldDefinition::isRequired)
                    .map(FieldDefinition::getName)
                    .collect(Collectors.toList());

            List<String> missingParams = new ArrayList<>();
            for (String param : requiredParams) {
                Object value = extractedParams.get(param);
                boolean isMissing = !extractedParams.containsKey(param) || value == null || 
                        "null".equals(value) || "Null".equals(value) ||
                        (value instanceof String && ((String) value).isEmpty());
                if (isMissing) {
                    missingParams.add(param);
                }
            }

            if (!missingParams.isEmpty()) {
                // 参数未满足，生成提示
                String prompt = buildMissingParamsPrompt(targetParamDefs, missingParams);
                setOutput(ctx, actualNodeId, prompt);
                recordExecution(ctx, actualNodeId, tool.getName(), "tag:fail", startTime, true, 
                        "missing params: " + missingParams);
                return "tag:fail";
            }

            // 参数全部满足，设置到 toolsParams
            ctx.setToolParams(tool.getName(), extractedParams);
            String output = "参数提取完成: " + extractedParams.keySet();
            setOutput(ctx, actualNodeId, output);
            recordExecution(ctx, actualNodeId, tool.getName(), "tag:success", startTime, true, null);
            return "tag:success";

        } catch (Exception e) {
            log.error("参数提取失败: toolName={}", tool.getName(), e);
            String errorMsg = "参数提取失败: " + e.getMessage();
            setOutput(ctx, actualNodeId, errorMsg);
            recordExecution(ctx, actualNodeId, tool.getName(), "error", startTime, false, e.getMessage());
            return "tag:fail";
        }
    }

    /**
     * 构建系统提示词（包含参数说明和提取规则）
     */
    private String buildSystemPrompt(String customPrompt, List<FieldDefinition> targetParamDefs, WorkflowContext ctx) {
        StringBuilder prompt = new StringBuilder();
        
        // 1. 自定义提示词（支持模板变量注入）
        if (customPrompt != null && !customPrompt.isEmpty()) {
            String renderedPrompt = renderTemplate(customPrompt);
            prompt.append(renderedPrompt).append("\n\n");
        } else {
            prompt.append("请从对话中提取以下信息。\n\n");
        }
        
        // 2. 添加需要提取的字段说明
        prompt.append("需要提取的字段：\n");
        for (FieldDefinition field : targetParamDefs) {
            prompt.append("- ").append(field.getName()).append(": ");
            if (field.getDescription() != null && !field.getDescription().isEmpty()) {
                prompt.append(field.getDescription());
            } else if (field.getDisplayName() != null && !field.getDisplayName().isEmpty()) {
                prompt.append(field.getDisplayName());
            } else {
                prompt.append(field.getName());
            }
            if (Boolean.TRUE.equals(field.isRequired())) {
                prompt.append(" (必填)");
            }
            prompt.append("\n");
        }
        
        // 3. 添加提取规则说明
        prompt.append("\n提取规则：\n");
        prompt.append("1. 如果所有必填参数都能从对话中提取到，请返回一个 JSON 对象，格式如下：\n");
        prompt.append("   {\n");
        for (FieldDefinition field : targetParamDefs) {
            prompt.append("     \"").append(field.getName()).append("\": \"提取的值\"");
            if (field != targetParamDefs.get(targetParamDefs.size() - 1)) {
                prompt.append(",");
            }
            prompt.append("\n");
        }
        prompt.append("   }\n");
        prompt.append("2. 如果有任何必填参数无法从对话中提取到，请直接返回提示文本（不要返回 JSON），\n");
        prompt.append("   提示用户需要提供哪些缺失的信息。\n");
        prompt.append("3. 如果某个字段无法提取，在 JSON 中设为 null。\n");
        prompt.append("4. 只返回 JSON 对象或提示文本，不要有其他解释性文字。\n");
        
        return prompt.toString();
    }

    /**
     * 渲染模板（支持环境变量注入）
     */
    private String renderTemplate(String template) {
        WorkflowContext ctx = this.getContextBean(WorkflowContext.class);
        return TemplateEngine.render(template, ctx);
    }

    /**
     * 构建缺失参数提示
     */
    private String buildMissingParamsPrompt(List<FieldDefinition> allParamDefs, List<String> missingParams) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("为了完成操作，我还需要以下信息：\n\n");

        for (String paramName : missingParams) {
            FieldDefinition field = allParamDefs.stream()
                    .filter(f -> f.getName().equals(paramName))
                    .findFirst()
                    .orElse(null);

            if (field != null) {
                prompt.append("- ");
                if (field.getDisplayName() != null && !field.getDisplayName().isEmpty()) {
                    prompt.append(field.getDisplayName());
                } else {
                    prompt.append(paramName);
                }
                if (field.getDescription() != null && !field.getDescription().isEmpty()) {
                    prompt.append("：").append(field.getDescription());
                }
                prompt.append("\n");
            } else {
                prompt.append("- ").append(paramName).append("\n");
            }
        }

        prompt.append("\n请提供上述信息。");
        return prompt.toString();
    }

    /**
     * 加载历史消息
     * @param sessionId 会话ID
     * @param readCount 读取数量
     * @param messageId 触发工作流的消息ID（可为null，用于时间过滤）
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
            } else {
                role = "assistant";
            }
            historyMessages.add(new LangChainChatService.ChatHistoryMessage(role, msg.getText()));
        }
        
        return historyMessages;
    }

    /**
     * 获取工具参数定义
     */
    private List<FieldDefinition> getToolParameters(AiTool tool) {
        if (tool.getSchema() == null || tool.getSchema().getFieldsJson() == null) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(
                    tool.getSchema().getFieldsJson(),
                    new TypeReference<List<FieldDefinition>>() {}
            );
        } catch (Exception e) {
            log.warn("解析工具参数失败: tool={}", tool.getName());
            return Collections.emptyList();
        }
    }

    /**
     * 获取配置值
     */
    private String getConfigValue(JsonNode config, String key, String defaultValue) {
        if (config == null || !config.has(key)) {
            return defaultValue;
        }
        JsonNode value = config.get(key);
        if (value.isNull()) {
            return defaultValue;
        }
        return value.asText();
    }

    /**
     * 获取配置整数值
     */
    private Integer getConfigInt(JsonNode config, String key, Integer defaultValue) {
        if (config == null || !config.has(key)) {
            return defaultValue;
        }
        JsonNode value = config.get(key);
        if (value.isNull()) {
            return defaultValue;
        }
        return value.asInt(defaultValue);
    }

    /**
     * 获取实际节点ID
     */
    private String getActualNodeId() {
        String tag = this.getTag();
        if (tag != null && !tag.isEmpty()) {
            return tag;
        }
        return this.getNodeId();
    }

    /**
     * 设置节点输出
     */
    private void setOutput(WorkflowContext ctx, String nodeId, Object output) {
        ctx.setOutput(nodeId, output);
    }

    /**
     * 记录节点执行详情
     */
    private void recordExecution(WorkflowContext ctx, String nodeId, String toolName,
                                 Object output, long startTime, boolean success, String errorMessage) {
        WorkflowContext.NodeExecutionDetail detail = new WorkflowContext.NodeExecutionDetail();
        detail.setNodeId(nodeId);
        detail.setNodeType("param_extract");
        detail.setNodeName(this.getName());
        
        // 从上下文中获取节点标签（来自 data.label）
        String nodeLabel = ctx.getNodeLabels().get(nodeId);
        detail.setNodeLabel(nodeLabel);
        
        detail.setInput(toolName);
        detail.setOutput(output);
        detail.setStartTime(startTime);
        detail.setEndTime(System.currentTimeMillis());
        detail.setDurationMs(detail.getEndTime() - startTime);
        detail.setSuccess(success);
        detail.setErrorMessage(errorMessage);
        ctx.addNodeExecutionDetail(detail);
    }
}

