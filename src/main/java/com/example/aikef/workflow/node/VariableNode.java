package com.example.aikef.workflow.node;

import com.example.aikef.workflow.context.WorkflowContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import jakarta.annotation.Resource;

import java.util.Iterator;
import java.util.Map;

/**
 * 变量操作节点
 * 设置、修改变量值
 * 
 * 配置方式1（从上一个节点输出提取字段）:
 * {
 *   "variableName": "orderId",        // 变量名
 *   "sourceField": "order.id",         // 必需：从上一个节点输出的JSON中提取的字段路径（如 "order.id"），或使用 "#lastResponse" 获取整个输出
 *   "sourceNodeId": "api_1"            // 可选：指定来源节点ID，为空则使用上一个节点（lastOutput）
 * }
 * 
 * 配置方式2（批量设置变量）:
 * {
 *   "operation": "set",                // set, append, delete
 *   "variables": {
 *     "var1": "value1",
 *     "var2": "{{sys.lastOutput}}"
 *   }
 * }
 */
@LiteflowComponent("variable")
public class VariableNode extends BaseWorkflowNode {

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void process() {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = getWorkflowContext();
        
        try {
            JsonNode config = getNodeConfig();
            
            // 方式1：从上一个节点输出提取字段并设置到变量
            String variableName = getConfigString("variableName");
            if (variableName != null && !variableName.isEmpty()) {
                String sourceField = getConfigString("sourceField", "");
                String sourceNodeId = getConfigString("sourceNodeId", "");
                
                // 获取来源输出
                String sourceOutput = null;
                if (sourceNodeId != null && !sourceNodeId.isEmpty()) {
                    // 从指定节点获取输出
                    Object output = ctx.getOutput(sourceNodeId);
                    sourceOutput = output != null ? output.toString() : "";
                } else {
                    // 使用上一个节点的输出
                    sourceOutput = ctx.getLastOutput();
                }
                
                if (sourceOutput == null || sourceOutput.isEmpty()) {
                    log.warn("来源输出为空，无法设置变量: variableName={}", variableName);
                    setOutput("no_source_output");
                    recordExecution(null, "no_source_output", startTime, false, "来源输出为空");
                    return;
                }
                
                // 提取字段值
                Object value;
                if (sourceField != null && !sourceField.isEmpty()) {
                    if ("#lastResponse".equals(sourceField)) {
                        // 特殊标记：使用整个输出
                        value = sourceOutput;
                    } else {
                        // 从 JSON 中提取指定字段
                        value = extractFieldFromJson(sourceOutput, sourceField);
                    }
                } else {
                    // sourceField 为空时，不设置变量（保持原有行为或报错）
                    log.warn("sourceField 为空，需要指定字段路径或使用 #lastResponse 获取整个输出: variableName={}", variableName);
                    setOutput("error: sourceField_required");
                    recordExecution(null, "error: sourceField_required", startTime, false, "sourceField 为空");
                    return;
                }
                
                // 设置变量
                ctx.setVariable(variableName, value);
                log.info("从上一个节点输出设置变量: variableName={}, sourceField={}, value={}", 
                        variableName, sourceField, value);
                setOutput("variable_set: " + variableName);
                recordExecution(sourceOutput, "variable_set: " + variableName, startTime, true, null);
                return;
            }
            
            // 方式2：批量设置变量（原有功能）
            String operation = getConfigString("operation", "set"); // set, append, delete
            JsonNode variablesConfig = config.get("variables");
            
            if (variablesConfig != null) {
                Iterator<Map.Entry<String, JsonNode>> fields = variablesConfig.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String varName = field.getKey();
                    String varValue = resolveValue(field.getValue(), ctx);
                    
                    switch (operation) {
                        case "set" -> ctx.setVariable(varName, varValue);
                        case "append" -> {
                            Object existing = ctx.getVariable(varName);
                            String newValue = (existing != null ? existing.toString() : "") + varValue;
                            ctx.setVariable(varName, newValue);
                        }
                        case "delete" -> ctx.getVariables().remove(varName);
                    }
                }
            }
            
            log.info("变量操作完成: operation={}", operation);
            setOutput("variables_updated");
            recordExecution(variablesConfig, "variables_updated", startTime, true, null);
            
        } catch (Exception e) {
            log.error("变量操作失败", e);
            setOutput("error");
            recordExecution(null, "error", startTime, false, e.getMessage());
        }
    }

    /**
     * 从 JSON 字符串中提取字段值
     * 支持路径格式: "field" 或 "field.subfield" 或 "field[0].subfield"
     * 
     * @param jsonStr JSON 字符串
     * @param fieldPath 字段路径，如 "order.id" 或 "items[0].name"
     * @return 字段值，如果不存在则返回整个 JSON 字符串
     */
    private Object extractFieldFromJson(String jsonStr, String fieldPath) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonStr);
            if (rootNode == null) {
                log.warn("无法解析 JSON: {}", jsonStr);
                return jsonStr;
            }
            
            // 解析路径
            String[] parts = fieldPath.split("\\.");
            JsonNode currentNode = rootNode;
            
            for (String part : parts) {
                if (currentNode == null) {
                    break;
                }
                
                // 处理数组索引，如 "items[0]"
                if (part.contains("[") && part.endsWith("]")) {
                    int bracketIndex = part.indexOf('[');
                    String arrayName = part.substring(0, bracketIndex);
                    String indexStr = part.substring(bracketIndex + 1, part.length() - 1);
                    
                    currentNode = currentNode.get(arrayName);
                    if (currentNode != null && currentNode.isArray()) {
                        try {
                            int index = Integer.parseInt(indexStr);
                            currentNode = currentNode.get(index);
                        } catch (NumberFormatException e) {
                            log.warn("无效的数组索引: {}", indexStr);
                            currentNode = null;
                        }
                    } else {
                        currentNode = null;
                    }
                } else {
                    currentNode = currentNode.get(part);
                }
            }
            
            if (currentNode != null) {
                // 根据节点类型返回适当的值
                if (currentNode.isTextual()) {
                    return currentNode.asText();
                } else if (currentNode.isNumber()) {
                    if (currentNode.isInt()) {
                        return currentNode.asInt();
                    } else if (currentNode.isLong()) {
                        return currentNode.asLong();
                    } else {
                        return currentNode.asDouble();
                    }
                } else if (currentNode.isBoolean()) {
                    return currentNode.asBoolean();
                } else {
                    return currentNode.toString();
                }
            } else {
                log.warn("字段路径不存在: fieldPath={}, json={}", fieldPath, jsonStr);
                return jsonStr; // 如果字段不存在，返回整个 JSON
            }
        } catch (Exception e) {
            log.warn("从 JSON 提取字段失败: fieldPath={}, error={}", fieldPath, e.getMessage());
            return jsonStr; // 解析失败，返回整个 JSON
        }
    }

    private String resolveValue(JsonNode valueNode, WorkflowContext ctx) {
        if (valueNode.isTextual()) {
            String value = valueNode.asText();
            // 检查是否是变量引用
            if (value.startsWith("{{") && value.endsWith("}}")) {
                String varName = value.substring(2, value.length() - 2);
                return switch (varName) {
                    case "query", "userMessage" -> ctx.getQuery();
                    case "lastOutput" -> ctx.getLastOutput();
                    case "intent" -> ctx.getIntent() != null ? ctx.getIntent() : "";
                    default -> {
                        Object v = ctx.getVariable(varName);
                        yield v != null ? v.toString() : "";
                    }
                };
            }
            return value;
        }
        return valueNode.toString();
    }
}

