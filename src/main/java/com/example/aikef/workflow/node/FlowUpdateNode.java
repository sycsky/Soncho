package com.example.aikef.workflow.node;

import com.example.aikef.model.AgentSession;
import com.example.aikef.repository.AgentSessionRepository;
import com.example.aikef.workflow.context.WorkflowContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Agent Update 节点
 * 接收上一个节点的输入，用来修改 AgentSession.sysPrompt 的值
 * 
 * 配置示例:
 * {
 *   "updateMode": "replace" // replace: 替换, append: 追加
 * }
 */
@LiteflowComponent("flow_update")
public class FlowUpdateNode extends BaseWorkflowNode {

    private static final Logger log = LoggerFactory.getLogger(FlowUpdateNode.class);

    @Resource
    private AgentSessionRepository agentSessionRepository;

    @Override
    public void process() {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = getWorkflowContext();
        
        try {
            UUID sessionId = ctx.getSessionId();
            if (sessionId == null) {
                log.error("AgentUpdate 节点需要 sessionId，但上下文中没有");
                setOutput("error: sessionId required");
                recordExecution(null, "error", startTime, false, "sessionId required");
                return;
            }

            // 获取当前工作流ID（应该是 Agent 节点启动的工作流）
            UUID currentWorkflowId = ctx.getWorkflowId();
            if (currentWorkflowId == null) {
                log.error("AgentUpdate 节点需要 workflowId，但上下文中没有");
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
                setOutput("error: agent session not found");
                recordExecution(null, "error", startTime, false, "agent session not found");
                return;
            }

            AgentSession agentSession = agentSessionOpt.get();
            
            // 获取上一个节点的输入
            String newValue = getInput();
            if (newValue == null) {
                newValue = "";
            }

            // 获取更新模式
            String updateMode = getConfigString("updateMode", "replace");
            
            if ("append".equals(updateMode)) {
                // 追加模式
                String existing = agentSession.getSysPrompt();
                if (existing == null) {
                    existing = "";
                }
                agentSession.setSysPrompt(existing + "\n" + newValue);
            } else {
                // 替换模式（默认）
                agentSession.setSysPrompt(newValue);
            }

            agentSessionRepository.save(agentSession);
            
            log.info("更新 AgentSession.sysPrompt: sessionId={}, workflowId={}, mode={}, newValue={}", 
                    sessionId, currentWorkflowId, updateMode, newValue);

            setOutput("agent_session_updated");
            recordExecution(newValue, "agent_session_updated", startTime, true, null);

        } catch (Exception e) {
            log.error("AgentUpdate 节点执行失败", e);
            setOutput("error: " + e.getMessage());
            recordExecution(null, "error", startTime, false, e.getMessage());
        }
    }
}

