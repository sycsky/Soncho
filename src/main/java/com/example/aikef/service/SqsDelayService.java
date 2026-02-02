package com.example.aikef.service;

import com.example.aikef.model.AiWorkflow;
import com.example.aikef.workflow.service.AiWorkflowService;
import com.example.aikef.model.AiWorkflow;
import com.example.aikef.service.SessionMessageGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class SqsDelayService {

    private final SqsClient sqsClient;
    private final AiWorkflowService workflowService;
    private final SessionMessageGateway messageGateway;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${aws.sqs.queue-url:}")
    private String queueUrl;

    @Value("${aws.sqs.enabled:true}")
    private boolean enabled;

    private ExecutorService executorService;
    private volatile boolean running = true;

    public SqsDelayService(SqsClient sqsClient, @Lazy AiWorkflowService workflowService, @Lazy SessionMessageGateway messageGateway) {
        this.sqsClient = sqsClient;
        this.workflowService = workflowService;
        this.messageGateway = messageGateway;
    }

    @PostConstruct
    public void init() {
        if (enabled && queueUrl != null && !queueUrl.isEmpty()) {
            executorService = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "sqs-poller");
                t.setDaemon(true);
                return t;
            });
            executorService.submit(this::pollMessages);
            log.info("SQS 延迟任务监听器已启动: {}", queueUrl);
        } else {
            log.warn("SQS 延迟任务监听器未启用或未配置队列 URL");
        }
    }

    public void sendDelayMessage(Map<String, Object> taskData, int delayMinutes) {
        if (!enabled || queueUrl == null || queueUrl.isEmpty()) {
            log.warn("SQS 未启用，无法发送延迟消息");
            return;
        }

        try {
            String messageBody = objectMapper.writeValueAsString(taskData);
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .delaySeconds(delayMinutes * 60)
                    .build();

            sqsClient.sendMessage(request);
            log.info("已发送延迟消息到 SQS: delay={}min, body={}", delayMinutes, messageBody);
        } catch (Exception e) {
            log.error("发送 SQS 延迟消息失败", e);
        }
    }

    private void pollMessages() {
        while (running) {
            try {
                ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(20) // Long polling
                        .build();

                ReceiveMessageResponse response = sqsClient.receiveMessage(receiveRequest);
                for (Message message : response.messages()) {
                    processMessage(message);
                }
            } catch (Exception e) {
                log.error("轮询 SQS 消息出错", e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void processMessage(Message message) {
        try {
            log.info("收到 SQS 延迟任务消息: {}", message.body());
            Map<String, Object> taskData = objectMapper.readValue(message.body(), Map.class);
            
            String sessionIdStr = (String) taskData.get("sessionId");
            UUID sessionId = (sessionIdStr != null && !sessionIdStr.isEmpty()) ? UUID.fromString(sessionIdStr) : null;
            
            String workflowIdStr = (String) taskData.get("workflowId");
            if (workflowIdStr == null || workflowIdStr.isEmpty()) {
                log.warn("SQS 消息中缺少 workflowId, 跳过处理: {}", message.body());
                deleteMessage(message);
                return;
            }
            UUID workflowId = UUID.fromString(workflowIdStr);
            String workflowName = (String) taskData.get("workflowName");

            // 验证工作流是否存在且名称匹配
            try {
                AiWorkflow workflow = workflowService.getWorkflow(workflowId);
                if (workflowName != null && !workflowName.isEmpty() && !workflowName.equals(workflow.getName())) {
                    log.warn("SQS 消息中工作流名称不匹配, 跳过处理: id={}, expectedName={}, actualName={}", 
                            workflowId, workflowName, workflow.getName());
                    deleteMessage(message);
                    return;
                }
            } catch (Exception e) {
                log.warn("获取工作流失败或工作流不存在: id={}, error={}", workflowId, e.getMessage());
                deleteMessage(message);
                return;
            }

            String inputData = (String) taskData.get("inputData");

            Map<String, Object> variables = new HashMap<>();
            variables.put("inputData", inputData);
            variables.put("_fromDelayNode", true);

            // 触发工作流
            AiWorkflowService.WorkflowExecutionResult result = workflowService.executeWorkflow(workflowId, sessionId, inputData, variables);

            // 如果工作流执行成功且有回复，发送 AI 回复消息
            if (result.success() && result.reply() != null && !result.reply().isBlank()) {
                try {
                    messageGateway.sendAiMessage(sessionId, result.reply());
                    log.info("延迟节点触发工作流执行成功，已发送 AI 回复: workflowId={}, sessionId={}, replyLength={}", 
                            workflowId, sessionId, result.reply().length());
                } catch (Exception e) {
                    log.error("发送 AI 回复失败: workflowId={}, sessionId={}", workflowId, sessionId, e);
                }
            } else if (!result.success()) {
                 log.warn("延迟节点触发工作流执行失败: workflowId={}, sessionId={}, error={}", 
                    workflowId, sessionId, result.errorMessage());
            }

            // 删除消息
            deleteMessage(message);
            log.info("SQS 延迟任务处理完成并已删除消息: sessionId={}, workflowId={}", sessionId, workflowId);

        } catch (Exception e) {
            log.error("处理 SQS 消息失败: {}", message.body(), e);
        }
    }

    private void deleteMessage(Message message) {
        try {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build();
            sqsClient.deleteMessage(deleteRequest);
        } catch (Exception e) {
            log.error("删除 SQS 消息失败: {}", message.receiptHandle(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
