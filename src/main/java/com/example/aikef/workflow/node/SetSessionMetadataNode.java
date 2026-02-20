package com.example.aikef.workflow.node;

import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.llm.LlmModelService;
import com.example.aikef.model.ChatSession;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.workflow.context.WorkflowContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 会话元数据设置节点
 * 使用配置的AI模型从上一个节点的输出中提取数据，设置到会话的元数据中
 * 
 * 配置示例:
 * {
 *   "modelId": "uuid",  // 可选，使用默认模型
 *   "mappings": {
 *     "sourceField1": "targetField1",
 *     "sourceField2": "targetField2"
 *   },
 *   "systemPrompt": "请从以下内容中提取信息..."  // 可选，自定义系统提示词
 * }
 * 
 * 说明：
 * - modelId: AI模型ID，用于提取数据（可选，不提供则使用默认模型）
 * - mappings: 字段映射，key 是源字段名（LLM提取的字段），value 是目标字段（会话元数据中的字段名）
 * - systemPrompt: 系统提示词（可选），用于指导LLM如何提取数据
 */
@LiteflowComponent("setSessionMetadata")
public class SetSessionMetadataNode extends BaseWorkflowNode {

    private static final Logger log = LoggerFactory.getLogger(SetSessionMetadataNode.class);

    @Resource
    private ChatSessionRepository chatSessionRepository;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private LangChainChatService langChainChatService;

    @Resource
    private LlmModelService llmModelService;

    @Override
    public void process() {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = getWorkflowContext();
        
        try {
            UUID sessionId = ctx.getSessionId();
            if (sessionId == null) {
                log.warn("会话ID为空，无法设置元数据");
                setOutput("session_id_missing");
                recordExecution(null, "session_id_missing", startTime, false, "会话ID为空");
                return;
            }

            // 获取节点配置
            String tag = this.getTag();
            String nodeId = this.getNodeId();
            String actualNodeId = getActualNodeId();
            log.debug("SetSessionMetadata节点ID获取: tag={}, nodeId={}, actualNodeId={}", tag, nodeId, actualNodeId);
            
            JsonNode config = getNodeConfig();
            if (config == null) {
                log.warn("节点配置为空，无法设置元数据: nodeId={}, tag={}, actualNodeId={}, availableConfigs={}", 
                        nodeId, tag, actualNodeId, 
                        ctx.getNodesConfig() != null ? ctx.getNodesConfig().keySet() : "null");
                setOutput("config_missing");
                recordExecution(null, "config_missing", startTime, false, 
                        String.format("节点配置为空: nodeId=%s, tag=%s, actualNodeId=%s", nodeId, tag, actualNodeId));
                return;
            }

            JsonNode mappingsConfig = config.get("mappings");
            String modelIdStr = getConfigString("modelId", null);
            
            // 先检查 mappingsConfig，再构建默认提示词
            if (mappingsConfig == null || !mappingsConfig.isObject()) {
                log.warn("字段映射配置为空或格式错误");
                setOutput("mappings_config_invalid");
                recordExecution(null, "mappings_config_invalid", startTime, false, "字段映射配置为空或格式错误");
                return;
            }
            
            String systemPrompt = getConfigString("systemPrompt", getDefaultSystemPrompt(mappingsConfig));

            // 获取上一个节点的输出
            String lastOutput = ctx.getLastOutput();
            if (lastOutput == null || lastOutput.trim().isEmpty()) {
                log.warn("上一个节点输出为空，无法提取数据");
                setOutput("last_output_empty");
                recordExecution(null, "last_output_empty", startTime, false, "上一个节点输出为空");
                return;
            }

            // 使用AI模型提取数据
            Map<String, Object> sourceData = extractDataWithAI(lastOutput, mappingsConfig, systemPrompt, modelIdStr);

            // 获取会话
            ChatSession session = chatSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("会话不存在: " + sessionId));

            // 读取现有元数据
            Map<String, Object> metadata = readSessionMetadata(session);

            // 应用字段映射
            int updatedCount = 0;
            Iterator<Map.Entry<String, JsonNode>> fields = mappingsConfig.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String sourceField = field.getKey();
                String targetField = field.getValue().asText();

                // 从提取的数据中获取值
                Object value = sourceData.get(sourceField);
                // 如果值为空字符串，也视为空值，不设置
                if (value != null && !(value instanceof String && ((String) value).trim().isEmpty())) {
                    // 设置到目标字段
                    metadata.put(targetField, value);
                    updatedCount++;
                    log.debug("设置元数据: sourceField={}, targetField={}, value={}", 
                            sourceField, targetField, value);
                } else {
                    log.debug("源字段值为空，跳过: sourceField={}", sourceField);
                }
            }

            // 保存更新后的元数据
            saveSessionMetadata(session, metadata);
            ctx.setSessionMetadata(metadata);

            String output = String.format("已更新 %d 个元数据字段", updatedCount);
            setOutput(output);
            recordExecution(lastOutput, output, startTime, true, null);
            
            log.info("会话元数据设置完成: sessionId={}, updatedCount={}", sessionId, updatedCount);

        } catch (Exception e) {
            log.error("设置会话元数据失败", e);
            setOutput("error");
            recordExecution(null, "error", startTime, false, e.getMessage());
        }
    }

    /**
     * 使用AI模型提取数据
     */
    private Map<String, Object> extractDataWithAI(String lastOutput, JsonNode mappingsConfig, 
                                                   String systemPrompt, String modelIdStr) {
        try {
            // 构建字段定义列表（基于 mappings 配置）
            List<LangChainChatService.FieldSchemaDefinition> fieldDefinitions = buildFieldDefinitions(mappingsConfig);

            // 获取模型ID
            UUID modelId = null;
            if (modelIdStr != null && !modelIdStr.isEmpty()) {
                modelId = UUID.fromString(modelIdStr);
            }

            // 使用结构化输出提取数据
            LangChainChatService.StructuredOutputResponse response = langChainChatService.chatWithFieldDefinitions(
                    modelId,
                    systemPrompt,
                    lastOutput,
                    fieldDefinitions,
                    "session_metadata_extraction",
                    0.3  // 使用较低温度保证输出稳定性
            );

            if (!response.success() || response.jsonResult() == null) {
                log.warn("AI提取数据失败: {}", response.errorMessage());
                return new HashMap<>();
            }

            // 解析提取的JSON结果
            return parseJsonResponse(response.jsonResult());

        } catch (Exception e) {
            log.error("使用AI提取数据失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 构建字段定义列表
     */
    private List<LangChainChatService.FieldSchemaDefinition> buildFieldDefinitions(JsonNode mappingsConfig) {
        List<LangChainChatService.FieldSchemaDefinition> fields = new ArrayList<>();
        
        Iterator<Map.Entry<String, JsonNode>> iterator = mappingsConfig.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            String fieldName = entry.getKey();
            
            // 所有字段都定义为字符串类型（可以根据需要扩展）
            // 注意：OpenAI 的 JSON Schema 要求 required 数组必须包含所有 properties 中的 key
            // 所以这里设置为 true，但实际上字段值可以为空（通过提示词控制）
            LangChainChatService.FieldSchemaDefinition field = new LangChainChatService.FieldSchemaDefinition(
                    fieldName,
                    LangChainChatService.FieldSchemaDefinition.FieldType.STRING,
                    "提取 " + fieldName + " 字段的值，如果无法提取则返回空字符串",
                    true,   // OpenAI 要求 required 必须包含所有字段
                    null,   // 无枚举值
                    null,   // 无嵌套属性
                    null    // 无数组元素
            );
            fields.add(field);
        }
        
        return fields;
    }

    /**
     * 解析JSON响应
     */
    private Map<String, Object> parseJsonResponse(String jsonResult) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResult);
            if (jsonNode.isObject()) {
                return objectMapper.convertValue(jsonNode, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("解析AI提取结果失败: {}", e.getMessage());
        }
        return new HashMap<>();
    }

    /**
     * 获取默认系统提示词
     */
    private String getDefaultSystemPrompt(JsonNode mappingsConfig) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个信息提取助手。请从用户提供的内容中提取以下字段的信息：\n\n");
        
        if (mappingsConfig != null && mappingsConfig.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> iterator = mappingsConfig.fields();
            int index = 1;
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                String fieldName = entry.getKey();
                sb.append(String.format("%d. %s\n", index++, fieldName));
            }
        }
        
        sb.append("\n请严格按照JSON格式返回，必须包含所有配置的字段。如果某个字段无法提取，请返回空字符串 \"\"。");
        
        return sb.toString();
    }

    /**
     * 读取会话元数据
     */
    private Map<String, Object> readSessionMetadata(ChatSession session) {
        String metadataJson = session.getMetadata();
        if (metadataJson == null || metadataJson.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("解析会话元数据失败，使用空 Map: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 保存会话元数据
     */
    private void saveSessionMetadata(ChatSession session, Map<String, Object> metadata) {
        try {
            String metadataJson = objectMapper.writeValueAsString(metadata);
            session.setMetadata(metadataJson);
            chatSessionRepository.save(session);
        } catch (Exception e) {
            log.error("保存会话元数据失败", e);
            throw new RuntimeException("保存会话元数据失败: " + e.getMessage(), e);
        }
    }
}

