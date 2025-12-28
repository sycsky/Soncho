package com.example.aikef.tool.service;

import com.example.aikef.extraction.model.ExtractionSchema;
import com.example.aikef.extraction.model.FieldDefinition;
import com.example.aikef.extraction.repository.ExtractionSchemaRepository;
import com.example.aikef.tool.model.AiTool;
import com.example.aikef.tool.model.ToolExecution;
import com.example.aikef.tool.repository.AiToolRepository;
import com.example.aikef.tool.repository.ToolExecutionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 工具服务
 * 管理工具的 CRUD 和执行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiToolService {

    private final AiToolRepository toolRepository;
    private final ToolExecutionRepository executionRepository;
    private final ExtractionSchemaRepository schemaRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final com.example.aikef.service.ChatSessionService chatSessionService;

    // ==================== 工具 CRUD ====================

    /**
     * 创建工具
     */
    @Transactional
    public AiTool createTool(CreateToolRequest request, UUID createdBy) {
        if (toolRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("工具名称已存在: " + request.name());
        }

        AiTool tool = new AiTool();
        // AiTool 的 ID 由应用层生成（支持 upsert-by-name 复用旧 ID）
        tool.setId(UUID.randomUUID());
        tool.setName(request.name());
        tool.setDisplayName(request.displayName());
        tool.setDescription(request.description());
        tool.setToolType(request.toolType());

        // 创建关联的 ExtractionSchema（1对1）
        if (request.parameters() != null && !request.parameters().isEmpty()) {
            ExtractionSchema schema = createSchemaForTool(request.name(), request.parameters());
            tool.setSchema(schema);
        }

        // API 配置
        if (request.toolType() == AiTool.ToolType.API) {
            tool.setApiMethod(request.apiMethod());
            tool.setApiUrl(request.apiUrl());
            tool.setApiHeaders(request.apiHeaders());
            tool.setApiBodyTemplate(request.apiBodyTemplate());
            tool.setApiResponsePath(request.apiResponsePath());
            tool.setApiTimeout(request.apiTimeout() != null ? request.apiTimeout() : 30);
        }

        // MCP 配置
        if (request.toolType() == AiTool.ToolType.MCP) {
            tool.setMcpEndpoint(request.mcpEndpoint());
            tool.setMcpToolName(request.mcpToolName());
            tool.setMcpServerType(request.mcpServerType());
            tool.setMcpConfig(request.mcpConfig());
        }

        // 认证配置
        tool.setAuthType(request.authType() != null ? request.authType() : AiTool.AuthType.NONE);
        tool.setAuthConfig(request.authConfig());

        // 其他配置
        tool.setInputExample(request.inputExample());
        tool.setOutputExample(request.outputExample());
        tool.setResultDescription(request.resultDescription());
        tool.setResultMetadata(request.resultMetadata());
        tool.setRetryCount(request.retryCount() != null ? request.retryCount() : 0);
        tool.setRequireConfirmation(request.requireConfirmation() != null ? request.requireConfirmation() : false);
        tool.setEnabled(true);
        tool.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        tool.setTags(request.tags());
        tool.setCreatedBy(createdBy);

        AiTool saved = toolRepository.save(tool);
        log.info("创建工具: id={}, name={}, type={}, schemaId={}",
                saved.getId(), saved.getName(), saved.getToolType(),
                saved.getSchema() != null ? saved.getSchema().getId() : null);
        return saved;
    }

    /**
     * 更新工具
     */
    @Transactional
    public AiTool updateTool(UUID toolId, UpdateToolRequest request) {
        AiTool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("工具不存在: " + toolId));

        if (request.displayName() != null) {
            tool.setDisplayName(request.displayName());
        }
        if (request.description() != null) {
            tool.setDescription(request.description());
        }

        // 更新参数定义：直接更新已有 Schema 或创建新 Schema
        if (request.parameters() != null) {
            if (tool.getSchema() != null) {
                // 直接更新已有 Schema（避免唯一键冲突）
                updateSchemaForTool(tool.getSchema(), request.parameters());
                log.info("更新 Schema: schemaId={}", tool.getSchema().getId());
            } else if (!request.parameters().isEmpty()) {
                // 创建新的 Schema
                ExtractionSchema newSchema = createSchemaForTool(tool.getName(), request.parameters());
                tool.setSchema(newSchema);
                log.info("创建新 Schema: name={}", newSchema.getName());
            }
        }

        // API 配置更新
        if (request.apiMethod() != null) tool.setApiMethod(request.apiMethod());
        if (request.apiUrl() != null) tool.setApiUrl(request.apiUrl());
        if (request.apiHeaders() != null) tool.setApiHeaders(request.apiHeaders());
        if (request.apiBodyTemplate() != null) tool.setApiBodyTemplate(request.apiBodyTemplate());
        if (request.apiResponsePath() != null) tool.setApiResponsePath(request.apiResponsePath());
        if (request.apiTimeout() != null) tool.setApiTimeout(request.apiTimeout());


        // MCP 配置更新
        if (request.mcpEndpoint() != null) tool.setMcpEndpoint(request.mcpEndpoint());
        if (request.mcpToolName() != null) tool.setMcpToolName(request.mcpToolName());
        if (request.mcpServerType() != null) tool.setMcpServerType(request.mcpServerType());
        if (request.mcpConfig() != null) tool.setMcpConfig(request.mcpConfig());

        // 认证配置更新
        if (request.authType() != null) tool.setAuthType(request.authType());
        if (request.authConfig() != null) tool.setAuthConfig(request.authConfig());

        // 其他配置更新
        if (request.inputExample() != null) tool.setInputExample(request.inputExample());
        if (request.outputExample() != null) tool.setOutputExample(request.outputExample());
        if (request.resultDescription() != null) tool.setResultDescription(request.resultDescription());
        if (request.resultMetadata() != null) tool.setResultMetadata(request.resultMetadata());
        if (request.retryCount() != null) tool.setRetryCount(request.retryCount());
        if (request.requireConfirmation() != null) tool.setRequireConfirmation(request.requireConfirmation());
        if (request.enabled() != null) tool.setEnabled(request.enabled());
        if (request.sortOrder() != null) tool.setSortOrder(request.sortOrder());
        if (request.tags() != null) tool.setTags(request.tags());

        log.info("更新工具: id={}, schemaId={}",
                toolId, tool.getSchema() != null ? tool.getSchema().getId() : null);
        return toolRepository.save(tool);
    }

    /**
     * 获取工具（带 Schema）
     */
    @Transactional(readOnly = true)
    public Optional<AiTool> getTool(UUID toolId) {
        return toolRepository.findByIdWithSchema(toolId);
    }

    /**
     * 根据名称获取工具（带 Schema）
     */
    @Transactional(readOnly = true)
    public Optional<AiTool> getToolByName(String name) {
        return toolRepository.findByNameWithSchema(name);
    }

    /**
     * 获取所有启用的工具
     */
    @Transactional(readOnly = true)
    public List<AiTool> getEnabledTools() {
        return toolRepository.findEnabledToolsWithSchema();
    }

    /**
     * 获取有参数定义的工具
     */
    @Transactional(readOnly = true)
    public List<AiTool> getToolsWithSchema() {
        return toolRepository.findEnabledToolsWithSchema();
    }

    /**
     * 获取工具的参数定义
     */
    @Transactional(readOnly = true)
    public List<FieldDefinition> getToolParameters(UUID toolId) {
        AiTool tool = toolRepository.findByIdWithSchema(toolId)
                .orElseThrow(() -> new IllegalArgumentException("工具不存在: " + toolId));

        if (tool.getSchema() == null || tool.getSchema().getFieldsJson() == null) {
            return Collections.emptyList();
        }

        return parseFieldsJson(tool.getSchema().getFieldsJson());
    }

    /**
     * 搜索工具
     */
    public List<AiTool> searchTools(String keyword) {
        return toolRepository.searchByKeyword(keyword);
    }

    /**
     * 删除工具
     */
    @Transactional
    public void deleteTool(UUID toolId) {
        AiTool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("工具不存在: " + toolId));
        // 先删除执行记录，避免外键约束导致无法删除工具
        executionRepository.deleteByTool_Id(toolId);
        toolRepository.delete(tool);
        log.info("删除工具: id={}, name={}", toolId, tool.getName());
    }

    /**
     * 覆盖创建工具（按 name 幂等）
     * <p>
     * - 若存在同名工具：立即删除旧工具（含执行记录/Schema），并复用旧工具 ID 创建新工具
     * - 若不存在：等同于 createTool
     */
    @Transactional
    public AiTool createOrReplaceToolByName(CreateToolRequest request, UUID createdBy) {
        UUID reuseId = null;
        String oldApiBodyTemplate = null; // 保存旧的 apiBodyTemplate

        AiTool existing = toolRepository.findByNameWithSchema(request.name()).orElse(null);
        if (existing != null) {
            reuseId = existing.getId();
            // 保存旧的 apiBodyTemplate
            oldApiBodyTemplate = existing.getApiBodyTemplate();
            // 先删执行记录（外键）
            executionRepository.deleteByTool_Id(reuseId);
            // 再删工具（Schema 会 orphanRemoval 级联删除）
            toolRepository.delete(existing);
            // 立即 flush，避免 name 唯一键/外键约束影响后续插入
            toolRepository.flush();
        } else if (toolRepository.existsByName(request.name())) {
            // 兜底：防止并发或未 fetch 到的情况
            AiTool existing2 = toolRepository.findByName(request.name()).orElse(null);
            if (existing2 != null) {
                reuseId = existing2.getId();
                // 保存旧的 apiBodyTemplate
                oldApiBodyTemplate = existing2.getApiBodyTemplate();
                executionRepository.deleteByTool_Id(reuseId);
                toolRepository.delete(existing2);
                toolRepository.flush();
            }
        }

        AiTool tool = new AiTool();
        // 复用旧 ID；若不存在旧工具则生成新 ID
        tool.setId(reuseId != null ? reuseId : UUID.randomUUID());
        tool.setName(request.name());
        tool.setDisplayName(request.displayName());
        tool.setDescription(request.description());
        tool.setToolType(request.toolType());

        // 创建关联的 ExtractionSchema（1对1）
        if (request.parameters() != null && !request.parameters().isEmpty()) {
            ExtractionSchema schema = createSchemaForTool(request.name(), request.parameters());
            tool.setSchema(schema);
        }

        // API 配置
        if (request.toolType() == AiTool.ToolType.API) {
            tool.setApiMethod(request.apiMethod());
            tool.setApiUrl(request.apiUrl());
            tool.setApiHeaders(request.apiHeaders());
            // 如果有旧的 apiBodyTemplate 且新请求的为空，则继承旧的
            String apiBodyTemplate = request.apiBodyTemplate();
            if ((apiBodyTemplate == null || apiBodyTemplate.trim().isEmpty()) && oldApiBodyTemplate != null) {
                apiBodyTemplate = oldApiBodyTemplate;
                log.info("继承旧的 apiBodyTemplate: toolName={}", request.name());
            }
            tool.setApiBodyTemplate(apiBodyTemplate);
            tool.setApiResponsePath(request.apiResponsePath());
            tool.setApiTimeout(request.apiTimeout() != null ? request.apiTimeout() : 30);
        }

        // MCP 配置
        if (request.toolType() == AiTool.ToolType.MCP) {
            tool.setMcpEndpoint(request.mcpEndpoint());
            tool.setMcpToolName(request.mcpToolName());
            tool.setMcpServerType(request.mcpServerType());
            tool.setMcpConfig(request.mcpConfig());
        }

        // 认证配置
        tool.setAuthType(request.authType() != null ? request.authType() : AiTool.AuthType.NONE);
        tool.setAuthConfig(request.authConfig());

        // 其他配置
        tool.setInputExample(request.inputExample());
        tool.setOutputExample(request.outputExample());
        tool.setResultDescription(request.resultDescription());
        tool.setResultMetadata(request.resultMetadata());
        tool.setRetryCount(request.retryCount() != null ? request.retryCount() : 0);
        tool.setRequireConfirmation(request.requireConfirmation() != null ? request.requireConfirmation() : false);
        tool.setEnabled(true);
        tool.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        tool.setTags(request.tags());
        tool.setCreatedBy(createdBy);

        AiTool saved = toolRepository.save(tool);
        log.info("覆盖创建工具: id={}, name={}, type={}, schemaId={}",
                saved.getId(), saved.getName(), saved.getToolType(),
                saved.getSchema() != null ? saved.getSchema().getId() : null);
        return saved;
    }

    // ==================== 工具执行 ====================

    /**
     * 执行工具
     */
    @Transactional
    public ToolExecutionResult executeTool(UUID toolId, Map<String, Object> params, UUID sessionId, UUID executedBy) {
        AiTool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("工具不存在: " + toolId));

        if (!tool.getEnabled()) {
            throw new IllegalStateException("工具已禁用: " + tool.getName());
        }

        // 创建执行记录
        ToolExecution execution = new ToolExecution();
        execution.setTool(tool);
        execution.setSessionId(sessionId);
        execution.setStatus(ToolExecution.ExecutionStatus.RUNNING);
        execution.setInputParams(serializeParams(params));
        execution.setStartedAt(Instant.now());
        execution.setExecutedBy(executedBy);
        execution.setTriggerSource("API");
        executionRepository.save(execution);

        try {
            ToolExecutionResult result;

            if (tool.getToolType() == AiTool.ToolType.API) {
                result = executeApiTool(tool, params, sessionId);
            } else if (tool.getToolType() == AiTool.ToolType.MCP) {
                result = executeMcpTool(tool, params);
            } else {
                throw new IllegalArgumentException("不支持的工具类型: " + tool.getToolType());
            }

            // 更新执行记录
            execution.setStatus(result.success() ? ToolExecution.ExecutionStatus.SUCCESS : ToolExecution.ExecutionStatus.FAILED);
            execution.setOutputResult(result.output());
            execution.setErrorMessage(result.errorMessage());
            execution.setHttpStatus(result.httpStatus());
            execution.setFinishedAt(Instant.now());
            execution.setDurationMs(execution.getFinishedAt().toEpochMilli() - execution.getStartedAt().toEpochMilli());
            executionRepository.save(execution);

            log.info("工具执行完成: toolId={}, status={}, duration={}ms",
                    toolId, result.success() ? "SUCCESS" : "FAILED", execution.getDurationMs());

            return new ToolExecutionResult(
                    result.success(),
                    result.output(),
                    result.errorMessage(),
                    result.httpStatus(),
                    execution.getDurationMs(),
                    execution.getId()
            );

        } catch (Exception e) {
            log.error("工具执行异常: toolId={}", toolId, e);

            execution.setStatus(ToolExecution.ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setFinishedAt(Instant.now());
            execution.setDurationMs(execution.getFinishedAt().toEpochMilli() - execution.getStartedAt().toEpochMilli());
            executionRepository.save(execution);

            return new ToolExecutionResult(false, null, e.getMessage(), null, execution.getDurationMs(), execution.getId());
        }
    }

    /**
     * 异步执行工具
     */
    @Async
    public CompletableFuture<ToolExecutionResult> executeToolAsync(UUID toolId, Map<String, Object> params, UUID sessionId, UUID executedBy) {
        return CompletableFuture.completedFuture(executeTool(toolId, params, sessionId, executedBy));
    }

    /**
     * 执行 API 工具（支持网络异常重试）
     */
    private ToolExecutionResult executeApiTool(AiTool tool, Map<String, Object> params, UUID sessionId) {
        int maxRetries = tool.getRetryCount() != null ? tool.getRetryCount() : 0;
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount <= maxRetries) {
            try {
                // 如果是重试，记录日志
                if (retryCount > 0) {
                    log.info("API 工具重试中: tool={}, 第 {}/{} 次重试",
                            tool.getName(), retryCount, maxRetries);
                    // 重试前等待一段时间（指数退避）
                    Thread.sleep(1000L * retryCount);
                }

                return executeApiToolInternal(tool, params, sessionId);

            } catch (Exception e) {
                lastException = e;

                // 判断是否是网络问题（可重试）
                if (isNetworkException(e) && retryCount < maxRetries) {
                    log.warn("API 工具执行网络异常，准备重试: tool={}, retry={}/{}, error={}",
                            tool.getName(), retryCount + 1, maxRetries, e.getMessage());
                    retryCount++;
                } else {
                    // 非网络问题或已达到最大重试次数，直接返回失败
                    log.error("API 工具执行失败: tool={}, retries={}", tool.getName(), retryCount, e);
                    break;
                }
            }
        }

        return new ToolExecutionResult(false, null,
                lastException != null ? lastException.getMessage() : "执行失败",
                null, null, null);
    }

    /**
     * 实际执行 API 工具调用
     */
    private ToolExecutionResult executeApiToolInternal(AiTool tool, Map<String, Object> params, UUID sessionId)
            throws Exception {
        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (tool.getApiHeaders() != null && !tool.getApiHeaders().isEmpty()) {
            Map<String, String> customHeaders = objectMapper.readValue(tool.getApiHeaders(),
                    new TypeReference<Map<String, String>>() {
                    });
            // 替换请求头中的变量（包括 metadata）
            customHeaders.forEach((key, value) -> {
                String replacedValue = replaceVariables(value, params, sessionId);
                headers.set(key, replacedValue);
            });
        }

        // 处理认证
        applyAuthentication(headers, tool, params);

        // 构建请求 URL（替换变量，包括会话元数据）
        String url = replaceVariables(tool.getApiUrl(), params, sessionId);

        // 构建请求体
        String body = null;
        if (tool.getApiBodyTemplate() != null && !tool.getApiBodyTemplate().isEmpty()) {
            body = replaceVariables(tool.getApiBodyTemplate(), params, sessionId);
        } else if (params != null && !params.isEmpty()) {
            body = objectMapper.writeValueAsString(params);
        }

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        // 发送请求
        HttpMethod method = HttpMethod.valueOf(tool.getApiMethod().toUpperCase());
        ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);

        // 解析响应
        String output = response.getBody();
        if (tool.getApiResponsePath() != null && !tool.getApiResponsePath().isEmpty()) {
            output = extractByJsonPath(response.getBody(), tool.getApiResponsePath());
        }

        return new ToolExecutionResult(
                response.getStatusCode().is2xxSuccessful(),
                output,
                null,
                response.getStatusCode().value(),
                null,
                null
        );
    }

    /**
     * 判断是否是网络异常（可重试）
     */
    private boolean isNetworkException(Exception e) {
        // ResourceAccessException 是 RestTemplate 封装的网络异常
        if (e instanceof ResourceAccessException) {
            return true;
        }

        // 检查根本原因
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof ConnectException ||
                    cause instanceof SocketTimeoutException ||
                    cause instanceof UnknownHostException ||
                    cause instanceof java.net.NoRouteToHostException ||
                    cause instanceof java.net.PortUnreachableException) {
                return true;
            }
            cause = cause.getCause();
        }

        // 检查异常消息
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("connection refused") ||
                    lowerMessage.contains("connection timed out") ||
                    lowerMessage.contains("read timed out") ||
                    lowerMessage.contains("connect timed out") ||
                    lowerMessage.contains("network is unreachable") ||
                    lowerMessage.contains("no route to host") ||
                    lowerMessage.contains("unknown host")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 执行 MCP 工具
     */
    private ToolExecutionResult executeMcpTool(AiTool tool, Map<String, Object> params) {
        // TODO: 实现 MCP 工具调用
        // MCP (Model Context Protocol) 需要根据具体实现来处理
        // 可能需要：
        // 1. stdio 方式：启动进程并通过标准输入输出通信
        // 2. SSE 方式：HTTP SSE 连接
        // 3. WebSocket 方式：WebSocket 连接

        log.warn("MCP 工具暂未实现: tool={}", tool.getName());
        return new ToolExecutionResult(false, null, "MCP 工具暂未实现", null, null, null);
    }

    /**
     * 应用认证
     */
    private void applyAuthentication(HttpHeaders headers, AiTool tool, Map<String, Object> params) {
        if (tool.getAuthType() == null || tool.getAuthType() == AiTool.AuthType.NONE) {
            return;
        }

        try {
            Map<String, String> authConfig = tool.getAuthConfig() != null
                    ? objectMapper.readValue(tool.getAuthConfig(), new TypeReference<Map<String, String>>() {
            })
                    : new HashMap<>();

            switch (tool.getAuthType()) {
                case API_KEY -> {
                    String headerName = authConfig.getOrDefault("headerName", "X-API-Key");
                    String apiKey = authConfig.get("apiKey");
                    if (apiKey != null) {
                        headers.set(headerName, apiKey);
                    }
                }
                case BEARER -> {
                    String token = authConfig.get("token");
                    if (token != null) {
                        headers.setBearerAuth(token);
                    }
                }
                case BASIC -> {
                    String username = authConfig.get("username");
                    String password = authConfig.get("password");
                    if (username != null && password != null) {
                        headers.setBasicAuth(username, password);
                    }
                }
                case OAUTH2 -> {
                    // OAuth2 需要更复杂的实现，这里简化处理
                    String accessToken = authConfig.get("accessToken");
                    if (accessToken != null) {
                        headers.setBearerAuth(accessToken);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("应用认证失败: tool={}", tool.getName(), e);
        }
    }

    /**
     * 替换变量
     * 支持三种格式：
     * - {{paramName}} - 从 params 中获取参数值
     * - {{meta.key}} - 从会话 metadata 中获取元数据值
     * - {{sessionId}} - 会话ID
     */
    private String replaceVariables(String template, Map<String, Object> params, UUID sessionId) {
        if (template == null) {
            return template;
        }

        String result = template;

        // 匹配 {{xxx}} 或 {{meta.xxx}} 格式
        // 支持字母、数字、下划线和点号
        Pattern pattern = Pattern.compile("\\{\\{([\\w.]+)}}");
        Matcher matcher = pattern.matcher(template);

        while (matcher.find()) {
            String varName = matcher.group(1);
            String fullMatch = matcher.group(0); // 完整的 {{xxx}} 或 {{meta.xxx}}

            // 检查是否是 meta.xxx 格式
            if (varName.startsWith("meta.")) {
                // 从会话 metadata 中获取
                String metaKey = varName.substring(5); // 去掉 "meta." 前缀
                String metaValue = getSessionMetadataValue(sessionId, metaKey);
                if (metaValue != null) {
                    result = result.replace(fullMatch, metaValue);
                }
            } else if ("sessionId".equalsIgnoreCase(varName)) {
                // 支持 {{sessionId}} 变量
                if (sessionId != null) {
                    result = result.replace(fullMatch, sessionId.toString());
                }
            } else {
                // 从 params 中获取
                if (params != null) {
                    Object value = params.get(varName);
                    if (value != null) {
                        result = result.replace(fullMatch, value.toString());
                    }else {
                        result = result.replace(fullMatch, "");
                    }
                }else  {
                    result = result.replace(fullMatch, "");
                }
            }
        }

        return result;
    }

    /**
     * 从会话 metadata 中获取指定 key 的值
     */
    private String getSessionMetadataValue(UUID sessionId, String key) {
        if (sessionId == null || key == null || key.isEmpty()) {
            return null;
        }

        try {
            com.example.aikef.model.ChatSession session = chatSessionService.getSession(sessionId);
            String metadataJson = session.getMetadata();

            if (metadataJson == null || metadataJson.isEmpty()) {
                return null;
            }

            // 解析 JSON
            JsonNode metadata = objectMapper.readTree(metadataJson);
            JsonNode valueNode = metadata.get(key);

            if (valueNode != null && !valueNode.isNull()) {
                if (valueNode.isTextual()) {
                    return valueNode.asText();
                } else {
                    // 非文本类型，转换为 JSON 字符串
                    return valueNode.toString();
                }
            }

        } catch (Exception e) {
            log.warn("获取会话元数据失败: sessionId={}, key={}, error={}", sessionId, key, e.getMessage());
        }

        return null;
    }

    /**
     * 根据 JSONPath 提取数据
     */
    private String extractByJsonPath(String json, String path) {
        try {
            JsonNode root = objectMapper.readTree(json);
            // 简单实现：支持 $.field.subfield 格式
            String[] parts = path.replace("$.", "").split("\\.");
            JsonNode current = root;
            for (String part : parts) {
                if (current == null) break;
                current = current.get(part);
            }
            return current != null ? current.toString() : json;
        } catch (Exception e) {
            return json;
        }
    }

    // ==================== 执行记录查询 ====================

    /**
     * 获取工具执行记录
     */
    public Page<ToolExecution> getExecutions(UUID toolId, Pageable pageable) {
        return executionRepository.findByTool_Id(toolId, pageable);
    }

    /**
     * 获取会话的执行记录
     */
    public List<ToolExecution> getExecutionsBySession(UUID sessionId) {
        return executionRepository.findBySessionId(sessionId);
    }

    /**
     * 获取工具统计
     */
    public ToolStats getToolStats(UUID toolId) {
        long successCount = executionRepository.countByToolIdAndStatus(toolId, ToolExecution.ExecutionStatus.SUCCESS);
        long failedCount = executionRepository.countByToolIdAndStatus(toolId, ToolExecution.ExecutionStatus.FAILED);
        Double avgDuration = executionRepository.getAverageDurationByToolId(toolId);

        return new ToolStats(successCount, failedCount, avgDuration != null ? avgDuration : 0.0);
    }

    // ==================== 工具定义生成（用于 LLM） ====================

    /**
     * 生成工具定义（用于 LLM function calling）
     */
    public List<ToolDefinition> generateToolDefinitions() {
        List<AiTool> tools = getToolsWithSchema();
        List<ToolDefinition> definitions = new ArrayList<>();

        for (AiTool tool : tools) {
            try {
                List<FieldDefinition> params = Collections.emptyList();
                if (tool.getSchema() != null && tool.getSchema().getFieldsJson() != null) {
                    params = parseFieldsJson(tool.getSchema().getFieldsJson());
                }

                definitions.add(new ToolDefinition(
                        tool.getName(),
                        tool.getDescription(),
                        params,
                        tool.getInputExample(),
                        tool.getOutputExample()
                ));
            } catch (Exception e) {
                log.warn("生成工具定义失败: tool={}", tool.getName(), e);
            }
        }

        return definitions;
    }

    /**
     * 为工具创建 ExtractionSchema
     */
    @Transactional
    public ExtractionSchema createSchemaForTool(String toolName, List<ParameterDefinition> parameters) {
        ExtractionSchema schema = new ExtractionSchema();
        schema.setName("tool_" + toolName + "_params");
        schema.setDescription("工具 [" + toolName + "] 的参数定义");

        // 转换为 FieldDefinition 格式（支持嵌套结构）
        List<FieldDefinition> fields = parameters.stream()
                .map(this::convertParameterDefinitionToFieldDefinition)
                .toList();

        try {
            schema.setFieldsJson(objectMapper.writeValueAsString(fields));
        } catch (JsonProcessingException e) {
            log.error("序列化参数定义失败", e);
            schema.setFieldsJson("[]");
        }

        return schemaRepository.save(schema);
    }

    /**
     * 更新已有的 ExtractionSchema（避免唯一键冲突）
     */
    @Transactional
    public void updateSchemaForTool(ExtractionSchema schema, List<ParameterDefinition> parameters) {
        // 转换为 FieldDefinition 格式（支持嵌套结构）
        List<FieldDefinition> fields = parameters.stream()
                .map(this::convertParameterDefinitionToFieldDefinition)
                .toList();

        try {
            schema.setFieldsJson(objectMapper.writeValueAsString(fields));
        } catch (JsonProcessingException e) {
            log.error("序列化参数定义失败", e);
            schema.setFieldsJson("[]");
        }

        schemaRepository.save(schema);
    }

    /**
     * 递归转换 ParameterDefinition 到 FieldDefinition（支持嵌套结构）
     */
    private FieldDefinition convertParameterDefinitionToFieldDefinition(ParameterDefinition param) {
        FieldDefinition field = new FieldDefinition();
        field.setName(param.name());
        field.setDisplayName(param.displayName());
        field.setType(param.type());
        field.setRequired(param.required());
        field.setDescription(param.description());
        field.setEnumValues(param.enumValues());
        field.setDefaultValue(param.defaultValue());

        // 递归转换 properties（如果是 OBJECT 类型）
        if (param.properties() != null && !param.properties().isEmpty()) {
            List<FieldDefinition> properties = param.properties().stream()
                    .map(this::convertParameterDefinitionToFieldDefinition)
                    .toList();
            field.setProperties(properties);
        }

        // 递归转换 items（如果是 ARRAY 类型）
        if (param.items() != null) {
            FieldDefinition items = convertParameterDefinitionToFieldDefinition(param.items());
            field.setItems(items);
        }

        return field;
    }

    /**
     * 解析字段 JSON
     */
    private List<FieldDefinition> parseFieldsJson(String fieldsJson) {
        if (fieldsJson == null || fieldsJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(fieldsJson, new TypeReference<List<FieldDefinition>>() {
            });
        } catch (JsonProcessingException e) {
            log.warn("解析字段定义失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 生成 OpenAI Function Calling 格式
     */
    public String generateOpenAiFunctions() {
        List<ToolDefinition> definitions = generateToolDefinitions();
        List<Map<String, Object>> functions = new ArrayList<>();

        for (ToolDefinition def : definitions) {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", def.name());
            function.put("description", def.description());

            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("type", "object");

            Map<String, Object> properties = new LinkedHashMap<>();
            List<String> required = new ArrayList<>();

            for (FieldDefinition field : def.parameters()) {
                Map<String, Object> prop = new LinkedHashMap<>();
                prop.put("type", mapFieldTypeToJson(field.getType()));
                if (field.getDescription() != null) {
                    prop.put("description", field.getDescription());
                }
                if (field.getEnumValues() != null && !field.getEnumValues().isEmpty()) {
                    prop.put("enum", field.getEnumValues());
                }
                properties.put(field.getName(), prop);

                if (field.isRequired()) {
                    required.add(field.getName());
                }
            }

            parameters.put("properties", properties);
            parameters.put("required", required);
            function.put("parameters", parameters);

            functions.add(function);
        }

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(functions);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String mapFieldTypeToJson(FieldDefinition.FieldType type) {
        return switch (type) {
            case STRING, DATE, DATETIME, EMAIL, PHONE -> "string";
            case NUMBER -> "number";
            case INTEGER -> "integer";
            case BOOLEAN -> "boolean";
            case ARRAY -> "array";
            case OBJECT, ENUM -> "string";
        };
    }

    private String serializeParams(Map<String, Object> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    // ==================== DTOs ====================

    /**
     * 参数定义（用于创建/更新工具）
     */
    public record ParameterDefinition(
            String name,
            String displayName,
            FieldDefinition.FieldType type,
            boolean required,
            String description,
            List<String> enumValues,
            String defaultValue,
            List<ParameterDefinition> properties,  // 嵌套属性（当type为OBJECT时）
            ParameterDefinition items  // 数组元素定义（当type为ARRAY时）
    ) {
    }

    public record CreateToolRequest(
            String name,
            String displayName,
            String description,
            AiTool.ToolType toolType,
            List<ParameterDefinition> parameters,  // 内嵌参数定义
            // API 配置
            String apiMethod,
            String apiUrl,
            String apiHeaders,
            String apiBodyTemplate,
            String apiResponsePath,
            Integer apiTimeout,
            // MCP 配置
            String mcpEndpoint,
            String mcpToolName,
            String mcpServerType,
            String mcpConfig,
            // 认证配置
            AiTool.AuthType authType,
            String authConfig,
            // 其他
            String inputExample,
            String outputExample,
            String resultDescription,   // 返回结果描述
            String resultMetadata,      // 返回字段元数据 (JSON)
            Integer retryCount,
            Boolean requireConfirmation,
            Integer sortOrder,
            String tags
    ) {
    }

    public record UpdateToolRequest(
            String displayName,
            String description,
            List<ParameterDefinition> parameters,  // 内嵌参数定义
            String apiMethod,
            String apiUrl,
            String apiHeaders,
            String apiBodyTemplate,
            String apiResponsePath,
            Integer apiTimeout,
            String mcpEndpoint,
            String mcpToolName,
            String mcpServerType,
            String mcpConfig,
            AiTool.AuthType authType,
            String authConfig,
            String inputExample,
            String outputExample,
            String resultDescription,   // 返回结果描述
            String resultMetadata,      // 返回字段元数据 (JSON)
            Integer retryCount,
            Boolean requireConfirmation,
            Boolean enabled,
            Integer sortOrder,
            String tags
    ) {
    }

    public record ToolExecutionResult(
            boolean success,
            String output,
            String errorMessage,
            Integer httpStatus,
            Long durationMs,
            UUID executionId
    ) {
    }

    public record ToolStats(
            long successCount,
            long failedCount,
            double avgDurationMs
    ) {
    }

    public record ToolDefinition(
            String name,
            String description,
            List<FieldDefinition> parameters,
            String inputExample,
            String outputExample
    ) {
    }
}

