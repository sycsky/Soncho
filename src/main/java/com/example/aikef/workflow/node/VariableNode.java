package com.example.aikef.workflow.node;

import com.example.aikef.workflow.context.WorkflowContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.annotation.LiteflowComponent;

import java.util.Iterator;
import java.util.Map;

/**
 * 变量操作节点
 * 设置、修改变量值
 */
@LiteflowComponent("variable")
public class VariableNode extends BaseWorkflowNode {

    @Override
    public void process() {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = getWorkflowContext();
        
        try {
            String operation = getConfigString("operation", "set"); // set, append, delete
            JsonNode variablesConfig = getNodeConfig().get("variables");
            
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

