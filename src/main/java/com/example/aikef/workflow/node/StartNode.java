package com.example.aikef.workflow.node;

import com.example.aikef.model.ChatSession;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.saas.context.TenantContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import jakarta.annotation.Resource;

import java.util.UUID;

/**
 * 开始节点
 * 工作流的入口，在执行前通过会话信息注入租户ID
 */
@LiteflowComponent("start")
public class StartNode extends BaseWorkflowNode {

    @Resource
    private ChatSessionRepository chatSessionRepository;

    @Override
    public void process() {
        long startTime = System.currentTimeMillis();
        
        UUID sessionId = getWorkflowContext().getSessionId();
        String query = getWorkflowContext().getQuery();
        
        // 通过会话信息注入租户ID到 TenantContext
        if (sessionId != null) {
            try {
                ChatSession session = chatSessionRepository.findById(sessionId).orElse(null);
                if (session != null && session.getTenantId() != null && !session.getTenantId().isEmpty()) {
                    String tenantId = session.getTenantId();
                    TenantContext.setTenantId(tenantId);
                    log.info("工作流开始执行, sessionId={}, tenantId={}, query={}", 
                            sessionId, tenantId, query);
                } else {
                    log.warn("工作流开始执行, sessionId={}, 但未找到租户信息, query={}", 
                            sessionId, query);
                }
            } catch (Exception e) {
                log.error("工作流开始节点：设置租户上下文失败, sessionId={}, error={}", 
                        sessionId, e.getMessage(), e);
            }
        } else {
            log.info("工作流开始执行, sessionId=null, query={}", query);
        }
        
        // 开始节点标记起点
        setOutput("workflow_started");
        
        recordExecution(null, "workflow_started", startTime, true, null);
    }
}

