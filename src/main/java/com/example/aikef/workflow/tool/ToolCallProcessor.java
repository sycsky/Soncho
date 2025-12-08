package com.example.aikef.workflow.tool;

import com.example.aikef.extraction.model.ExtractionSchema;
import com.example.aikef.extraction.model.FieldDefinition;
import com.example.aikef.extraction.repository.ExtractionSchemaRepository;
import com.example.aikef.extraction.service.StructuredExtractionService;
import com.example.aikef.llm.LangChainChatService;
import com.example.aikef.llm.LlmModelService;
import com.example.aikef.tool.model.AiTool;
import com.example.aikef.tool.repository.AiToolRepository;
import com.example.aikef.tool.service.AiToolService;
import com.example.aikef.workflow.context.WorkflowContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.JsonSchemaProperty;
import dev.langchain4j.agent.tool.ToolParameters;
import dev.langchain4j.agent.tool.ToolSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 工具调用处理器
 * 负责处理 LLM 的工具调用请求，包括：
 * 1. 构建工具规格（ToolSpecification）
 * 2. 解析工具调用请求
 * 3. 参数提取与多轮对话
 * 4. 工具执行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolCallProcessor {

    private final AiToolRepository toolRepository;
    private final AiToolService toolService;
    private final StructuredExtractionService extractionService;
    private final ExtractionSchemaRepository schemaRepository;
    private final LlmModelService llmModelService;
    private final ObjectMapper objectMapper;

    /**
     * 根据工具ID列表构建 ToolSpecification 列表
     */
    public List<ToolSpecification> buildToolSpecifications(List<UUID> toolIds) {
        List<ToolSpecification> specifications = new ArrayList<>();

        for (UUID toolId : toolIds) {
            // 使用带 Schema 的查询避免 LazyInitializationException
            AiTool tool = toolRepository.findByIdWithSchema(toolId).orElse(null);
            if (tool == null || !tool.getEnabled()) {
                log.warn("工具不存在或已禁用: {}", toolId);
                continue;
            }

            ToolSpecification spec = buildToolSpecification(tool);
            if (spec != null) {
                specifications.add(spec);
            }
        }

        return specifications;
    }

    /**
     * 构建单个工具的 ToolSpecification
     */
    public ToolSpecification buildToolSpecification(AiTool tool) {
        try {
            ToolSpecification.Builder builder = ToolSpecification.builder()
                    .name(tool.getName())
                    .description(tool.getDescription());

            // 从关联的 Schema 构建参数
            if (tool.getSchema() != null && tool.getSchema().getFieldsJson() != null) {
                List<FieldDefinition> fields = objectMapper.readValue(
                        tool.getSchema().getFieldsJson(),
                        new TypeReference<List<FieldDefinition>>() {}
                );

                if (!fields.isEmpty()) {


                    // 添加参数属性
                    for (FieldDefinition field : fields) {
                        String desc = field.getDescription() != null ? 
                                field.getDescription() : field.getDisplayName();
                        
                        // 如果是必填参数，在描述中标注
//                        if (Boolean.TRUE.equals(field.isRequired())) {
//                            desc = "[必填] " + desc;
//                        }else{
//                            desc = "[选填] " + desc;
//                        }
                        //都设置为选填可以在触发工具后调用必填验证
                        desc = "[选填] " + desc;
                        builder.addParameter(
                                field.getName(),
                                JsonSchemaProperty.description(desc),
                                mapFieldTypeToJsonSchemaProperty(field)
                        );
                    }
                    

                }
            }

            return builder.build();
        } catch (Exception e) {
            log.error("构建工具规格失败: tool={}", tool.getName(), e);
            return null;
        }
    }

    /**
     * 将字段定义映射为 JsonSchemaProperty
     */
    private JsonSchemaProperty mapFieldTypeToJsonSchemaProperty(FieldDefinition field) {
        String description = field.getDescription() != null ? field.getDescription() : field.getDisplayName();

        // JsonSchemaProperty 使用静态工厂方法
        return switch (field.getType()) {
            case STRING, DATE, DATETIME, EMAIL, PHONE -> JsonSchemaProperty.STRING;
            case INTEGER -> JsonSchemaProperty.INTEGER;
            case NUMBER -> JsonSchemaProperty.NUMBER;
            case BOOLEAN -> JsonSchemaProperty.BOOLEAN;
            case ENUM -> JsonSchemaProperty.enums(field.getEnumValues().toArray(new String[0]));
            case ARRAY -> JsonSchemaProperty.ARRAY;
            case OBJECT -> JsonSchemaProperty.OBJECT;
        };
    }

    /**
     * 处理工具调用
     * 检测是否有缺失参数，如果有则启动参数收集
     *
     * @param toolCallState 工具调用状态
     * @param userMessage 用户原始消息
     * @return 处理结果
     */
    public ToolCallProcessResult processToolCall(ToolCallState toolCallState, String userMessage) {
        ToolCallState.ToolCallRequest request = toolCallState.getCurrentToolCall();
        if (request == null) {
            return ToolCallProcessResult.error("没有待处理的工具调用");
        }

        // 获取工具（带 Schema）
        AiTool tool = toolRepository.findByNameWithSchema(request.getToolName()).orElse(null);
        if (tool == null && request.getToolId() != null) {
            tool = toolRepository.findByIdWithSchema(request.getToolId()).orElse(null);
        }

        if (tool == null) {
            return ToolCallProcessResult.error("工具不存在: " + request.getToolName());
        }

        // 获取参数定义
        List<FieldDefinition> paramDefs = getToolParameters(tool);
        List<String> requiredParams = paramDefs.stream()
                .filter(FieldDefinition::isRequired)
                .map(FieldDefinition::getName)
                .toList();

        log.info("工具参数检查: tool={}, 总参数数={}, 必填参数={}", 
                tool.getName(), paramDefs.size(), requiredParams);

        // 合并 LLM 提取的参数和已收集的参数
        Map<String, Object> allParams = new HashMap<>(toolCallState.getCollectedParams());
        if (request.getArguments() != null) {
            allParams.putAll(request.getArguments());
        }
        
        log.info("当前参数值: {}", allParams);

        // 检查缺失参数
        List<String> missingParams = new ArrayList<>();
        for (String param : requiredParams) {
            Object value = allParams.get(param);
            boolean isMissing = !allParams.containsKey(param) || value == null || "null".equals(value) || "Null".equals(value) ||
                    (value instanceof String && ((String) value).isEmpty());
            if (isMissing) {
                missingParams.add(param);
                log.debug("缺失必填参数: param={}, value={}", param, value);
            }
        }
        
        log.info("缺失参数: {}", missingParams);

        toolCallState.setCollectedParams(allParams);
        toolCallState.setMissingParams(missingParams);

        if (missingParams.isEmpty()) {
            // 所有参数都有了，执行工具
            return executeToolWithParams(tool, allParams, request.getId());
        } else {
            // 有缺失参数，需要收集
            String nextQuestion = buildFollowupQuestion(paramDefs, missingParams);
            toolCallState.setNextQuestion(nextQuestion);
            toolCallState.setStatus(ToolCallState.Status.WAITING_USER_INPUT);
            toolCallState.setCurrentRound(toolCallState.getCurrentRound() + 1);

            return ToolCallProcessResult.needMoreParams(nextQuestion, missingParams);
        }
    }

    /**
     * 继续参数收集（用户回答了追问）
     */
    public ToolCallProcessResult continueParamCollection(
            ToolCallState toolCallState,
            String userResponse,
            UUID llmModelId) {

        ToolCallState.ToolCallRequest request = toolCallState.getCurrentToolCall();
        if (request == null) {
            return ToolCallProcessResult.error("没有待处理的工具调用");
        }

        // 获取工具（带 Schema）
        AiTool tool = toolRepository.findByNameWithSchema(request.getToolName()).orElse(null);
        if (tool == null && request.getToolId() != null) {
            tool = toolRepository.findByIdWithSchema(request.getToolId()).orElse(null);
        }

        if (tool == null || tool.getSchema() == null) {
            return ToolCallProcessResult.error("工具或参数定义不存在");
        }

        // 检查是否超过最大轮次
        if (toolCallState.getCurrentRound() >= toolCallState.getMaxRounds()) {
            toolCallState.setStatus(ToolCallState.Status.SKIPPED);
            return ToolCallProcessResult.skipped("参数收集超过最大轮次，跳过工具执行");
        }

        try {
            // 使用结构化提取从用户回复中提取参数
            List<FieldDefinition> paramDefs = getToolParameters(tool);
            Map<String, Object> extractedParams = extractParamsFromText(
                    userResponse,
                    paramDefs,
                    toolCallState.getMissingParams(),
                    llmModelId
            );

            // 合并参数
            Map<String, Object> allParams = new HashMap<>(toolCallState.getCollectedParams());
            allParams.putAll(extractedParams);
            toolCallState.setCollectedParams(allParams);

            // 重新检查缺失参数
            List<String> requiredParams = paramDefs.stream()
                    .filter(FieldDefinition::isRequired)
                    .map(FieldDefinition::getName)
                    .toList();

            List<String> stillMissing = new ArrayList<>();
            for (String param : requiredParams) {
                if (!allParams.containsKey(param) || allParams.get(param) == null ||
                        (allParams.get(param) instanceof String && ((String) allParams.get(param)).isEmpty())) {
                    stillMissing.add(param);
                }
            }

            toolCallState.setMissingParams(stillMissing);

            if (stillMissing.isEmpty()) {
                // 参数收集完成，执行工具
                return executeToolWithParams(tool, allParams, request.getId());
            } else {
                // 还有缺失参数
                String nextQuestion = buildFollowupQuestion(paramDefs, stillMissing);
                toolCallState.setNextQuestion(nextQuestion);
                toolCallState.setStatus(ToolCallState.Status.WAITING_USER_INPUT);
                toolCallState.setCurrentRound(toolCallState.getCurrentRound() + 1);

                return ToolCallProcessResult.needMoreParams(nextQuestion, stillMissing);
            }

        } catch (Exception e) {
            log.error("参数收集失败", e);
            toolCallState.setStatus(ToolCallState.Status.TOOL_FAILED);
            return ToolCallProcessResult.error("参数收集失败: " + e.getMessage());
        }
    }

    /**
     * 从文本中提取参数
     */
    private Map<String, Object> extractParamsFromText(
            String text,
            List<FieldDefinition> paramDefs,
            List<String> targetParams,
            UUID llmModelId) {

        try {
            // 构建提取提示
            StringBuilder prompt = new StringBuilder();
            prompt.append("请从以下文本中提取信息，以 JSON 格式返回。\n\n");
            prompt.append("文本：").append(text).append("\n\n");
            prompt.append("需要提取的字段：\n");

            for (FieldDefinition field : paramDefs) {
                if (targetParams.contains(field.getName())) {
                    prompt.append("- ").append(field.getName()).append(": ")
                            .append(field.getDescription() != null ? field.getDescription() : field.getDisplayName())
                            .append("\n");
                }
            }

            prompt.append("\n只返回 JSON 对象，不要有其他文字。如果某个字段无法从文本中提取，设为 null。");

            // 调用 LLM 提取
            var defaultModel = llmModelService.getDefaultModel();
            UUID modelId = llmModelId != null ? llmModelId : (defaultModel.isPresent() ? defaultModel.get().getId() : null);

            // 这里简化处理，直接用 simpleChat
            // 实际可以用 ResponseFormat 做结构化提取
            String response = extractionService.extractAsJson(modelId, prompt.toString());

            // 解析 JSON
            if (response != null && response.contains("{")) {
                int start = response.indexOf("{");
                int end = response.lastIndexOf("}") + 1;
                String json = response.substring(start, end);
                return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            }

        } catch (Exception e) {
            log.warn("提取参数失败: {}", e.getMessage());
        }

        return new HashMap<>();
    }

    /**
     * 使用参数执行工具
     */
    private ToolCallProcessResult executeToolWithParams(AiTool tool, Map<String, Object> params, String toolCallId) {
        try {
            log.info("执行工具: tool={}, params={}", tool.getName(), params);

            AiToolService.ToolExecutionResult result = toolService.executeTool(
                    tool.getId(),
                    params,
                    null,
                    null
            );

            if (result.success()) {
                return ToolCallProcessResult.success(
                        new ToolCallState.ToolCallResult(
                                toolCallId,
                                tool.getName(),
                                true,
                                result.output(),
                                null,
                                result.durationMs()
                        )
                );
            } else {
                return ToolCallProcessResult.failed(
                        new ToolCallState.ToolCallResult(
                                toolCallId,
                                tool.getName(),
                                false,
                                null,
                                result.errorMessage(),
                                result.durationMs()
                        )
                );
            }

        } catch (Exception e) {
            log.error("工具执行失败: tool={}", tool.getName(), e);
            return ToolCallProcessResult.error("工具执行异常: " + e.getMessage());
        }
    }

    /**
     * 构建追问问题（一次询问所有缺失参数）
     */
    private String buildFollowupQuestion(List<FieldDefinition> paramDefs, List<String> missingParams) {
        log.debug("构建追问问题: paramDefs.size={}, missingParams={}", 
                paramDefs != null ? paramDefs.size() : 0, missingParams);

        if (missingParams == null || missingParams.isEmpty()) {
            return "请提供更多信息以完成操作。";
        }

        // 收集所有缺失参数的问题
        List<String> questions = new ArrayList<>();
        
        for (String paramName : missingParams) {
            // 查找对应的字段定义
            FieldDefinition field = null;
            for (FieldDefinition f : paramDefs) {
                if (f.getName().equals(paramName)) {
                    field = f;
                    break;
                }
            }
            
            if (field != null) {
                // 优先使用配置的追问问题
                if (field.getFollowupQuestion() != null && !field.getFollowupQuestion().isEmpty()) {
                    questions.add(field.getFollowupQuestion());
                } else {
                    // 自动生成问题描述
                    String displayName = field.getDisplayName() != null ? field.getDisplayName() : field.getName();
                    if (field.getDescription() != null && !field.getDescription().isEmpty()) {
                        questions.add(displayName + "（" + field.getDescription() + "）");
                    } else {
                        questions.add(displayName);
                    }
                }
            } else {
                // 没有字段定义，直接使用参数名
                questions.add(paramName);
            }
        }
        
        // 组合问题
        if (questions.size() == 1) {
            String question = questions.get(0);
            // 如果是配置的完整问题（包含问号或请字），直接返回
            if (question.contains("?") || question.contains("？") || question.startsWith("请")) {
                return question;
            }
            return "请提供" + question;
        } else {
            // 多个缺失参数，组合成一个问题
            StringBuilder sb = new StringBuilder();
            sb.append("请提供以下信息：\n");
            for (int i = 0; i < questions.size(); i++) {
                sb.append(i + 1).append(". ").append(questions.get(i));
                if (i < questions.size() - 1) {
                    sb.append("\n");
                }
            }
            String combinedQuestion = sb.toString();
            log.debug("组合追问问题: {}", combinedQuestion);
            return combinedQuestion;
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
     * 根据工具名称获取工具ID
     */
    public UUID getToolIdByName(String toolName) {
        return toolRepository.findByName(toolName)
                .map(AiTool::getId)
                .orElse(null);
    }

    // ==================== 结果类 ====================

    /**
     * 工具调用处理结果
     */
    public static class ToolCallProcessResult {
        private ResultType type;
        private ToolCallState.ToolCallResult result;
        private String question;
        private List<String> missingParams;
        private String errorMessage;

        public enum ResultType {
            SUCCESS,           // 工具执行成功
            FAILED,            // 工具执行失败
            NEED_MORE_PARAMS,  // 需要更多参数
            SKIPPED,           // 跳过执行
            ERROR              // 处理错误
        }

        public static ToolCallProcessResult success(ToolCallState.ToolCallResult result) {
            ToolCallProcessResult r = new ToolCallProcessResult();
            r.type = ResultType.SUCCESS;
            r.result = result;
            return r;
        }

        public static ToolCallProcessResult failed(ToolCallState.ToolCallResult result) {
            ToolCallProcessResult r = new ToolCallProcessResult();
            r.type = ResultType.FAILED;
            r.result = result;
            return r;
        }

        public static ToolCallProcessResult needMoreParams(String question, List<String> missingParams) {
            ToolCallProcessResult r = new ToolCallProcessResult();
            r.type = ResultType.NEED_MORE_PARAMS;
            r.question = question;
            r.missingParams = missingParams;
            return r;
        }

        public static ToolCallProcessResult skipped(String reason) {
            ToolCallProcessResult r = new ToolCallProcessResult();
            r.type = ResultType.SKIPPED;
            r.errorMessage = reason;
            return r;
        }

        public static ToolCallProcessResult error(String message) {
            ToolCallProcessResult r = new ToolCallProcessResult();
            r.type = ResultType.ERROR;
            r.errorMessage = message;
            return r;
        }

        public ResultType getType() {
            return type;
        }

        public ToolCallState.ToolCallResult getResult() {
            return result;
        }

        public String getQuestion() {
            return question;
        }

        public List<String> getMissingParams() {
            return missingParams;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean isSuccess() {
            return type == ResultType.SUCCESS;
        }

        public boolean needsUserInput() {
            return type == ResultType.NEED_MORE_PARAMS;
        }
    }
}

