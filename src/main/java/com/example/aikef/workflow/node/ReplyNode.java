package com.example.aikef.workflow.node;

import com.example.aikef.service.SessionMessageGateway;
import com.example.aikef.workflow.context.WorkflowContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 回复节点
 * 设置最终回复内容
 * 
 * 配置示例:
 * {
 *   "text": "您好，{{var.customerName}}，您的订单{{var.orderId}}已处理完成。"
 * }
 * 
 * 支持的模板变量:
 * - {{sys.query}} - 用户输入查询
 * - {{sys.lastOutput}} - 上一个节点的输出
 * - {{sys.intent}} - 识别的意图
 * - {{sys.now}} - 当前日期 (格式: yyyy-MM-dd)
 * - {{var.variableName}} - 自定义变量
 * - {{node.nodeId}} - 指定节点的输出
 * - {{customer.name}} - 客户信息
 * - {{entity.entityName}} - 提取的实体
 * 
 * 注意: 如果未配置 text 字段，将使用上一个节点的输出 (lastOutput)
 */
@LiteflowComponent("reply")
public class ReplyNode extends BaseWorkflowNode {

    @Autowired
    private SessionMessageGateway messageGateway;

    @Override
    public void process() {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = getWorkflowContext();
        
        try {
            // 从配置字段 text 中获取回复文本
            String text = getConfigString("text");
            String reply;
            
            if (text != null && !text.isEmpty()) {
                // 如果配置了 text，使用它（支持模板变量）
                reply = renderTemplate(text);
            } else {
                // 如果没有配置 text，回退到使用上一个节点的输出
                reply = ctx.getLastOutput();
                log.warn("回复节点未配置 text 字段，使用上一个节点的输出");
            }

            messageGateway.sendAiMessage(ctx.getSessionId(), reply);

            
            log.info("回复节点设置回复: {}", reply);
            recordExecution(text != null ? text : ctx.getLastOutput(), reply, startTime, true, null);
            
        } catch (Exception e) {
            log.error("回复节点执行失败", e);
            String errorReply = "抱歉，处理您的请求时出现问题。";
            messageGateway.sendAiMessage(ctx.getSessionId(), errorReply);
            ctx.setFinalReply(errorReply);
            setOutput(errorReply);
            recordExecution(null, errorReply, startTime, false, e.getMessage());
        }
    }
}

