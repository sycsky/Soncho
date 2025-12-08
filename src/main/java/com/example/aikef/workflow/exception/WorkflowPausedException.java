package com.example.aikef.workflow.exception;

/**
 * 工作流暂停异常
 * 用于在需要暂停工作流时中断执行（如等待用户输入参数）
 */
public class WorkflowPausedException extends RuntimeException {
    
    private final String pauseReason;
    private final String pauseMessage;
    
    public WorkflowPausedException(String pauseReason, String pauseMessage) {
        super("工作流暂停: " + pauseReason);
        this.pauseReason = pauseReason;
        this.pauseMessage = pauseMessage;
    }
    
    public String getPauseReason() {
        return pauseReason;
    }
    
    public String getPauseMessage() {
        return pauseMessage;
    }
}

