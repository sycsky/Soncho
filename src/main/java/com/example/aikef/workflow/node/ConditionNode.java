package com.example.aikef.workflow.node;

import com.example.aikef.workflow.context.WorkflowContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;

import java.util.regex.Pattern;

/**
 * 条件判断节点
 * 根据配置的条件进行分支判断
 * 使用 NodeBooleanComponent 实现 IF 条件判断
 */
@LiteflowComponent("condition")
public class ConditionNode extends NodeBooleanComponent {

    @Override
    public boolean processBoolean() throws Exception {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = this.getContextBean(WorkflowContext.class);
        String actualNodeId = BaseWorkflowNode.resolveActualNodeId(this.getTag(), this.getNodeId(), ctx);
        JsonNode config = ctx.getNodeConfig(actualNodeId);
        
        boolean result = false;
        String conditionType = BaseWorkflowNode.readConfigString(config, "conditionType", "contains");
        String targetValue = BaseWorkflowNode.readConfigString(config, "value", "");
        String sourceType = BaseWorkflowNode.readConfigString(config, "sourceType", "lastOutput"); // lastOutput, userMessage, intent, variable
        
        // 获取要判断的值
        String sourceValue = getSourceValue(ctx, sourceType, config);
        
        // 执行判断
        result = switch (conditionType) {
            case "contains" -> sourceValue != null && sourceValue.contains(targetValue);
            case "notContains" -> sourceValue == null || !sourceValue.contains(targetValue);
            case "equals" -> targetValue.equals(sourceValue);
            case "notEquals" -> !targetValue.equals(sourceValue);
            case "startsWith" -> sourceValue != null && sourceValue.startsWith(targetValue);
            case "endsWith" -> sourceValue != null && sourceValue.endsWith(targetValue);
            case "regex" -> sourceValue != null && Pattern.matches(targetValue, sourceValue);
            case "isEmpty" -> sourceValue == null || sourceValue.trim().isEmpty();
            case "isNotEmpty" -> sourceValue != null && !sourceValue.trim().isEmpty();
            case "intentEquals" -> targetValue.equals(ctx.getIntent());
            case "confidenceGreaterThan" -> {
                Double threshold;
                try {
                    threshold = Double.parseDouble(targetValue);
                } catch (Exception e) {
                    threshold = null;
                }
                yield threshold != null && ctx.getIntentConfidence() != null && ctx.getIntentConfidence() > threshold;
            }
            default -> false;
        };
        
        // 记录执行详情
        BaseWorkflowNode.recordExecution(ctx, actualNodeId, this.getNodeId(), this.getName(), sourceValue, result, startTime, true, null);
        
        return result;
    }

    private String getSourceValue(WorkflowContext ctx, String sourceType, JsonNode config) {
        return switch (sourceType) {
            case "lastOutput" -> ctx.getLastOutput();
            case "query", "userMessage" -> ctx.getQuery();
            case "intent" -> ctx.getIntent();
            case "variable" -> {
                String variableName = BaseWorkflowNode.readConfigString(config, "variableName", "");
                Object value = ctx.getVariable(variableName);
                yield value != null ? value.toString() : null;
            }
            case "entity" -> {
                String entityName = BaseWorkflowNode.readConfigString(config, "entityName", "");
                Object value = ctx.getEntities().get(entityName);
                yield value != null ? value.toString() : null;
            }
            default -> ctx.getLastOutput();
        };
    }
}

