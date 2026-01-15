package com.example.aikef.workflow.node;

import com.example.aikef.model.AgentSession;
import com.example.aikef.model.AiWorkflow;
import com.example.aikef.repository.AgentSessionRepository;
import com.example.aikef.repository.AiWorkflowRepository;
import com.example.aikef.workflow.context.WorkflowContext;
import com.example.aikef.workflow.exception.WorkflowPausedException;
import com.example.aikef.workflow.service.AiWorkflowService;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Agent 节点
 * 配置其他工作流ID，在流转到这个节点时启动配置的工作流
 * 创建 AgentSession 并标记会话，所有后续消息都会从这个工作流流转
 * 直到走到 Agent End 节点才注销标记
 * 
 * 配置示例:
 * {
 *   "workflowId": "uuid-of-workflow-to-start"
 * }
 */
@LiteflowComponent("flow")
public class FlowNode extends BaseWorkflowNode {

    private static final Logger log = LoggerFactory.getLogger(FlowNode.class);

    @Resource
    private AiWorkflowRepository workflowRepository;

    @Resource
    private AgentSessionRepository agentSessionRepository;

    @Resource
    private AiWorkflowService workflowService;

    @Override
    public void process() {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = getWorkflowContext();
        
        try {
            // 获取配置的工作流ID
            String workflowIdStr = getConfigString("workflowId");
            if (workflowIdStr == null || workflowIdStr.isEmpty()) {
                log.error("Agent 节点未配置 workflowId");
                setOutput("error: workflowId not configured");
                recordExecution(null, "error", startTime, false, "workflowId not configured");
                return;
            }

            UUID targetWorkflowId = UUID.fromString(workflowIdStr);
            AiWorkflow targetWorkflow = workflowRepository.findById(targetWorkflowId)
                    .orElseThrow(() -> new IllegalArgumentException("目标工作流不存在: " + targetWorkflowId));

            if (!targetWorkflow.getEnabled()) {
                log.error("目标工作流已禁用: workflowId={}", targetWorkflowId);
                setOutput("error: target workflow is disabled");
                recordExecution(null, "error", startTime, false, "target workflow is disabled");
                return;
            }

            UUID sessionId = ctx.getSessionId();
            if (sessionId == null) {
                log.error("Agent 节点需要 sessionId，但上下文中没有");
                setOutput("error: sessionId required");
                recordExecution(null, "error", startTime, false, "sessionId required");
                return;
            }

            // 检查是否已存在未结束的 AgentSession
            var existingSession = agentSessionRepository.findBySessionIdAndWorkflowIdAndNotEnded(
                    sessionId, targetWorkflowId);
            
            if (existingSession.isPresent()) {
                log.info("AgentSession 已存在，跳过创建: sessionId={}, workflowId={}", 
                        sessionId, targetWorkflowId);
                setOutput("agent_session_exists");
                recordExecution(null, "agent_session_exists", startTime, true, null);
                return;
            }

            // 创建 AgentSession
            AgentSession agentSession = new AgentSession();
            agentSession.setSessionId(sessionId);
            agentSession.setWorkflow(targetWorkflow);
            
            // 以上一个节点的输入作为 sysPrompt
            String sysPrompt = getInput();
            agentSession.setSysPrompt(sysPrompt != null ? sysPrompt : "");
            
            agentSession = agentSessionRepository.save(agentSession);
            
            log.info("创建 AgentSession: sessionId={}, workflowId={}, sysPrompt={}", 
                    sessionId, targetWorkflowId, sysPrompt);

            // 立即启动目标工作流，使用当前工作流的用户输入作为输入
            String userInput = ctx.getQuery();
            if (userInput == null || userInput.isEmpty()) {
                // 如果没有用户输入，使用上一个节点的输出
                userInput = getInput();
            }
            
            // 构建变量（传递当前上下文的变量）
            Map<String, Object> variables = new HashMap<>();
            if (ctx.getVariables() != null) {
                variables.putAll(ctx.getVariables());
            }
            variables.put("sessionId", sessionId);
            if (ctx.getCustomerId() != null) {
                variables.put("customerId", ctx.getCustomerId());
            }
            
            log.info("立即启动 Agent 工作流: sessionId={}, workflowId={}, userInput={}", 
                    sessionId, targetWorkflowId, userInput);
            
            // 执行目标工作流（带 AgentSession）
            // 从上下文获取触发消息ID（如果有）
            UUID messageId = ctx.getMessageId();
            AiWorkflowService.WorkflowExecutionResult result = 
                    workflowService.executeWorkflowInternalWithAgentSession(
                            targetWorkflow, sessionId, userInput, variables, agentSession, messageId);
            
            // 将目标工作流的输出作为当前节点的输出
            String output = result.reply();
            if (output == null || output.isEmpty()) {
                output = "Agent 工作流执行完成";
            }
            
            setOutput(output);
            recordExecution(userInput, output, startTime, result.success(), result.errorMessage());
            
            log.info("Agent 工作流执行完成: sessionId={}, workflowId={}, success={}, output={}", 
                    sessionId, targetWorkflowId, result.success(), output);

        } catch (WorkflowPausedException e) {
            throw e; // 重新抛出暂停异常
        } catch (Exception e) {
            log.error("Agent 节点执行失败", e);
            setOutput("error: " + e.getMessage());
            recordExecution(null, "error", startTime, false, e.getMessage());
        }
    }
}

