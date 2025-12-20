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
//            String replyType = getConfigString("replyType", "template");
            String reply;

            reply = ctx.getLastOutput();
            
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

