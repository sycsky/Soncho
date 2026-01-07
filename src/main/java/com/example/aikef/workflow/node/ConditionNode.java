package com.example.aikef.workflow.node;

import com.example.aikef.workflow.context.WorkflowContext;
import com.example.aikef.workflow.util.TemplateEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeSwitchComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 条件判断节点 (Switch 类型)
 * 支持 IF / ELSE IF / ELSE 多分支判断
 */
@LiteflowComponent("condition")
public class ConditionNode extends NodeSwitchComponent {

    private static final Logger log = LoggerFactory.getLogger(ConditionNode.class);

    @Override
    public String processSwitch() throws Exception {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = this.getContextBean(WorkflowContext.class);
        String actualNodeId = BaseWorkflowNode.resolveActualNodeId(this.getTag(), this.getNodeId(), ctx);
        JsonNode config = ctx.getNodeConfig(actualNodeId);

        String matchedHandleId = "else"; // 默认走 ELSE 分支
        String matchedConditionDetail = "else";
        String lastSourceValue = "";

        ctx.setOutput(this.getTag(),ctx.getLastOutput());
        // 获取条件列表
        JsonNode conditions = config.get("conditions");
        if (conditions != null && conditions.isArray()) {
            for (JsonNode condition : conditions) {
                String id = BaseWorkflowNode.readConfigString(condition, "id", "");
                String sourceValueTpl = BaseWorkflowNode.readConfigString(condition, "sourceValue", "");
                String conditionType = BaseWorkflowNode.readConfigString(condition, "conditionType", "");
                String inputValueTpl = BaseWorkflowNode.readConfigString(condition, "inputValue", "");

                // 解析变量
                String sourceValue = TemplateEngine.render(sourceValueTpl, ctx);
                String inputValue = TemplateEngine.render(inputValueTpl, ctx);
                lastSourceValue = sourceValue;

                if (checkCondition(sourceValue, conditionType, inputValue)) {
                    matchedHandleId = id;
                    matchedConditionDetail = String.format("%s %s %s", sourceValue, conditionType, inputValue);
                    break; // 找到第一个满足的条件即停止
                }
            }
        }

        // 路由逻辑
        String routesKey = "__condition_routes_" + actualNodeId; 
        @SuppressWarnings("unchecked")
        Map<String, String> routeKeyToNode = ctx.getVariable(routesKey);
        
        // 兼容旧的 ConditionNode (Boolean) 逻辑或处理路由表不存在的情况
        // 如果是新的 Switch 模式，必须依赖 routeKeyToNode
        
        String targetNodeId = null;
        if (routeKeyToNode != null) {
            targetNodeId = routeKeyToNode.get(matchedHandleId);
            
            // 如果匹配到了条件但没有连接线，或者走 else 但没有连接线
            if (targetNodeId == null && !"else".equals(matchedHandleId)) {
                // 尝试 fallback 到 else
                targetNodeId = routeKeyToNode.get("else");
            }
        }

        // 如果没有找到目标节点，可能需要返回 default 或 null
        // LiteFlow Switch 如果返回 null 会报错，通常返回 "default" 或空字符串
        if (targetNodeId == null) {
            targetNodeId = "default"; 
            // 或者如果这是一个终端节点，可能需要特殊处理，但 Switch 节点通常后面接其他节点
        }

        log.info("条件判断节点执行: nodeId={}, matched={}, target={}", actualNodeId, matchedHandleId, targetNodeId);

        // 记录执行详情
        BaseWorkflowNode.recordExecution(ctx, actualNodeId, this.getNodeId(), this.getName(),
                lastSourceValue, matchedConditionDetail, startTime, true, null);

        return "tag:" + targetNodeId;
    }

    private boolean checkCondition(String source, String type, String target) {
        if (source == null) source = "";
        if (target == null) target = "";

        try {
            switch (type) {
                case "contains": return source.contains(target);
                case "notContains": return !source.contains(target);
                case "startsWith": return source.startsWith(target);
                case "endsWith": return source.endsWith(target);
                case "equals": return source.equals(target);
                case "notEquals": return !source.equals(target);
                case "isEmpty": return source.isEmpty();
                case "isNotEmpty": return !source.isEmpty();
                case "gt": return Double.parseDouble(source) > Double.parseDouble(target);
                case "lt": return Double.parseDouble(source) < Double.parseDouble(target);
                case "gte": return Double.parseDouble(source) >= Double.parseDouble(target);
                case "lte": return Double.parseDouble(source) <= Double.parseDouble(target);
                default: return false;
            }
        } catch (NumberFormatException e) {
            // 数值比较异常时返回 false
            return false;
        }
    }
}

