package com.example.aikef.workflow.node;

import com.yomahub.liteflow.annotation.LiteflowComponent;

/**
 * 开始节点
 * 工作流的入口，不做任何处理
 */
@LiteflowComponent("start")
public class StartNode extends BaseWorkflowNode {

    @Override
    public void process() {
        long startTime = System.currentTimeMillis();
        
        log.info("工作流开始执行, sessionId={}, query={}", 
                getWorkflowContext().getSessionId(),
                getWorkflowContext().getQuery());
        

        
        recordExecution(null, "workflow_started", startTime, true, null);
    }
}

