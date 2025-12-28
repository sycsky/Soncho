package com.example.aikef.workflow.util;

import com.example.aikef.workflow.context.WorkflowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工作流模板引擎
 * 支持解析模板中的变量表达式
 * 
 * 变量格式: {{namespace.key}}
 * 
 * 支持的命名空间:
 * - sys: 系统变量 (query, lastOutput, intent, sessionId, customerId, now)
 * - var: 自定义变量
 * - node: 节点输出
 * - customer: 客户信息
 * - entity: 提取的实体
 * - agent: Agent 会话变量 (sysPrompt)
 * - event: 事件数据 (eventData，来自 webhook)
 * 
 * 示例:
 * - {{sys.query}} - 用户输入查询
 * - {{sys.lastOutput}} - 上一个节点的输出
 * - {{sys.intent}} - 识别的意图
 * - {{sys.now}} - 当前日期 (格式: yyyy-MM-dd)
 * - {{var.orderId}} - 自定义变量 orderId
 * - {{node.llm_1}} - 节点 llm_1 的输出
 * - {{customer.name}} - 客户姓名
 * - {{entity.productName}} - 提取的实体 productName
 * - {{agent.sysPrompt}} - Agent 会话的系统提示词
 * - {{event.orderId}} - 事件数据中的 orderId 字段
 * - {{event.userName}} - 事件数据中的 userName 字段
 */
public class TemplateEngine {

    private static final Logger log = LoggerFactory.getLogger(TemplateEngine.class);

    /**
     * 变量模式: {{namespace.key}} 或 {{key}}
     */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    /**
     * 解析模板，替换变量
     *
     * @param template 模板字符串
     * @param ctx      工作流上下文
     * @return 解析后的字符串
     */
    public static String render(String template, WorkflowContext ctx) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String expression = matcher.group(1).trim();
            String replacement = resolveExpression(expression, ctx);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 解析变量表达式
     */
    private static String resolveExpression(String expression, WorkflowContext ctx) {
        try {
            // 解析 namespace.key 格式
            int dotIndex = expression.indexOf('.');
            if (dotIndex > 0) {
                String namespace = expression.substring(0, dotIndex);
                String key = expression.substring(dotIndex + 1);
                return resolveNamespacedVariable(namespace, key, ctx);
            }

            // 没有命名空间，尝试按优先级查找
            return resolveSimpleVariable(expression, ctx);

        } catch (Exception e) {
            log.warn("解析变量表达式失败: {}", expression, e);
            return "{{" + expression + "}}"; // 保留原始表达式
        }
    }

    /**
     * 解析带命名空间的变量
     */
    private static String resolveNamespacedVariable(String namespace, String key, WorkflowContext ctx) {
        return switch (namespace.toLowerCase()) {
            case "sys", "system" -> resolveSystemVariable(key, ctx);
            case "var", "variable" -> resolveCustomVariable(key, ctx);
            case "node" -> resolveNodeOutput(key, ctx);
            case "customer" -> resolveCustomerInfo(key, ctx);
            case "entity" -> resolveEntity(key, ctx);
            case "agent" -> resolveAgentVariable(key, ctx);
            case "event" -> resolveEventData(key, ctx);
            default -> {
                log.warn("未知的命名空间: {}", namespace);
                yield "";
            }
        };
    }

    /**
     * 解析系统变量
     */
    private static String resolveSystemVariable(String key, WorkflowContext ctx) {
        return switch (key.toLowerCase()) {
            case "query", "input", "usermessage", "user_message" -> 
                    nullToEmpty(ctx.getQuery());
            case "lastoutput", "last_output", "previousoutput", "previous_output" -> 
                    nullToEmpty(ctx.getLastOutput());
            case "intent" -> 
                    nullToEmpty(ctx.getIntent());
            case "intentconfidence", "intent_confidence" -> 
                    ctx.getIntentConfidence() != null ? String.format("%.2f", ctx.getIntentConfidence()) : "";
            case "sessionid", "session_id" -> 
                    ctx.getSessionId() != null ? ctx.getSessionId().toString() : "";
            case "customerid", "customer_id" -> 
                    ctx.getCustomerId() != null ? ctx.getCustomerId().toString() : "";
            case "finalreply", "final_reply" -> 
                    nullToEmpty(ctx.getFinalReply());
            case "needhumantransfer", "need_human_transfer" -> 
                    String.valueOf(ctx.isNeedHumanTransfer());
            case "humantransferreason", "human_transfer_reason" -> 
                    nullToEmpty(ctx.getHumanTransferReason());
            case "now", "date", "currentdate", "current_date" -> 
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            default -> {
                log.warn("未知的系统变量: {}", key);
                yield "";
            }
        };
    }

    /**
     * 解析自定义变量
     */
    private static String resolveCustomVariable(String key, WorkflowContext ctx) {
        Object value = ctx.getVariable(key);
        return value != null ? value.toString() : "";
    }

    /**
     * 解析节点输出
     */
    private static String resolveNodeOutput(String nodeId, WorkflowContext ctx) {
        Object output = ctx.getOutput(nodeId);
        return output != null ? output.toString() : "";
    }

    /**
     * 解析客户信息
     */
    private static String resolveCustomerInfo(String key, WorkflowContext ctx) {
        Object value = ctx.getCustomerInfo().get(key);
        return value != null ? value.toString() : "";
    }

    /**
     * 解析实体
     */
    private static String resolveEntity(String key, WorkflowContext ctx) {
        Object value = ctx.getEntities().get(key);
        return value != null ? value.toString() : "";
    }

    /**
     * 解析 Agent 变量
     */
    private static String resolveAgentVariable(String key, WorkflowContext ctx) {
        com.example.aikef.model.AgentSession agentSession = ctx.getAgentSession();
        if (agentSession == null) {
            return "";
        }
        
        return switch (key.toLowerCase()) {
            case "sysprompt", "sys_prompt" -> nullToEmpty(agentSession.getSysPrompt());
            default -> {
                log.warn("未知的 Agent 变量: {}", key);
                yield "";
            }
        };
    }

    /**
     * 解析事件数据 (eventData)
     * 从工作流变量的 eventData 中获取指定字段
     * 
     * 示例: {{event.orderId}} 会从 eventData Map 中获取 "orderId" 字段
     */
    private static String resolveEventData(String key, WorkflowContext ctx) {
        // 从 variables 中获取 eventData
        Object eventDataObj = ctx.getVariable("eventData");
        if (eventDataObj == null) {
            log.debug("eventData 不存在于工作流变量中");
            return "";
        }

        // eventData 应该是一个 Map<String, Object>
        if (eventDataObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = (Map<String, Object>) eventDataObj;
            Object value = eventData.get(key);
            if (value != null) {
                return value.toString();
            }
            log.debug("eventData 中不存在字段: {}", key);
            return "";
        }

        log.warn("eventData 不是 Map 类型: {}", eventDataObj.getClass().getName());
        return "";
    }

    /**
     * 解析简单变量（无命名空间）
     * 按优先级查找: 系统变量 > 自定义变量 > 实体 > 客户信息
     */
    private static String resolveSimpleVariable(String key, WorkflowContext ctx) {
        // 1. 尝试系统变量
        String sysValue = tryResolveSystemVariable(key, ctx);
        if (sysValue != null) {
            return sysValue;
        }

        // 2. 尝试自定义变量
        Object varValue = ctx.getVariable(key);
        if (varValue != null) {
            return varValue.toString();
        }

        // 3. 尝试实体
        Object entityValue = ctx.getEntities().get(key);
        if (entityValue != null) {
            return entityValue.toString();
        }

        // 4. 尝试客户信息
        Object customerValue = ctx.getCustomerInfo().get(key);
        if (customerValue != null) {
            return customerValue.toString();
        }

        // 5. 尝试节点输出
        Object nodeOutput = ctx.getOutput(key);
        if (nodeOutput != null) {
            return nodeOutput.toString();
        }

        log.debug("变量未找到: {}", key);
        return "";
    }

    /**
     * 尝试解析系统变量（不抛异常）
     */
    private static String tryResolveSystemVariable(String key, WorkflowContext ctx) {
        return switch (key.toLowerCase()) {
            case "query", "input", "usermessage", "user_message" -> ctx.getQuery();
            case "lastoutput", "last_output" -> ctx.getLastOutput();
            case "intent" -> ctx.getIntent();
            default -> null;
        };
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    /**
     * 检查模板是否包含变量
     */
    public static boolean hasVariables(String template) {
        if (template == null || template.isEmpty()) {
            return false;
        }
        return VARIABLE_PATTERN.matcher(template).find();
    }

    /**
     * 提取模板中的所有变量表达式
     */
    public static java.util.List<String> extractVariables(String template) {
        java.util.List<String> variables = new java.util.ArrayList<>();
        if (template == null || template.isEmpty()) {
            return variables;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            variables.add(matcher.group(1).trim());
        }
        return variables;
    }
}

