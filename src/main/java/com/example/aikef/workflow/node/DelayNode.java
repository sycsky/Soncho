package com.example.aikef.workflow.node;

import com.example.aikef.workflow.context.WorkflowContext;
import com.example.aikef.workflow.service.WorkflowExecutionScheduler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 延迟节点
 * 使用 SQS 延迟队列实现
 */
@Component("delay")
public class DelayNode extends BaseWorkflowNode {

    @Resource
    private WorkflowExecutionScheduler scheduler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void process() throws Exception {
        long startTime = System.currentTimeMillis();
        WorkflowContext ctx = getWorkflowContext();
        JsonNode config = getNodeConfig();

        try {
            // 1. 获取配置
            String targetWorkflowIdStr = readConfigString(config, "targetWorkflowId", null);
            String targetWorkflowName = readConfigString(config, "targetWorkflowName", null);
            int delayMinutes = readConfigInt(config, "delayMinutes", 0);
            String inputDataTemplate = readConfigString(config, "inputData", "");

            if (targetWorkflowName == null || targetWorkflowName.isEmpty() ) {
                throw new IllegalArgumentException("未配置延迟后触发的工作流");
            }

            // 限制最大延迟时间为一天
            if (delayMinutes > 60 * 24) {
                delayMinutes = 60 * 24;
            }

            // 2. 处理模板数据
            String processedInputData = renderTemplate(inputDataTemplate);

            // 3. 构建延迟任务数据
            Map<String, Object> taskData = new HashMap<>();
            taskData.put("sessionId", ctx.getSessionId());
            taskData.put("workflowId", targetWorkflowIdStr);
            taskData.put("workflowName", targetWorkflowName);
            taskData.put("inputData", processedInputData);
            taskData.put("originalWorkflowId", ctx.getWorkflowId());

            // 4. 调度延迟任务
            scheduler.scheduleDelayTask(taskData, delayMinutes);

            log.info("延迟节点执行成功: sessionId={}, targetWorkflowId={}, targetWorkflowName={}, delayMinutes={}",
                    ctx.getSessionId(), targetWorkflowIdStr, targetWorkflowName, delayMinutes);

            recordExecution(ctx, getActualNodeId(), "delay", getName(), 
                    config, "Task scheduled", startTime, true, null);

        } catch (Exception e) {
            log.error("延迟节点执行失败", e);
            recordExecution(ctx, getActualNodeId(), "delay", getName(), 
                    config, null, startTime, false, e.getMessage());
            throw e;
        }
    }
}
