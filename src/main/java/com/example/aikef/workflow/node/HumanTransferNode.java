package com.example.aikef.workflow.node;

import com.example.aikef.model.ChatSession;
import com.example.aikef.model.enums.SessionStatus;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.service.SessionMessageGateway;
import com.example.aikef.workflow.context.WorkflowContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 转人工节点
 * 直接切换当前会话为人工处理状态
 */
@LiteflowComponent("human_transfer")
public class HumanTransferNode extends BaseWorkflowNode {

    @Autowired
    private ChatSessionRepository chatSessionRepository;
    
    @Autowired
    private SessionMessageGateway messageGateway;

    @Override
    public void process() {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = getWorkflowContext();
        
        try {
            // 获取转人工原因和消息
            String reason = getConfigString("reason", "用户请求转人工");
            String message = getConfigString("message", "已经为您转接到人工");
            
            // 设置转人工标记（兼容旧逻辑）
            ctx.setNeedHumanTransfer(true);
            ctx.setHumanTransferReason(reason);
            
            // 直接更新会话状态为人工处理
            if (ctx.getSessionId() != null) {
                ChatSession session = chatSessionRepository.findById(ctx.getSessionId()).orElse(null);
                if (session != null) {
                    session.setStatus(SessionStatus.HUMAN_HANDLING);
                    chatSessionRepository.save(session);
                    log.info("会话状态已切换为人工处理: sessionId={}, reason={}", ctx.getSessionId(), reason);
                    
//                    // 发送系统消息通知
//                    try {
//                        messageGateway.sendSystemMessage(ctx.getSessionId(), message);
//                    } catch (Exception e) {
//                        log.warn("发送转人工系统消息失败: sessionId={}", ctx.getSessionId(), e);
//                    }
                } else {
                    log.warn("会话不存在，无法切换状态: sessionId={}", ctx.getSessionId());
                }
            } else {
                log.warn("会话ID为空，无法切换状态");
            }
            
            // 设置转人工提示消息
            ctx.setFinalReply(message);
            
            setOutput(message);
            recordExecution(reason, message, startTime, true, null);
            
        } catch (Exception e) {
            log.error("转人工节点执行失败", e);
            String errorMessage = "转接人工客服时出现问题，请稍后重试。";
            ctx.setFinalReply(errorMessage);
            setOutput(errorMessage);
            recordExecution(null, errorMessage, startTime, false, e.getMessage());
        }
    }
}

