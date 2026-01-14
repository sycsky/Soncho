package com.example.aikef.workflow.node;

import com.example.aikef.saas.context.TenantContext;
import com.example.aikef.workflow.context.WorkflowContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;

/**
 * 结束节点
 * 工作流的出口，收集最终结果并清理租户上下文
 */
@LiteflowComponent("end")
public class EndNode extends BaseWorkflowNode {

    @Override
    public void process() {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = getWorkflowContext();
        
        try {
            // 如果没有设置最终回复，使用最后一个节点的输出
            if (ctx.getFinalReply() == null || ctx.getFinalReply().isEmpty()) {
                ctx.setFinalReply(ctx.getLastOutput());
            }
            
            log.info("工作流执行结束, sessionId={}, tenantId={}, finalReply={}", 
                    ctx.getSessionId(),
                    TenantContext.getTenantId(),
                    ctx.getFinalReply());
            
            recordExecution(ctx.getLastOutput(), ctx.getFinalReply(), startTime, true, null);
        } finally {
            // 清除租户上下文，防止线程池复用时的数据泄露
//            TenantContext.clear();
            log.debug("工作流结束节点：已清除租户上下文, sessionId={}", ctx.getSessionId());
        }
    }
}

