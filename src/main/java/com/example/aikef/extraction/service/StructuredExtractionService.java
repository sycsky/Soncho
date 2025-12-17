package com.example.aikef.extraction.service;

import com.example.aikef.extraction.model.ExtractionSchema;
import com.example.aikef.extraction.model.ExtractionSession;
import com.example.aikef.extraction.model.FieldDefinition;
import com.example.aikef.extraction.repository.ExtractionSchemaRepository;
import com.example.aikef.extraction.repository.ExtractionSessionRepository;
import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.llm.LlmModelService;
import com.example.aikef.model.LlmModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.example.aikef.llm.LangChainChatService.buildJsonSchemaElement;

/**
 * 结构化提取服务
 * 支持多轮对话从文本中提取结构化数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StructuredExtractionService {

    private final ExtractionSchemaRepository schemaRepository;
    private final ExtractionSessionRepository sessionRepository;
    private final LangChainChatService chatService;
    private final LlmModelService llmModelService;
    private final ObjectMapper objectMapper;

    // ==================== Schema 管理 ====================

    /**
     * 创建提取模式
     */
    @Transactional
    public ExtractionSchema createSchema(CreateSchemaRequest request, UUID createdBy) {
        if (schemaRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("模式名称已存在: " + request.name());
        }

        ExtractionSchema schema = new ExtractionSchema();
        schema.setName(request.name());
        schema.setDescription(request.description());
        schema.setFieldsJson(serializeFields(request.fields()));
        schema.setExtractionPrompt(request.extractionPrompt() != null ? request.extractionPrompt() : getDefaultExtractionPrompt());
        schema.setFollowupPrompt(request.followupPrompt() != null ? request.followupPrompt() : getDefaultFollowupPrompt());
        schema.setLlmModelId(request.llmModelId());
        schema.setEnabled(true);
        schema.setCreatedBy(createdBy);

        ExtractionSchema saved = schemaRepository.save(schema);
        log.info("创建提取模式: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    /**
     * 获取所有启用的模式
     */
    public List<ExtractionSchema> getEnabledSchemas() {
        return schemaRepository.findByEnabledTrue();
    }

    /**
     * 获取模式详情
     */
    public Optional<ExtractionSchema> getSchema(UUID schemaId) {
        return schemaRepository.findById(schemaId);
    }

    // ==================== 提取会话 ====================

    /**
     * 创建提取会话
     */
    @Transactional
    public ExtractionSession createSession(UUID schemaId, UUID createdBy, UUID referenceId, String referenceType) {
        ExtractionSchema schema = schemaRepository.findById(schemaId)
                .orElseThrow(() -> new IllegalArgumentException("模式不存在: " + schemaId));

        ExtractionSession session = new ExtractionSession();
        session.setSchema(schema);
        session.setStatus(ExtractionSession.SessionStatus.IN_PROGRESS);
        session.setExtractedData("{}");
        session.setMissingFields(getAllRequiredFieldNames(schema));
        session.setConversationHistory("[]");
        session.setCurrentRound(0);
        session.setMaxRounds(5);
        session.setCreatedBy(createdBy);
        session.setReferenceId(referenceId);
        session.setReferenceType(referenceType);

        ExtractionSession saved = sessionRepository.save(session);
        log.info("创建提取会话: id={}, schemaId={}", saved.getId(), schemaId);
        return saved;
    }

    /**
     * 提交文本并提取数据
     * @return 提取结果，包含已提取数据、缺失字段、下一步提问等
     */
    @Transactional
    public ExtractionResult submitText(UUID sessionId, String userText) {
        ExtractionSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在: " + sessionId));

        if (session.getStatus() != ExtractionSession.SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("会话已结束: " + session.getStatus());
        }

        ExtractionSchema schema = session.getSchema();
        List<FieldDefinition> fields = deserializeFields(schema.getFieldsJson());

        // 增加轮次
        session.setCurrentRound(session.getCurrentRound() + 1);

        // 检查是否超过最大轮次
        if (session.getCurrentRound() > session.getMaxRounds()) {
            session.setStatus(ExtractionSession.SessionStatus.MAX_ROUNDS_REACHED);
            sessionRepository.save(session);
            return buildResult(session, fields, "已达到最大对话轮次，请手动补充剩余信息。");
        }

        // 添加用户消息到历史
        addToConversationHistory(session, "user", userText);

        // 调用LLM提取数据
        Map<String, Object> currentData = deserializeExtractedData(session.getExtractedData());
        Map<String, Object> newData = extractFromText(schema, fields, userText, currentData);

        // 合并数据
        mergeExtractedData(currentData, newData);
        session.setExtractedData(serializeExtractedData(currentData));

        // 检查缺失字段
        List<String> missingFields = findMissingRequiredFields(fields, currentData);
        session.setMissingFields(serializeMissingFields(missingFields));

        // 判断是否完成
        String nextQuestion = null;
        if (missingFields.isEmpty()) {
            session.setStatus(ExtractionSession.SessionStatus.COMPLETED);
            log.info("提取完成: sessionId={}", sessionId);
        } else {
            // 生成追问
            nextQuestion = generateFollowupQuestion(schema, fields, missingFields, currentData);
            addToConversationHistory(session, "assistant", nextQuestion);
        }

        sessionRepository.save(session);
        return buildResult(session, fields, nextQuestion);
    }

    /**
     * 手动更新字段值
     */
    @Transactional
    public ExtractionResult updateField(UUID sessionId, String fieldName, Object value) {
        ExtractionSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在: " + sessionId));

        ExtractionSchema schema = session.getSchema();
        List<FieldDefinition> fields = deserializeFields(schema.getFieldsJson());

        Map<String, Object> currentData = deserializeExtractedData(session.getExtractedData());
        currentData.put(fieldName, value);
        session.setExtractedData(serializeExtractedData(currentData));

        List<String> missingFields = findMissingRequiredFields(fields, currentData);
        session.setMissingFields(serializeMissingFields(missingFields));

        if (missingFields.isEmpty()) {
            session.setStatus(ExtractionSession.SessionStatus.COMPLETED);
        }

        sessionRepository.save(session);
        return buildResult(session, fields, null);
    }

    /**
     * 完成会话（即使有缺失字段）
     */
    @Transactional
    public ExtractionResult completeSession(UUID sessionId) {
        ExtractionSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在: " + sessionId));

        session.setStatus(ExtractionSession.SessionStatus.COMPLETED);
        sessionRepository.save(session);

        List<FieldDefinition> fields = deserializeFields(session.getSchema().getFieldsJson());
        return buildResult(session, fields, null);
    }

    /**
     * 取消会话
     */
    @Transactional
    public void cancelSession(UUID sessionId) {
        ExtractionSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在: " + sessionId));

        session.setStatus(ExtractionSession.SessionStatus.CANCELLED);
        sessionRepository.save(session);
        log.info("取消提取会话: sessionId={}", sessionId);
    }

    /**
     * 获取会话详情
     */
    public Optional<ExtractionSession> getSession(UUID sessionId) {
        return sessionRepository.findById(sessionId);
    }

    // ==================== 私有方法 ====================

    /**
     * 从文本中提取数据
     * 使用 ResponseFormat + JSON Schema 强制结构化输出
     */
    private Map<String, Object> extractFromText(ExtractionSchema schema, List<FieldDefinition> fields,
                                                  String userText, Map<String, Object> existingData) {
        try {
            UUID modelId = schema.getLlmModelId();
            if (modelId == null) {
                var defaultModel = llmModelService.getDefaultModel()
                        .orElseThrow(() -> new IllegalStateException("未配置默认模型"));
                modelId = defaultModel.getId();
            }

            // 构建字段 Schema 定义（支持嵌套结构）
            List<LangChainChatService.FieldSchemaDefinition> fieldSchemas = fields.stream()
                    .map(this::convertToFieldSchemaDefinition)
                    .toList();

            // 构建提示词
            String systemPrompt = buildSystemPrompt(schema, fields, existingData);

            // 使用结构化输出
            var response = chatService.chatWithFieldDefinitions(
                    modelId,
                    systemPrompt,
                    userText,
                    fieldSchemas,
                    "extraction_" + schema.getName().replaceAll("[^a-zA-Z0-9]", "_"),
                    0.1
            );

            if (response.success() && response.jsonResult() != null) {
                return parseJsonResponse(response.jsonResult());
            } else {
                log.warn("结构化输出失败: {}", response.errorMessage());
                // 降级到普通提取
                return extractFromTextFallback(schema, fields, userText, existingData);
            }

        } catch (Exception e) {
            log.error("提取数据失败，尝试降级", e);
            return extractFromTextFallback(schema, fields, userText, existingData);
        }
    }

    /**
     * 映射字段类型
     */
    private LangChainChatService.FieldSchemaDefinition.FieldType mapFieldType(FieldDefinition.FieldType type) {
        return switch (type) {
            case STRING, DATE, DATETIME, EMAIL, PHONE -> LangChainChatService.FieldSchemaDefinition.FieldType.STRING;
            case NUMBER -> LangChainChatService.FieldSchemaDefinition.FieldType.NUMBER;
            case INTEGER -> LangChainChatService.FieldSchemaDefinition.FieldType.INTEGER;
            case BOOLEAN -> LangChainChatService.FieldSchemaDefinition.FieldType.BOOLEAN;
            case ENUM -> LangChainChatService.FieldSchemaDefinition.FieldType.ENUM;
            case ARRAY -> LangChainChatService.FieldSchemaDefinition.FieldType.ARRAY;
            case OBJECT -> LangChainChatService.FieldSchemaDefinition.FieldType.OBJECT;
        };
    }

    /**
     * 将 FieldDefinition 转换为 FieldSchemaDefinition（支持嵌套结构）
     */
    private LangChainChatService.FieldSchemaDefinition convertToFieldSchemaDefinition(FieldDefinition field) {
        String description = field.getDescription() != null ? field.getDescription() : 
                (field.getDisplayName() != null ? field.getDisplayName() : field.getName());
        
        // 转换嵌套的 properties（如果是 object 类型）
        List<LangChainChatService.FieldSchemaDefinition> properties = null;
        if (field.getType() == FieldDefinition.FieldType.OBJECT && field.getProperties() != null) {
            properties = field.getProperties().stream()
                    .map(this::convertToFieldSchemaDefinition)
                    .toList();
        }
        
        // 转换 items（如果是 array 类型）
        LangChainChatService.FieldSchemaDefinition items = null;
        if (field.getType() == FieldDefinition.FieldType.ARRAY && field.getItems() != null) {
            items = convertToFieldSchemaDefinition(field.getItems());
        }
        
        return new LangChainChatService.FieldSchemaDefinition(
                field.getName(),
                mapFieldType(field.getType()),
                description,
                field.isRequired(),
                field.getEnumValues(),
                properties,
                items
        );
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(ExtractionSchema schema, List<FieldDefinition> fields,
                                      Map<String, Object> existingData) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个信息提取助手。请从用户输入中提取结构化信息。\n\n");
        sb.append("## 字段说明：\n");

        for (FieldDefinition field : fields) {
            sb.append(String.format("- %s: %s%s\n",
                    field.getName(),
                    field.getDescription() != null ? field.getDescription() : field.getDisplayName(),
                    field.isRequired() ? " [必填]" : " [可选]"));

            if (field.getEnumValues() != null && !field.getEnumValues().isEmpty()) {
                sb.append(String.format("  可选值: %s\n", String.join(", ", field.getEnumValues())));
            }
        }

        if (!existingData.isEmpty()) {
            sb.append("\n## 已有数据：\n");
            sb.append(serializeExtractedData(existingData));
            sb.append("\n请补充提取缺失的字段。\n");
        }

        sb.append("\n## 要求：\n");
        sb.append("1. 只提取能从用户输入中明确识别的信息\n");
        sb.append("2. 无法识别的字段设为 null\n");
        sb.append("3. 不要猜测或编造数据\n");

        return sb.toString();
    }

    /**
     * 降级提取（使用普通聊天方式）
     */
    private Map<String, Object> extractFromTextFallback(ExtractionSchema schema, List<FieldDefinition> fields,
                                                         String userText, Map<String, Object> existingData) {
        String prompt = buildExtractionPrompt(schema, fields, userText, existingData);

        try {
            UUID modelId = schema.getLlmModelId();
            String response;

            if (modelId != null) {
                var chatResponse = chatService.chat(modelId, null, prompt, null, 0.1, 2000);
                response = chatResponse.content();
            } else {
                var defaultModel = llmModelService.getDefaultModel()
                        .orElseThrow(() -> new IllegalStateException("未配置默认模型"));
                var chatResponse = chatService.chat(defaultModel.getId(), null, prompt, null, 0.1, 2000);
                response = chatResponse.content();
            }

            return parseJsonResponse(response);

        } catch (Exception e) {
            log.error("降级提取也失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 构建提取提示词
     */
    private String buildExtractionPrompt(ExtractionSchema schema, List<FieldDefinition> fields,
                                          String userText, Map<String, Object> existingData) {
        StringBuilder sb = new StringBuilder();

        sb.append("你是一个数据提取助手。请从用户输入中提取结构化信息。\n\n");

        sb.append("## 要提取的字段定义：\n");
        for (FieldDefinition field : fields) {
            sb.append(String.format("- %s (%s): %s%s\n",
                    field.getName(),
                    field.getType().name(),
                    field.getDescription() != null ? field.getDescription() : field.getDisplayName(),
                    field.isRequired() ? " [必填]" : " [可选]"));

            if (field.getEnumValues() != null && !field.getEnumValues().isEmpty()) {
                sb.append(String.format("  可选值: %s\n", String.join(", ", field.getEnumValues())));
            }
            if (field.getExamples() != null && !field.getExamples().isEmpty()) {
                sb.append(String.format("  示例: %s\n", String.join(", ", field.getExamples())));
            }
        }

        if (!existingData.isEmpty()) {
            sb.append("\n## 已提取的数据：\n");
            sb.append("```json\n");
            sb.append(serializeExtractedData(existingData));
            sb.append("\n```\n");
        }

        sb.append("\n## 用户输入：\n");
        sb.append(userText);

        sb.append("\n\n## 要求：\n");
        sb.append("1. 仅返回JSON格式的提取结果\n");
        sb.append("2. 只包含能从用户输入中明确识别的字段\n");
        sb.append("3. 不要猜测或编造数据\n");
        sb.append("4. 如果某字段无法从输入中提取，不要包含该字段\n");
        sb.append("5. 直接返回JSON，不要有其他解释文字\n");

        return sb.toString();
    }

    /**
     * 生成追问
     */
    private String generateFollowupQuestion(ExtractionSchema schema, List<FieldDefinition> fields,
                                             List<String> missingFields, Map<String, Object> extractedData) {
        // 找到下一个要询问的字段
        String nextFieldName = missingFields.get(0);
        FieldDefinition nextField = fields.stream()
                .filter(f -> f.getName().equals(nextFieldName))
                .findFirst()
                .orElse(null);

        if (nextField != null && nextField.getFollowupQuestion() != null) {
            return nextField.getFollowupQuestion();
        }

        // 默认追问
        StringBuilder sb = new StringBuilder();
        sb.append("还需要以下信息：\n");
        
        int count = 0;
        for (String fieldName : missingFields) {
            if (count >= 3) {
                sb.append(String.format("...还有 %d 项\n", missingFields.size() - count));
                break;
            }
            
            FieldDefinition field = fields.stream()
                    .filter(f -> f.getName().equals(fieldName))
                    .findFirst()
                    .orElse(null);
            
            if (field != null) {
                sb.append(String.format("- %s", field.getDisplayName() != null ? field.getDisplayName() : fieldName));
                if (field.getDescription() != null) {
                    sb.append(String.format(" (%s)", field.getDescription()));
                }
                sb.append("\n");
            }
            count++;
        }

        sb.append("\n请提供以上信息。");
        return sb.toString();
    }

    /**
     * 解析JSON响应
     */
    private Map<String, Object> parseJsonResponse(String response) {
        try {
            // 尝试提取JSON部分
            String json = response;
            if (response.contains("```json")) {
                int start = response.indexOf("```json") + 7;
                int end = response.indexOf("```", start);
                if (end > start) {
                    json = response.substring(start, end).trim();
                }
            } else if (response.contains("```")) {
                int start = response.indexOf("```") + 3;
                int end = response.indexOf("```", start);
                if (end > start) {
                    json = response.substring(start, end).trim();
                }
            }

            // 找到JSON对象
            int braceStart = json.indexOf('{');
            int braceEnd = json.lastIndexOf('}');
            if (braceStart >= 0 && braceEnd > braceStart) {
                json = json.substring(braceStart, braceEnd + 1);
            }

            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("解析JSON响应失败: {}", response, e);
            return new HashMap<>();
        }
    }

    /**
     * 合并提取的数据
     */
    private void mergeExtractedData(Map<String, Object> existing, Map<String, Object> newData) {
        for (Map.Entry<String, Object> entry : newData.entrySet()) {
            if (entry.getValue() != null) {
                existing.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 查找缺失的必填字段
     */
    private List<String> findMissingRequiredFields(List<FieldDefinition> fields, Map<String, Object> data) {
        return fields.stream()
                .filter(FieldDefinition::isRequired)
                .filter(f -> {
                    Object value = data.get(f.getName());
                    return value == null || (value instanceof String && ((String) value).isEmpty());
                })
                .map(FieldDefinition::getName)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有必填字段名
     */
    private String getAllRequiredFieldNames(ExtractionSchema schema) {
        try {
            List<FieldDefinition> fields = deserializeFields(schema.getFieldsJson());
            List<String> requiredFields = fields.stream()
                    .filter(FieldDefinition::isRequired)
                    .map(FieldDefinition::getName)
                    .collect(Collectors.toList());
            return objectMapper.writeValueAsString(requiredFields);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 添加到对话历史
     */
    private void addToConversationHistory(ExtractionSession session, String role, String content) {
        try {
            ArrayNode history;
            if (session.getConversationHistory() == null || session.getConversationHistory().isEmpty()) {
                history = objectMapper.createArrayNode();
            } else {
                history = (ArrayNode) objectMapper.readTree(session.getConversationHistory());
            }

            ObjectNode message = objectMapper.createObjectNode();
            message.put("role", role);
            message.put("content", content);
            message.put("timestamp", System.currentTimeMillis());
            history.add(message);

            session.setConversationHistory(objectMapper.writeValueAsString(history));
        } catch (Exception e) {
            log.error("添加对话历史失败", e);
        }
    }

    /**
     * 构建结果
     */
    private ExtractionResult buildResult(ExtractionSession session, List<FieldDefinition> fields, String nextQuestion) {
        Map<String, Object> data = deserializeExtractedData(session.getExtractedData());
        List<String> missing = deserializeMissingFields(session.getMissingFields());

        List<FieldStatus> fieldStatuses = fields.stream()
                .map(f -> new FieldStatus(
                        f.getName(),
                        f.getDisplayName(),
                        data.get(f.getName()),
                        f.isRequired(),
                        data.containsKey(f.getName()) && data.get(f.getName()) != null
                ))
                .collect(Collectors.toList());

        return new ExtractionResult(
                session.getId(),
                session.getStatus(),
                data,
                missing,
                fieldStatuses,
                session.getCurrentRound(),
                session.getMaxRounds(),
                nextQuestion,
                session.getStatus() == ExtractionSession.SessionStatus.COMPLETED
        );
    }

    // ==================== 序列化方法 ====================

    private String serializeFields(List<FieldDefinition> fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<FieldDefinition> deserializeFields(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<FieldDefinition>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String serializeExtractedData(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Map<String, Object> deserializeExtractedData(String json) {
        try {
            if (json == null || json.isEmpty()) return new HashMap<>();
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String serializeMissingFields(List<String> fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> deserializeMissingFields(String json) {
        try {
            if (json == null || json.isEmpty()) return new ArrayList<>();
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }


    /**
     * 简单的 JSON 提取（用于工具参数提取）
     * 
     * @param modelId 模型ID（如果为 null，使用默认模型）
     * @param prompt 系统提示词
     * @param messages 聊天消息列表（如果为空，则使用 prompt 作为用户消息）
     * @param name Schema 名称（用于 JSON Schema）
     * @param targetParamDefs 目标参数定义列表
     * @return 提取的 JSON 字符串
     */
    public String extractAsJson(UUID modelId, String prompt, List<ChatMessage> messages, 
                                String name, List<FieldDefinition> targetParamDefs) {
        try {
            // 如果没有提供 modelId，使用默认模型
            if (modelId == null) {
                var defaultModel = llmModelService.getDefaultModel();
                if (defaultModel.isEmpty()) {
                    throw new IllegalStateException("未配置默认模型");
                }
                modelId = defaultModel.get().getId();
            }

            Map<String, JsonSchemaElement> properties = new LinkedHashMap<>();
            List<String> requiredFields = new ArrayList<>();

            // 将 FieldDefinition 转换为 FieldSchemaDefinition（支持嵌套结构）
            List<LangChainChatService.FieldSchemaDefinition> fieldSchemas = targetParamDefs.stream()
                    .map(this::convertToFieldSchemaDefinition)
                    .toList();

            for (LangChainChatService.FieldSchemaDefinition field : fieldSchemas) {
                JsonSchemaElement element = buildJsonSchemaElement(field);
                properties.put(field.name(), element);

                if (field.required()) {
                    requiredFields.add(field.name());
                }
            }

            // 构建 JsonObjectSchema
            dev.langchain4j.model.chat.request.json.JsonObjectSchema jsonSchema = JsonObjectSchema.builder()
                    .properties(properties)
                    .required(requiredFields)
                    .additionalProperties(false)
                    .build();

            // 构建 ResponseFormat
            ResponseFormat responseFormat = ResponseFormat.builder()
                    .type(ResponseFormatType.JSON)
                    .jsonSchema(JsonSchema.builder()
                            .name(name != null ? name : "extraction_result")
                            .rootElement(jsonSchema)
                            .build())
                    .build();

            LlmModel modelConfig = llmModelService.getModel(modelId);

            OpenAiChatModel chatModel = chatService.createOpenAiModelWithResponseFormat(modelConfig, null);
            // 构建消息列表
            // 构建请求
            ChatRequest request = ChatRequest.builder()
                    .messages(messages)
                    .responseFormat(responseFormat)
                    .build();

            // 调用 LLM
            ChatResponse response = chatModel.chat(request);

            return response.aiMessage().text();

        } catch (Exception e) {
            log.error("JSON 提取失败", e);
            // 降级到普通提取
            try {
                return extractAsJsonFallback(modelId, prompt, messages, targetParamDefs);
            } catch (Exception fallbackError) {
                log.error("降级提取也失败", fallbackError);
                return "{}";
            }
        }
    }

    /**
     * 构建提取系统提示词
     */
    private String buildExtractionSystemPrompt(List<FieldDefinition> targetParamDefs) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个信息提取助手。请从用户输入中提取结构化信息，以 JSON 格式返回。\n\n");
        sb.append("## 字段说明：\n");

        for (FieldDefinition field : targetParamDefs) {
            sb.append(String.format("- %s: %s%s\n",
                    field.getName(),
                    field.getDescription() != null ? field.getDescription() : 
                            (field.getDisplayName() != null ? field.getDisplayName() : field.getName()),
                    field.isRequired() ? " [必填]" : " [可选]"));

            if (field.getEnumValues() != null && !field.getEnumValues().isEmpty()) {
                sb.append(String.format("  可选值: %s\n", String.join(", ", field.getEnumValues())));
            }
        }

        sb.append("\n## 要求：\n");
        sb.append("1. 只提取能从用户输入中明确识别的信息\n");
        sb.append("2. 无法识别的字段设为 null\n");
        sb.append("3. 不要猜测或编造数据\n");
        sb.append("4. 返回标准的 JSON 格式\n");

        return sb.toString();
    }

    /**
     * 降级提取（使用普通聊天方式）
     */
    private String extractAsJsonFallback(UUID modelId, String prompt, 
                                         List<ChatMessage> messages, 
                                         List<FieldDefinition> targetParamDefs) {
        try {
            // 构建提取提示词
            StringBuilder extractPrompt = new StringBuilder();
            extractPrompt.append("请从以下内容中提取信息，以 JSON 格式返回。\n\n");
            
            extractPrompt.append("## 要提取的字段：\n");
            for (FieldDefinition field : targetParamDefs) {
                extractPrompt.append(String.format("- %s (%s): %s%s\n",
                        field.getName(),
                        field.getType().name(),
                        field.getDescription() != null ? field.getDescription() : 
                                (field.getDisplayName() != null ? field.getDisplayName() : field.getName()),
                        field.isRequired() ? " [必填]" : " [可选]"));
            }

            extractPrompt.append("\n## 内容：\n");
            if (messages != null && !messages.isEmpty()) {
                for (ChatMessage msg : messages) {
                    if (msg instanceof UserMessage) {
                        extractPrompt.append("用户: ").append(((UserMessage) msg).text()).append("\n");
                    } else if (msg instanceof AiMessage) {
                        extractPrompt.append("助手: ").append(((AiMessage) msg).text()).append("\n");
                    }
                }
            } else if (prompt != null && !prompt.isEmpty()) {
                extractPrompt.append(prompt).append("\n");
            }

            extractPrompt.append("\n只返回 JSON 对象，不要有其他文字。");

            // 调用普通聊天接口
            var response = chatService.chat(modelId, null, extractPrompt.toString(), null, 0.1, 2000);
            if (response.success()) {
                String result = response.reply();
                // 尝试提取 JSON 部分
                return extractJsonFromText(result);
            }
            return "{}";
        } catch (Exception e) {
            log.error("降级提取失败", e);
            return "{}";
        }
    }

    /**
     * 从文本中提取 JSON
     */
    private String extractJsonFromText(String text) {
        if (text == null || text.isEmpty()) {
            return "{}";
        }

        try {
            // 尝试提取 JSON 部分
            String json = text;
            if (text.contains("```json")) {
                int start = text.indexOf("```json") + 7;
                int end = text.indexOf("```", start);
                if (end > start) {
                    json = text.substring(start, end).trim();
                }
            } else if (text.contains("```")) {
                int start = text.indexOf("```") + 3;
                int end = text.indexOf("```", start);
                if (end > start) {
                    json = text.substring(start, end).trim();
                }
            }

            // 找到 JSON 对象
            int braceStart = json.indexOf('{');
            int braceEnd = json.lastIndexOf('}');
            if (braceStart >= 0 && braceEnd > braceStart) {
                json = json.substring(braceStart, braceEnd + 1);
            }

            // 验证 JSON 格式
            objectMapper.readTree(json);
            return json;
        } catch (Exception e) {
            log.warn("提取 JSON 失败: {}", text, e);
            return "{}";
        }
    }

    /**
     * 简单的 JSON 提取（用于工具参数提取）
     */
    public String extractAsJson(UUID modelId, String prompt) {

        try {
            if (modelId == null) {
                var defaultModel = llmModelService.getDefaultModel();
                if (defaultModel.isEmpty()) {
                    throw new IllegalStateException("未配置默认模型");
                }
                modelId = defaultModel.get().getId();
            }

            var response = chatService.chat(modelId, null, prompt, null, 0.1, 2000);
            return response.success() ? response.reply() : null;

        } catch (Exception e) {
            log.error("JSON 提取失败", e);
            return null;
        }
    }

    private String getDefaultExtractionPrompt() {
        return "请从用户输入中提取结构化信息，返回JSON格式。";
    }

    private String getDefaultFollowupPrompt() {
        return "请提供缺失的必填信息。";
    }

    // ==================== DTOs ====================

    public record CreateSchemaRequest(
            String name,
            String description,
            List<FieldDefinition> fields,
            String extractionPrompt,
            String followupPrompt,
            UUID llmModelId
    ) {}

    public record ExtractionResult(
            UUID sessionId,
            ExtractionSession.SessionStatus status,
            Map<String, Object> extractedData,
            List<String> missingFields,
            List<FieldStatus> fieldStatuses,
            int currentRound,
            int maxRounds,
            String nextQuestion,
            boolean isComplete
    ) {}

    public record FieldStatus(
            String name,
            String displayName,
            Object value,
            boolean required,
            boolean filled
    ) {}
}

