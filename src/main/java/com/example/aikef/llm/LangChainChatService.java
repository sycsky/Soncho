package com.example.aikef.llm;

import com.example.aikef.model.LlmModel;
import com.example.aikef.model.enums.LlmProvider;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.*;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LangChain4j 聊天服务
 * 根据模型配置动态创建和管理 ChatModel 实例
 */
@Service
public class LangChainChatService {

    private static final Logger log = LoggerFactory.getLogger(LangChainChatService.class);

    private final LlmModelService llmModelService;

    /**
     * 模型实例缓存
     * key: modelId, value: ChatModel
     */
    private final Map<UUID, ChatModel> modelCache = new ConcurrentHashMap<>();

    /**
     * 模型版本缓存（用于检测配置变更）
     */
    private final Map<UUID, Long> modelVersionCache = new ConcurrentHashMap<>();

    public LangChainChatService(LlmModelService llmModelService) {
        this.llmModelService = llmModelService;
    }

    /**
     * 发送聊天请求
     *
     * @param modelId 模型ID（如果为 null，使用默认模型）
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @param chatHistory 聊天历史
     * @param temperature 温度（可选，使用模型默认值）
     * @param maxTokens 最大 Token 数（可选，使用模型默认值）
     * @return AI 回复
     */
    public LlmChatResponse chat(UUID modelId, 
                             String systemPrompt, 
                             String userMessage,
                             List<ChatHistoryMessage> chatHistory,
                             Double temperature,
                             Integer maxTokens) {
        return chat(modelId, systemPrompt, userMessage, chatHistory, temperature, maxTokens, null);
    }

    /**
     * 发送聊天请求（支持自定义超时）
     */
    public LlmChatResponse chat(UUID modelId, 
                             String systemPrompt, 
                             String userMessage,
                             List<ChatHistoryMessage> chatHistory,
                             Double temperature,
                             Integer maxTokens,
                             Integer timeoutSeconds) {
        
        // 获取模型配置
        LlmModel modelConfig;
        if (modelId != null) {
            modelConfig = llmModelService.getModel(modelId);
        } else {
            modelConfig = llmModelService.getDefaultModel()
                    .orElseThrow(() -> new EntityNotFoundException("未配置默认模型"));
        }

        if (!modelConfig.getEnabled()) {
            throw new IllegalStateException("模型已禁用: " + modelConfig.getName());
        }

        // 获取或创建模型实例（如果指定了超时，创建新实例，不使用缓存）
        ChatModel chatModel;
        if (timeoutSeconds != null && timeoutSeconds > 60) {
            // 使用自定义超时，创建新实例（不缓存）
            chatModel = createChatModelWithTimeout(modelConfig, temperature, maxTokens, timeoutSeconds);
        } else {
            // 使用默认超时，使用缓存
            chatModel = getOrCreateModel(modelConfig, temperature, maxTokens);
        }

        // 构建消息列表
        List<ChatMessage> messages = buildMessages(systemPrompt, userMessage, chatHistory);

        // 发送请求
        long startTime = System.currentTimeMillis();
        try {
            ChatResponse response = chatModel.chat(messages);
            long duration = System.currentTimeMillis() - startTime;

            String reply = response.aiMessage().text();
            
            // 估算 Token 使用量
            int inputTokens = estimateTokens(messages);
            int outputTokens = estimateTokens(reply);

            log.info("LLM 调用完成: model={}, duration={}ms, inputTokens={}, outputTokens={}",
                    modelConfig.getName(), duration, inputTokens, outputTokens);

            return new LlmChatResponse(
                    true,
                    reply,
                    null,
                    modelConfig.getId(),
                    modelConfig.getName(),
                    duration,
                    inputTokens,
                    outputTokens
            );

        } catch (Exception e) {
            log.error("LLM 调用失败: model={}", modelConfig.getName(), e);
            return new LlmChatResponse(
                    false,
                    null,
                    e.getMessage(),
                    modelConfig.getId(),
                    modelConfig.getName(),
                    System.currentTimeMillis() - startTime,
                    0,
                    0
            );
        }
    }

    /**
     * 使用模型编码发送聊天请求
     */
    public LlmChatResponse chatByCode(String modelCode,
                                    String systemPrompt,
                                    String userMessage,
                                    List<ChatHistoryMessage> chatHistory,
                                    Double temperature,
                                    Integer maxTokens) {
        LlmModel model = llmModelService.getModelByCode(modelCode);
        return chat(model.getId(), systemPrompt, userMessage, chatHistory, temperature, maxTokens);
    }

    /**
     * 发送聊天请求（直接传入消息列表）
     * 适用于已经构建好消息列表的场景
     *
     * @param modelId 模型ID（如果为 null，使用默认模型）
     * @param systemPrompt 系统提示词
     * @param messages 消息列表（包含历史和当前消息）
     * @param temperature 温度
     * @param maxTokens 最大 Token 数
     * @return AI 回复
     */
    public LlmChatResponse chatWithMessages(UUID modelId,
                                          String systemPrompt,
                                          List<ChatHistoryMessage> messages,
                                          Double temperature,
                                          Integer maxTokens) {
        // 获取模型配置
        LlmModel modelConfig;
        if (modelId != null) {
            modelConfig = llmModelService.getModel(modelId);
        } else {
            modelConfig = llmModelService.getDefaultModel()
                    .orElseThrow(() -> new EntityNotFoundException("未配置默认模型"));
        }

        if (!modelConfig.getEnabled()) {
            throw new IllegalStateException("模型已禁用: " + modelConfig.getName());
        }

        // 获取或创建模型实例
        ChatModel chatModel = getOrCreateModel(modelConfig, temperature, maxTokens);

        // 构建 LangChain4j 消息列表
        List<ChatMessage> chatMessages = buildMessagesFromList(systemPrompt, messages);


        // 发送请求
        long startTime = System.currentTimeMillis();
        try {
            ChatResponse response = chatModel.chat(chatMessages);
            long duration = System.currentTimeMillis() - startTime;

            String reply = response.aiMessage().text();
            
            int inputTokens = estimateTokens(chatMessages);
            int outputTokens = estimateTokens(reply);

            log.info("LLM 调用完成: model={}, duration={}ms, inputTokens={}, outputTokens={}",
                    modelConfig.getName(), duration, inputTokens, outputTokens);

            return new LlmChatResponse(
                    true,
                    reply,
                    null,
                    modelConfig.getId(),
                    modelConfig.getName(),
                    duration,
                    inputTokens,
                    outputTokens
            );

        } catch (Exception e) {
            log.error("LLM 调用失败: model={}", modelConfig.getName(), e);
            return new LlmChatResponse(
                    false,
                    null,
                    e.getMessage(),
                    modelConfig.getId(),
                    modelConfig.getName(),
                    System.currentTimeMillis() - startTime,
                    0,
                    0
            );
        }
    }

    /**
     * 使用模型编码发送聊天请求（直接传入消息列表）
     */
    public LlmChatResponse chatWithMessagesByCode(String modelCode,
                                                String systemPrompt,
                                                List<ChatHistoryMessage> messages,
                                                Double temperature,
                                                Integer maxTokens) {
        LlmModel model = llmModelService.getModelByCode(modelCode);
        return chatWithMessages(model.getId(), systemPrompt, messages, temperature, maxTokens);
    }

    public ChatResponse chatWithTools(
            UUID modelId,
            List<ChatMessage> messages,
            List<ToolSpecification> toolSpecifications,
            Double temperature,
            Integer maxTokens) {

        LlmModel modelConfig;
        if (modelId != null) {
            modelConfig = llmModelService.getModel(modelId);
        } else {
            modelConfig = llmModelService.getDefaultModel()
                    .orElseThrow(() -> new EntityNotFoundException("未配置默认模型"));
        }

        if (!modelConfig.getEnabled()) {
            throw new IllegalStateException("模型已禁用: " + modelConfig.getName());
        }

        ChatModel chatModel = getOrCreateModel(modelConfig, temperature, maxTokens);
        ChatRequest.Builder requestBuilder = ChatRequest.builder().messages(messages);

        if (toolSpecifications != null && !toolSpecifications.isEmpty()) {
            requestBuilder.toolSpecifications(toolSpecifications);
        }

        return chatModel.chat(requestBuilder.build());
    }

    /**
     * 简单聊天（使用默认模型）
     */
    public String simpleChat(String systemPrompt, String userMessage) {
        LlmChatResponse response = chat(null, systemPrompt, userMessage, null, null, null);
        if (!response.success()) {
            throw new RuntimeException("LLM 调用失败: " + response.errorMessage());
        }
        return response.reply();
    }

    /**
     * 结构化输出 - 使用 JSON Schema 强制 LLM 返回指定格式
     *
     * @param modelId 模型ID
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @param jsonSchema JSON Schema 定义
     * @param schemaName Schema 名称
     * @param temperature 温度
     * @return 结构化输出结果
     */
    public StructuredOutputResponse chatWithStructuredOutput(
            UUID modelId,
            String systemPrompt,
            String userMessage,
            JsonObjectSchema jsonSchema,
            String schemaName,
            Double temperature) {

        LlmModel modelConfig;
        if (modelId != null) {
            modelConfig = llmModelService.getModel(modelId);
        } else {
            modelConfig = llmModelService.getDefaultModel()
                    .orElseThrow(() -> new EntityNotFoundException("未配置默认模型"));
        }

        if (!modelConfig.getEnabled()) {
            throw new IllegalStateException("模型已禁用: " + modelConfig.getName());
        }

        LlmProvider provider = LlmProvider.valueOf(modelConfig.getProvider());

        // 构建消息
        List<ChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(SystemMessage.from(systemPrompt));
        }
        String actualUserMessage = userMessage;
        if (!supportsResponseFormat(provider)) {
            actualUserMessage = userMessage + "\n\n请只输出一个 JSON 对象，不要输出任何额外文本。JSON 必须满足如下 JSON Schema:\n" + jsonSchema;
        }
        messages.add(UserMessage.from(actualUserMessage));

        long startTime = System.currentTimeMillis();
        try {
            String jsonResult;
            if (supportsResponseFormat(provider)) {
                ChatModel chatModel = getOrCreateModel(modelConfig, temperature, null);

                ResponseFormat responseFormat = ResponseFormat.builder()
                        .type(ResponseFormatType.JSON)
                        .jsonSchema(JsonSchema.builder()
                                .name(schemaName != null ? schemaName : "extraction_result")
                                .rootElement(jsonSchema)
                                .build())
                        .build();

                ChatRequest request = ChatRequest.builder()
                        .messages(messages)
                        .responseFormat(responseFormat)
                        .build();

                ChatResponse response = chatModel.chat(request);
                jsonResult = response.aiMessage().text();
            } else {
                ChatModel chatModel = getOrCreateModel(modelConfig, temperature, null);
                ChatResponse response = chatModel.chat(messages);
                jsonResult = response.aiMessage().text();
            }
            long duration = System.currentTimeMillis() - startTime;

            log.info("结构化输出完成: model={}, duration={}ms, schema={}",
                    modelConfig.getName(), duration, schemaName);

            return new StructuredOutputResponse(
                    true,
                    jsonResult,
                    null,
                    modelConfig.getId(),
                    modelConfig.getName(),
                    duration
            );

        } catch (Exception e) {
            log.error("结构化输出失败: model={}, schema={}", modelConfig.getName(), schemaName, e);
            return new StructuredOutputResponse(
                    false,
                    null,
                    e.getMessage(),
                    modelConfig.getId(),
                    modelConfig.getName(),
                    System.currentTimeMillis() - startTime
            );
        }
    }

    public StructuredOutputResponse chatWithStructuredOutputMessages(
            UUID modelId,
            List<ChatMessage> messages,
            JsonObjectSchema jsonSchema,
            String schemaName,
            Double temperature) {

        LlmModel modelConfig;
        if (modelId != null) {
            modelConfig = llmModelService.getModel(modelId);
        } else {
            modelConfig = llmModelService.getDefaultModel()
                    .orElseThrow(() -> new EntityNotFoundException("未配置默认模型"));
        }

        if (!modelConfig.getEnabled()) {
            throw new IllegalStateException("模型已禁用: " + modelConfig.getName());
        }

        LlmProvider provider = LlmProvider.valueOf(modelConfig.getProvider());

        List<ChatMessage> actualMessages = new ArrayList<>(messages != null ? messages : Collections.emptyList());
        if (!supportsResponseFormat(provider)) {
            actualMessages.add(UserMessage.from("\n\n请只输出一个 JSON 对象，不要输出任何额外文本。JSON 必须满足如下 JSON Schema:\n" + jsonSchema));
        }

        long startTime = System.currentTimeMillis();
        try {
            String jsonResult;
            if (supportsResponseFormat(provider)) {
                ChatModel chatModel = getOrCreateModel(modelConfig, temperature, null);

                ResponseFormat responseFormat = ResponseFormat.builder()
                        .type(ResponseFormatType.JSON)
                        .jsonSchema(JsonSchema.builder()
                                .name(schemaName != null ? schemaName : "extraction_result")
                                .rootElement(jsonSchema)
                                .build())
                        .build();

                ChatRequest request = ChatRequest.builder()
                        .messages(actualMessages)
                        .responseFormat(responseFormat)
                        .build();

                ChatResponse response = chatModel.chat(request);
                jsonResult = response.aiMessage().text();
            } else {
                ChatModel chatModel = getOrCreateModel(modelConfig, temperature, null);
                ChatResponse response = chatModel.chat(actualMessages);
                jsonResult = response.aiMessage().text();
            }
            long duration = System.currentTimeMillis() - startTime;

            log.info("结构化输出完成: model={}, duration={}ms, schema={}",
                    modelConfig.getName(), duration, schemaName);

            return new StructuredOutputResponse(
                    true,
                    jsonResult,
                    null,
                    modelConfig.getId(),
                    modelConfig.getName(),
                    duration
            );

        } catch (Exception e) {
            log.error("结构化输出失败: model={}, schema={}", modelConfig.getName(), schemaName, e);
            return new StructuredOutputResponse(
                    false,
                    null,
                    e.getMessage(),
                    modelConfig.getId(),
                    modelConfig.getName(),
                    System.currentTimeMillis() - startTime
            );
        }
    }

    /**
     * 使用字段定义构建结构化输出
     */
    public StructuredOutputResponse chatWithFieldDefinitions(
            UUID modelId,
            String systemPrompt,
            String userMessage,
            List<FieldSchemaDefinition> fields,
            String schemaName,
            Double temperature) {

        // 构建 properties Map
        Map<String, JsonSchemaElement> properties = new LinkedHashMap<>();
        List<String> requiredFields = new ArrayList<>();

        for (FieldSchemaDefinition field : fields) {
            JsonSchemaElement element = buildJsonSchemaElement(field);
            properties.put(field.name(), element);

            if (field.required()) {
                requiredFields.add(field.name());
            }
        }

        // 构建 JsonObjectSchema
        JsonObjectSchema jsonSchema = JsonObjectSchema.builder()
                .addProperties(properties)
                .required(requiredFields)
                .additionalProperties(false)
                .build();

        return chatWithStructuredOutput(modelId, systemPrompt, userMessage,
                jsonSchema, schemaName, temperature);
    }

    /**
     * 构建 JSON Schema 元素（支持嵌套的 object 和 array）
     */
    public static JsonSchemaElement buildJsonSchemaElement(FieldSchemaDefinition field) {
        return buildJsonSchemaElement(field, true);
    }

    /**
     * 构建 JSON Schema 元素（支持嵌套的 object 和 array）
     * @param field 字段定义
     * @param strict 是否严格模式（object 类型是否要求所有属性必填）
     */
    private static JsonSchemaElement buildJsonSchemaElement(FieldSchemaDefinition field, boolean strict) {
        String description = field.description();
        
        return switch (field.type()) {
            case STRING -> JsonStringSchema.builder()
                    .description(description)
                    .build();
            case INTEGER -> JsonIntegerSchema.builder()
                    .description(description)
                    .build();
            case NUMBER -> JsonNumberSchema.builder()
                    .description(description)
                    .build();
            case BOOLEAN -> JsonBooleanSchema.builder()
                    .description(description)
                    .build();
            case ENUM -> JsonEnumSchema.builder()
                    .enumValues(field.enumValues())
                    .description(description)
                    .build();
            case ARRAY -> {
                // 如果定义了 items，使用定义的 items；否则默认使用 string
                JsonSchemaElement itemsElement;
                if (field.items() != null) {
                    itemsElement = buildJsonSchemaElement(field.items(), strict);
                } else {
                    itemsElement = JsonStringSchema.builder().build();
                }
                yield JsonArraySchema.builder()
                        .items(itemsElement)
                        .description(description)
                        .build();
            }
            case OBJECT -> {
                // 构建嵌套对象的 properties
                Map<String, JsonSchemaElement> properties = new LinkedHashMap<>();
                List<String> required = new ArrayList<>();
                
                if (field.properties() != null) {
                    for (FieldSchemaDefinition prop : field.properties()) {
                        JsonSchemaElement element = buildJsonSchemaElement(prop, strict);
                        properties.put(prop.name(), element);
                        
                        if (prop.required()) {
                            required.add(prop.name());
                        }
                    }
                }
                
                JsonObjectSchema.Builder builder = JsonObjectSchema.builder()
                        .description(description)
                        .addProperties(properties);
                
                if (!required.isEmpty()) {
                    builder.required(required);
                }
                
                if (strict && field.properties() != null) {
                    // 严格模式：所有属性都必填，不允许额外属性
                    builder.required(new ArrayList<>(properties.keySet()))
                           .additionalProperties(false);
                }
                
                yield builder.build();
            }
        };
    }

    /**
     * 清除模型缓存（配置变更后调用）
     */
    public void clearModelCache(UUID modelId) {
        modelCache.remove(modelId);
        modelVersionCache.remove(modelId);
        log.info("清除模型缓存: modelId={}", modelId);
    }

    /**
     * 清除所有模型缓存
     */
    public void clearAllModelCache() {
        modelCache.clear();
        modelVersionCache.clear();
        log.info("清除所有模型缓存");
    }

    // ==================== 私有方法 ====================

    /**
     * 获取或创建模型实例
     */
    private ChatModel getOrCreateModel(LlmModel config, Double temperature, Integer maxTokens) {
        UUID modelId = config.getId();
        long currentVersion = config.getUpdatedAt() != null ? config.getUpdatedAt().toEpochMilli() : 0;

        // 检查缓存是否有效
        Long cachedVersion = modelVersionCache.get(modelId);
        if (cachedVersion != null && cachedVersion == currentVersion && modelCache.containsKey(modelId)) {
            return modelCache.get(modelId);
        }

        // 创建新的模型实例
        ChatModel chatModel = createChatModel(config, temperature, maxTokens);
        modelCache.put(modelId, chatModel);
        modelVersionCache.put(modelId, currentVersion);

        return chatModel;
    }

    /**
     * 根据配置创建 ChatModel
     */
    private ChatModel createChatModel(LlmModel config, Double temperature, Integer maxTokens) {
        return createChatModelWithTimeout(config, temperature, maxTokens, 60);
    }

    /**
     * 根据配置创建 ChatModel（支持自定义超时）
     */
    private ChatModel createChatModelWithTimeout(LlmModel config, Double temperature, Integer maxTokens, int timeoutSeconds) {
        String provider = config.getProvider();
        
        // 使用节点配置的参数，如果没有则使用模型默认值，最后使用系统默认值
        double temp = temperature != null ? temperature 
                : (config.getDefaultTemperature() != null ? config.getDefaultTemperature() : 1);
        int tokens = maxTokens != null ? maxTokens 
                : (config.getDefaultMaxTokens() != null ? config.getDefaultMaxTokens() : 2000);

        return switch (LlmProvider.valueOf(provider)) {
            case OPENAI, CUSTOM, DASHSCOPE, MOONSHOT, DEEPSEEK, HUGGINGFACE,GEMINI -> createOpenAiCompatibleModel(config, temp, tokens, timeoutSeconds);
            case AZURE_OPENAI -> createAzureOpenAiModel(config, temp, tokens, timeoutSeconds);
            case OLLAMA -> createOllamaModel(config, temp, tokens, timeoutSeconds);
            case ZHIPU -> createZhipuModel(config, temp, tokens, timeoutSeconds);
//            case GEMINI -> throw new UnsupportedOperationException("暂不支持的提供商: " + provider);
        };
    }

    /**
     * 创建 OpenAI 兼容模型（包括 OpenAI、DeepSeek、Moonshot 等）
     */
    private ChatModel createOpenAiCompatibleModel(LlmModel config, double temperature, int maxTokens) {
        return createOpenAiCompatibleModel(config, temperature, maxTokens, 60);
    }

    /**
     * 创建 OpenAI 兼容模型（支持自定义超时）
     */
    private ChatModel createOpenAiCompatibleModel(LlmModel config, double temperature, int maxTokens, int timeoutSeconds) {
        String baseUrl = resolveOpenAiCompatibleBaseUrl(config);
        if(config.getModelName().contains("gpt-5")){
            if(temperature<1.0){
                temperature=1.0;
            }
            return OpenAiChatModel.builder()
                    .apiKey(config.getApiKey())
                    .baseUrl(baseUrl)
                    .modelName(config.getModelName())
                    .temperature(temperature)
//                    .maxCompletionTokens(maxTokens)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .strictJsonSchema(true)
                    .logRequests(true)
                    .logResponses(true)
                    .build();
        }else{
            return OpenAiChatModel.builder()
                    .apiKey(config.getApiKey())
                    .baseUrl(baseUrl)
                    .modelName(config.getModelName())
                    .temperature(temperature)
//                    .maxTokens(maxTokens)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .strictJsonSchema(true)
                    .logRequests(true)
                    .logResponses(true)
                    .build();
        }
    }

    private String resolveOpenAiCompatibleBaseUrl(LlmModel config) {
        String configured = config.getBaseUrl();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }

        LlmProvider provider = LlmProvider.valueOf(config.getProvider());
        String defaultBaseUrl = provider.getDefaultBaseUrl();
        if (defaultBaseUrl != null && !defaultBaseUrl.isBlank()) {
            return defaultBaseUrl;
        }

        throw new IllegalArgumentException("未配置 baseUrl: model=" + config.getName() + ", provider=" + provider.name());
    }

    private boolean supportsResponseFormat(LlmProvider provider) {
        return switch (provider) {
            case OPENAI, AZURE_OPENAI, CUSTOM, DASHSCOPE, MOONSHOT, DEEPSEEK, HUGGINGFACE -> true;
            case OLLAMA, ZHIPU, GEMINI -> false;
        };
    }

    /**
     * 创建 Azure OpenAI 模型
     */
    private ChatModel createAzureOpenAiModel(LlmModel config, double temperature, int maxTokens) {
        return createAzureOpenAiModel(config, temperature, maxTokens, 60);
    }

    /**
     * 创建 Azure OpenAI 模型（支持自定义超时）
     */
    private ChatModel createAzureOpenAiModel(LlmModel config, double temperature, int maxTokens, int timeoutSeconds) {
        // 注意：Azure OpenAI 需要不同的配置方式
        // 这里使用 OpenAI 兼容模式
        return OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getAzureDeploymentName() != null ? 
                          config.getAzureDeploymentName() : config.getModelName())
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .strictJsonSchema(true)
                .build();
    }

    /**
     * 创建 Ollama 模型
     */
    private ChatModel createOllamaModel(LlmModel config, double temperature, int maxTokens) {
        return createOllamaModel(config, temperature, maxTokens, 120);
    }

    /**
     * 创建 Ollama 模型（支持自定义超时）
     */
    private ChatModel createOllamaModel(LlmModel config, double temperature, int maxTokens, int timeoutSeconds) {
        String baseUrl = config.getBaseUrl() != null ? 
                config.getBaseUrl() : "http://localhost:11434";
        
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(config.getModelName())
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    /**
     * 创建智谱 AI 模型
     */
    private ChatModel createZhipuModel(LlmModel config, double temperature, int maxTokens) {
        return createZhipuModel(config, temperature, maxTokens, 60);
    }

    /**
     * 创建智谱 AI 模型（支持自定义超时）
     * 注意：ZhipuAiChatModel 可能不支持 timeout 参数，这里保持原样
     */
    private ChatModel createZhipuModel(LlmModel config, double temperature, int maxTokens, int timeoutSeconds) {
        // ZhipuAiChatModel 可能不支持 timeout，先保持原样
        return ZhipuAiChatModel.builder()
                .apiKey(config.getApiKey())
                .model(config.getModelName())
                .temperature(temperature)
                .maxToken(maxTokens)
                .build();
    }

    /**
     * 构建消息列表
     */
    private List<ChatMessage> buildMessages(String systemPrompt,
                                            String userMessage,
                                            List<ChatHistoryMessage> chatHistory) {
        List<ChatMessage> messages = new ArrayList<>();

        // 系统消息
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(SystemMessage.from(systemPrompt));
        }

        // 历史消息
        if (chatHistory != null) {
            for (ChatHistoryMessage history : chatHistory) {
                if ("user".equals(history.role())) {
                    messages.add(UserMessage.from(history.content()));
                } else if ("assistant".equals(history.role())) {
                    messages.add(AiMessage.from(history.content()));
                }
            }
        }

        // 当前用户消息
        messages.add(UserMessage.from(userMessage));

        return messages;
    }

    /**
     * 从消息列表构建 LangChain4j 消息
     */
    private List<ChatMessage> buildMessagesFromList(String systemPrompt, 
                                                     List<ChatHistoryMessage> messageList) {
        List<ChatMessage> messages = new ArrayList<>();

        String normalizedSystemPrompt = systemPrompt == null ? "" : systemPrompt.trim();
        boolean alreadyHasSameSystemPrompt = false;
        if (!normalizedSystemPrompt.isEmpty() && messageList != null) {
            for (ChatHistoryMessage msg : messageList) {
                if ("system".equals(msg.role())) {
                    String normalizedExisting = msg.content() == null ? "" : msg.content().trim();
                    if (normalizedSystemPrompt.equals(normalizedExisting)) {
                        alreadyHasSameSystemPrompt = true;
                        break;
                    }
                }
            }
        }

        if (!normalizedSystemPrompt.isEmpty() && !alreadyHasSameSystemPrompt) {
            messages.add(SystemMessage.from(normalizedSystemPrompt));
        }

        // 消息列表
        if (messageList != null) {
            String lastSystemContent = null;
            for (ChatHistoryMessage msg : messageList) {
                if ("user".equals(msg.role())) {
                    messages.add(UserMessage.from(msg.content()));
                } else if ("assistant".equals(msg.role())) {
                    messages.add(AiMessage.from(msg.content()));
                } else if ("system".equals(msg.role())) {
                    String normalized = msg.content() == null ? "" : msg.content().trim();
                    if (!normalized.isEmpty() && normalized.equals(lastSystemContent)) {
                        continue;
                    }
                    messages.add(SystemMessage.from(msg.content()));
                    lastSystemContent = normalized;
                }
            }
        }

        return messages;
    }

    /**
     * 估算 Token 数量（简单估算）
     */
    private int estimateTokens(List<ChatMessage> messages) {
        int total = 0;
        for (ChatMessage message : messages) {
            total += estimateTokens(message.toString());
        }
        return total;
    }

    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 简单估算：英文约 4 字符 1 token，中文约 1.5 字符 1 token
        int chineseCount = 0;
        int otherCount = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                chineseCount++;
            } else {
                otherCount++;
            }
        }
        return (int) (chineseCount / 1.5 + otherCount / 4);
    }

    // ==================== 数据类 ====================

    /**
     * 聊天响应
     */
    public record LlmChatResponse(
            boolean success,
            String reply,
            String errorMessage,
            UUID modelId,
            String modelName,
            long durationMs,
            int inputTokens,
            int outputTokens
    ) {
        public String content() {
            return reply;
        }
    }

    /**
     * 结构化输出响应
     */
    public record StructuredOutputResponse(
            boolean success,
            String jsonResult,
            String errorMessage,
            UUID modelId,
            String modelName,
            long durationMs
    ) {}

    /**
     * 字段 Schema 定义
     */
    public record FieldSchemaDefinition(
            String name,
            FieldType type,
            String description,
            boolean required,
            List<String> enumValues,
            List<FieldSchemaDefinition> properties,  // 嵌套属性（当type为OBJECT时）
            FieldSchemaDefinition items  // 数组元素定义（当type为ARRAY时）
    ) {
        public enum FieldType {
            STRING, INTEGER, NUMBER, BOOLEAN, ENUM, ARRAY, OBJECT
        }
    }

    /**
     * 聊天历史消息
     */
    public record ChatHistoryMessage(
            String role,  // "user" or "assistant"
            String content
    ) {}
}
