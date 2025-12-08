package com.example.aikef.workflow.node;

import com.example.aikef.workflow.context.WorkflowContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;

/**
 * 回复节点
 * 设置最终回复内容
 * 
 * 支持的模板变量:
 * - {{sys.query}} - 用户输入查询
 * - {{sys.lastOutput}} - 上一个节点的输出
 * - {{sys.intent}} - 识别的意图
 * - {{var.variableName}} - 自定义变量
 * - {{node.nodeId}} - 指定节点的输出
 * - {{customer.name}} - 客户信息
 * - {{entity.entityName}} - 提取的实体
 */
@LiteflowComponent("reply")
public class ReplyNode extends BaseWorkflowNode {

    @Override
    public void process() {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = getWorkflowContext();
        
        try {
            String replyType = getConfigString("replyType", "template");
            String reply;
            
            switch (replyType) {
                case "template" -> {
                    // 使用模板回复，支持变量替换
                    String template = getConfigString("template", "");
                    // 也支持 text 字段（前端可能用 text）
                    if (template == null || template.isEmpty()) {
                        template = getConfigString("text", "");
                    }
                    reply = renderTemplate(template);
                }
                case "lastOutput" -> {
                    // 使用上一个节点的输出
                    reply = ctx.getLastOutput();
                }
                case "nodeOutput" -> {
                    // 使用指定节点的输出
                    String nodeId = getConfigString("sourceNodeId", "");
                    Object output = ctx.getOutput(nodeId);
                    reply = output != null ? output.toString() : "";
                }
                case "variable" -> {
                    // 使用变量值
                    String variableName = getConfigString("variableName", "");
                    Object value = ctx.getVariable(variableName);
                    reply = value != null ? value.toString() : "";
                }
                default -> {
                    // 默认也尝试解析 text 字段作为模板
                    String text = getConfigString("text", "");
                    if (text != null && !text.isEmpty()) {
                        reply = renderTemplate(text);
                    } else {
                        reply = ctx.getLastOutput();
                    }
                }
            }
            
            // 设置最终回复
            ctx.setFinalReply(reply);
            setOutput(reply);
            
            log.info("回复节点设置回复: {}", reply);
            recordExecution(ctx.getLastOutput(), reply, startTime, true, null);
            
        } catch (Exception e) {
            log.error("回复节点执行失败", e);
            String errorReply = "抱歉，处理您的请求时出现问题。";
            ctx.setFinalReply(errorReply);
            setOutput(errorReply);
            recordExecution(null, errorReply, startTime, false, e.getMessage());
        }
    }
}

