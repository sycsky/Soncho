package com.example.aikef.workflow.node;

import com.example.aikef.model.AgentSession;
import com.example.aikef.repository.AgentSessionRepository;
import com.example.aikef.workflow.context.WorkflowContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

/**
 * Agent End 节点
 * 注销当前 AgentSession，标记为已结束
 * 之后会话将恢复正常的匹配流程（分类绑定工作流）
 */
@LiteflowComponent("agent_end")
public class AgentEndNode extends BaseWorkflowNode {

    private static final Logger log = LoggerFactory.getLogger(AgentEndNode.class);

    @Resource
    private AgentSessionRepository agentSessionRepository;

    @Override
    public void process() {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = getWorkflowContext();
        
        try {
            UUID sessionId = ctx.getSessionId();
            if (sessionId == null) {
                log.error("AgentEnd 节点需要 sessionId，但上下文中没有");
                setOutput("error: sessionId required");
                recordExecution(null, "error", startTime, false, "sessionId required");
                return;
            }

            // 获取当前工作流ID（应该是 Agent 节点启动的工作流）
            UUID currentWorkflowId = ctx.getWorkflowId();
            if (currentWorkflowId == null) {
                log.error("AgentEnd 节点需要 workflowId，但上下文中没有");
                setOutput("error: workflowId required");
                recordExecution(null, "error", startTime, false, "workflowId required");
                return;
            }

            // 查找 AgentSession
            var agentSessionOpt = agentSessionRepository.findBySessionIdAndWorkflowIdAndNotEnded(
                    sessionId, currentWorkflowId);
            
            if (agentSessionOpt.isEmpty()) {
                log.warn("未找到 AgentSession: sessionId={}, workflowId={}", 
                        sessionId, currentWorkflowId);
                setOutput("warning: agent session not found");
                recordExecution(null, "warning", startTime, true, "agent session not found");
                return;
            }

            AgentSession agentSession = agentSessionOpt.get();
            
            // 标记为已结束
            agentSession.setEnded(true);
            agentSession.setEndedAt(Instant.now());
            agentSessionRepository.save(agentSession);
            
            log.info("注销 AgentSession: sessionId={}, workflowId={}", 
                    sessionId, currentWorkflowId);

            setOutput("agent_session_ended");
            recordExecution(null, "agent_session_ended", startTime, true, null);

        } catch (Exception e) {
            log.error("AgentEnd 节点执行失败", e);
            setOutput("error: " + e.getMessage());
            recordExecution(null, "error", startTime, false, e.getMessage());
        }
    }
}

