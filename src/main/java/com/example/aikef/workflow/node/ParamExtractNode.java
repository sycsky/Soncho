package com.example.aikef.workflow.node;

import com.example.aikef.extraction.model.FieldDefinition;
import com.example.aikef.llm.LangChainChatService;
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
 * - "tag:incomplete": 未满足参数时（生成提示提醒用户需要提供什么）
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
    private LangChainChatService langChainChatService;

    @Resource
    private HistoryMessageLoader historyMessageLoader;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public String processSwitch() throws Exception {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = this.getContextBean(WorkflowContext.class);
        String actualNodeId = BaseWorkflowNode.resolveActualNodeId(this.getTag(), this.getNodeId(), ctx);
        JsonNode config = ctx.getNodeConfig(actualNodeId);

        if (config == null) {
            log.error("参数提取节点配置为空: nodeId={}", actualNodeId);
            return "tag:incomplete";
        }

        // 获取工具ID或工具名称
        String toolIdStr = BaseWorkflowNode.readConfigString(config, "toolId", null);
        String toolName = BaseWorkflowNode.readConfigString(config, "toolName", null);

        if (toolIdStr == null && toolName == null) {
            log.error("参数提取节点未配置 toolId 或 toolName");
            return "tag:incomplete";
        }

        // 查找工具
        AiTool tool = null;
        if (toolName != null && !toolName.isEmpty()) {
            tool = toolRepository.findByNameWithSchema(toolName).orElse(null);
        }
        if (tool == null) {
            UUID toolId = BaseWorkflowNode.parseUuidValue(toolIdStr);
            if (toolIdStr != null && !toolIdStr.isEmpty() && toolId == null) {
                log.warn("无效的工具ID: {}", toolIdStr);
            }
            if (toolId != null) {
                tool = toolRepository.findByIdWithSchema(toolId).orElse(null);
            }
        }

        if (tool == null) {
            log.error("工具不存在: toolId={}, toolName={}", toolIdStr, toolName);
            return "tag:incomplete";
        }

        // 获取工具参数定义
        List<FieldDefinition> paramDefs = getToolParameters(tool);
        if (paramDefs.isEmpty()) {
            log.warn("工具没有参数定义: toolName={}", tool.getName());
            return "tag:success"; // 没有参数，直接成功
        }

        // 获取要提取的参数列表（如果配置了 extractParams，只提取指定的参数）
        List<String> extractParamNames = getExtractParamNames(config, paramDefs);
        
        // 过滤出需要提取的参数定义
        List<FieldDefinition> extractParamDefs = paramDefs.stream()
                .filter(p -> extractParamNames.contains(p.getName()))
                .collect(Collectors.toList());

        if (extractParamDefs.isEmpty()) {
            log.warn("没有需要提取的参数");
            return "tag:success";
        }

        // 获取历史聊天记录
        List<LangChainChatService.ChatHistoryMessage> chatHistory = new ArrayList<>();
        int historyCount = BaseWorkflowNode.readConfigInt(config, "historyCount", 0);
        if (historyCount > 0 && ctx.getSessionId() != null) {
            chatHistory = loadHistoryMessages(ctx.getSessionId(), historyCount, ctx.getMessageId());
            log.debug("加载了 {} 条历史消息用于参数提取", chatHistory.size());
        }

        // 添加当前用户消息
        String userMessage = ctx.getQuery();
        if (userMessage != null && !userMessage.trim().isEmpty()) {
            chatHistory.add(new LangChainChatService.ChatHistoryMessage("user", userMessage));
        }

        // 获取模型ID
        String modelIdStr = BaseWorkflowNode.readConfigString(config, "modelId", null);
        UUID modelId = BaseWorkflowNode.parseUuidValue(modelIdStr);
        if (modelIdStr != null && !modelIdStr.isEmpty() && modelId == null) {
            log.warn("无效的模型ID: {}", modelIdStr);
        }

        // 构建系统提示词
        String systemPrompt = buildSystemPrompt(ctx, config, extractParamDefs, tool.getName());

        // 使用LLM提取参数
        Map<String, Object> extractedParams = extractParametersWithLLM(
                chatHistory, systemPrompt, extractParamDefs, modelId);

        // 检查参数完整性
        List<String> missingParams = checkParameterCompleteness(extractParamDefs, extractedParams);

        if (missingParams.isEmpty()) {
            // 所有参数都已提取，设置到 toolsParams
            ctx.setToolParams(tool.getName(), extractedParams);
            log.info("参数提取成功: toolName={}, params={}", tool.getName(), extractedParams);
            return "tag:success";
        } else {
            // 参数不完整，生成提示
            String prompt = generateMissingParamsPrompt(tool.getName(), missingParams, extractParamDefs);
            ctx.setOutput(actualNodeId, prompt);
            log.info("参数提取不完整: toolName={}, missingParams={}", tool.getName(), missingParams);
            return "tag:incomplete";
        }
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
     * 获取要提取的参数名称列表
     */
    private List<String> getExtractParamNames(JsonNode config, List<FieldDefinition> paramDefs) {
        JsonNode extractParamsNode = config.get("extractParams");
        if (extractParamsNode != null && extractParamsNode.isArray()) {
            List<String> extractParams = new ArrayList<>();
            for (JsonNode param : extractParamsNode) {
                extractParams.add(param.asText());
            }
            return extractParams;
        }
        // 如果没有配置，提取所有必填参数
        return paramDefs.stream()
                .filter(FieldDefinition::isRequired)
                .map(FieldDefinition::getName)
                .collect(Collectors.toList());
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(WorkflowContext ctx, JsonNode config, List<FieldDefinition> paramDefs, String toolName) {
        String customPrompt = BaseWorkflowNode.readConfigString(config, "systemPrompt", null);
        if (customPrompt != null && !customPrompt.trim().isEmpty()) {
            return TemplateEngine.render(customPrompt, ctx);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("你是一个参数提取助手。请从用户对话中提取调用工具 \"").append(toolName).append("\" 所需的参数。\n\n");
        sb.append("需要提取的参数：\n");
        for (FieldDefinition param : paramDefs) {
            sb.append("- ").append(param.getName());
            if (param.getDescription() != null && !param.getDescription().isEmpty()) {
                sb.append(": ").append(param.getDescription());
            }
            if (param.isRequired()) {
                sb.append(" (必填)");
            }
            sb.append("\n");
        }
        sb.append("\n请严格按照JSON格式返回提取到的参数值。如果某个参数无法从对话中提取，请返回空字符串。");
        return sb.toString();
    }

    /**
     * 使用LLM提取参数
     */
    private Map<String, Object> extractParametersWithLLM(
            List<LangChainChatService.ChatHistoryMessage> chatHistory,
            String systemPrompt,
            List<FieldDefinition> paramDefs,
            UUID modelId) {
        try {
            // 构建字段定义列表
            List<LangChainChatService.FieldSchemaDefinition> fieldDefinitions = new ArrayList<>();
            for (FieldDefinition param : paramDefs) {
                fieldDefinitions.add(new LangChainChatService.FieldSchemaDefinition(
                        param.getName(),
                        LangChainChatService.FieldSchemaDefinition.FieldType.STRING,
                        param.getDescription() != null ? param.getDescription() : param.getName(),
                        param.isRequired(),
                        null, null, null
                ));
            }

            // 构建用户消息（从聊天历史中提取）
            String userMessage = chatHistory.stream()
                    .filter(m -> "user".equals(m.role()))
                    .map(LangChainChatService.ChatHistoryMessage::content)
                    .collect(Collectors.joining("\n"));

            if (userMessage == null || userMessage.trim().isEmpty()) {
                userMessage = "请提取参数";
            }

            // 使用结构化输出提取参数
            LangChainChatService.StructuredOutputResponse response = langChainChatService.chatWithFieldDefinitions(
                    modelId,
                    systemPrompt,
                    userMessage,
                    fieldDefinitions,
                    "parameter_extraction",
                    0.3  // 使用较低温度保证输出稳定性
            );

            if (!response.success() || response.jsonResult() == null) {
                log.warn("LLM参数提取失败: {}", response.errorMessage());
                return new HashMap<>();
            }

            // 解析提取的JSON结果
            return parseJsonResponse(response.jsonResult());

        } catch (Exception e) {
            log.error("使用LLM提取参数失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 解析JSON响应
     */
    private Map<String, Object> parseJsonResponse(String jsonStr) {
        try {
            return objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("解析JSON响应失败: {}", jsonStr, e);
            return new HashMap<>();
        }
    }

    /**
     * 检查参数完整性
     */
    private List<String> checkParameterCompleteness(
            List<FieldDefinition> paramDefs, Map<String, Object> extractedParams) {
        List<String> missingParams = new ArrayList<>();
        for (FieldDefinition param : paramDefs) {
            if (param.isRequired()) {
                Object value = extractedParams.get(param.getName());
                if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                    missingParams.add(param.getName());
                }
            }
        }
        return missingParams;
    }

    /**
     * 生成缺失参数的提示
     */
    private String generateMissingParamsPrompt(
            String toolName, List<String> missingParams, List<FieldDefinition> paramDefs) {
        StringBuilder sb = new StringBuilder();
        sb.append("为了调用工具 \"").append(toolName).append("\"，还需要以下信息：\n");
        for (String paramName : missingParams) {
            FieldDefinition param = paramDefs.stream()
                    .filter(p -> p.getName().equals(paramName))
                    .findFirst()
                    .orElse(null);
            if (param != null) {
                sb.append("- ").append(paramName);
                if (param.getDescription() != null && !param.getDescription().isEmpty()) {
                    sb.append(": ").append(param.getDescription());
                }
                sb.append("\n");
            } else {
                sb.append("- ").append(paramName).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 加载历史消息
     */
    private List<LangChainChatService.ChatHistoryMessage> loadHistoryMessages(
            UUID sessionId, int count, UUID excludeMessageId) {
        List<com.example.aikef.model.Message> messages = historyMessageLoader.loadHistoryMessages(sessionId, count, excludeMessageId);
        return messages.stream()
                .map(msg -> {
                    String role = msg.getSenderType() == com.example.aikef.model.enums.SenderType.USER ? "user" : "assistant";
                    return new LangChainChatService.ChatHistoryMessage(role, msg.getText());
                })
                .collect(Collectors.toList());
    }
}
