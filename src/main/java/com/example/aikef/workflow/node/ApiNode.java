package com.example.aikef.workflow.node;

import com.example.aikef.workflow.context.WorkflowContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API 调用节点
 * 调用外部 API 并处理响应
 */
@LiteflowComponent("api")
public class ApiNode extends BaseWorkflowNode {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    @Override
    public void process() {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = getWorkflowContext();
        
        try {
            String url = getConfigString("url", "");
            String method = getConfigString("method", "GET");
            JsonNode headersConfig = getNodeConfig().get("headers");
            JsonNode bodyConfig = getNodeConfig().get("body");
            String responseMapping = getConfigString("responseMapping", ""); // JSONPath 表达式
            
            // 替换 URL 中的变量
            url = replaceVariables(url, ctx);
            
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (headersConfig != null) {
                Iterator<Map.Entry<String, JsonNode>> fields = headersConfig.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    headers.add(field.getKey(), replaceVariables(field.getValue().asText(), ctx));
                }
            }
            
            // 构建请求体
            Object requestBody = null;
            if (bodyConfig != null && !bodyConfig.isNull()) {
                String bodyStr = replaceVariables(bodyConfig.toString(), ctx);
                requestBody = objectMapper.readValue(bodyStr, Map.class);
            }
            
            // 发送请求
            HttpEntity<?> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, 
                    HttpMethod.valueOf(method.toUpperCase()), 
                    entity, 
                    String.class
            );
            
            String responseBody = response.getBody();
            
            // 处理响应映射
            String output = responseBody;
            if (responseMapping != null && !responseMapping.isEmpty()) {
                // 简单的 JSON 路径提取
                output = extractJsonPath(responseBody, responseMapping);
            }
            
            // 是否保存到变量
            String saveToVariable = getConfigString("saveToVariable", "");
            if (!saveToVariable.isEmpty()) {
                ctx.setVariable(saveToVariable, output);
            }
            
            log.info("API 调用成功: url={}, response={}", url, output);
            setOutput(output);
            recordExecution(url, output, startTime, true, null);
            
        } catch (Exception e) {
            log.error("API 调用失败", e);
            String errorOutput = "API 调用失败: " + e.getMessage();
            setOutput(errorOutput);
            recordExecution(null, errorOutput, startTime, false, e.getMessage());
        }
    }

    private String replaceVariables(String template, WorkflowContext ctx) {
        if (template == null) return null;
        
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String value = getVariableValue(variableName, ctx);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    private String getVariableValue(String variableName, WorkflowContext ctx) {
        return switch (variableName) {
            case "query", "userMessage" -> ctx.getQuery();
            case "lastOutput" -> ctx.getLastOutput();
            case "sessionId" -> ctx.getSessionId() != null ? ctx.getSessionId().toString() : "";
            case "customerId" -> ctx.getCustomerId() != null ? ctx.getCustomerId().toString() : "";
            default -> {
                Object value = ctx.getVariable(variableName);
                yield value != null ? value.toString() : "";
            }
        };
    }

    /**
     * 简单的 JSON 路径提取
     * 支持格式: $.field.subfield
     */
    private String extractJsonPath(String json, String path) {
        try {
            JsonNode node = objectMapper.readTree(json);
            String[] parts = path.replace("$.", "").split("\\.");
            
            for (String part : parts) {
                if (node == null) break;
                node = node.get(part);
            }
            
            if (node != null) {
                return node.isTextual() ? node.asText() : node.toString();
            }
        } catch (Exception e) {
            log.warn("JSON 路径提取失败: path={}, error={}", path, e.getMessage());
        }
        return json;
    }
}

