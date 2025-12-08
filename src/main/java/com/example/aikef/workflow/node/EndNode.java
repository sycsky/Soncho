package com.example.aikef.workflow.node;

import com.example.aikef.workflow.context.WorkflowContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;

/**
 * 结束节点
 * 工作流的出口，收集最终结果
 */
@LiteflowComponent("end")
public class EndNode extends BaseWorkflowNode {

    @Override
    public void process() {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = getWorkflowContext();
        
        // 如果没有设置最终回复，使用最后一个节点的输出
        if (ctx.getFinalReply() == null || ctx.getFinalReply().isEmpty()) {
            ctx.setFinalReply(ctx.getLastOutput());
        }
        
        log.info("工作流执行结束, sessionId={}, finalReply={}", 
                ctx.getSessionId(),
                ctx.getFinalReply());
        
        recordExecution(ctx.getLastOutput(), ctx.getFinalReply(), startTime, true, null);
    }
}

