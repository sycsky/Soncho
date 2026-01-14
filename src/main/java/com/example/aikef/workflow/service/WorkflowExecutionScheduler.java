package com.example.aikef.workflow.service;

import com.example.aikef.model.ChatSession;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.service.SessionMessageGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 工作流执行调度服务
 * 实现防抖和调度功能：
 * 1. 防抖：短时间内多条消息只取最新一条执行工作流
 * 2. 调度：执行中的会话，新消息入队；执行完后出队处理
 */
@Slf4j
@Service
public class WorkflowExecutionScheduler {

    private final AiWorkflowService workflowService;
    private final ChatSessionRepository sessionRepository;
    private final SessionMessageGateway messageGateway;

    // 防抖时间（秒），默认3秒
    @Value("${workflow.debounce.seconds:3}")
    private int debounceSeconds;

    // 每个会话的执行状态
    private final Map<UUID, SessionExecutionState> executionStates = new ConcurrentHashMap<>();

    // 锁，确保每个会话的执行是串行的
    private final Map<UUID, ReentrantLock> sessionLocks = new ConcurrentHashMap<>();

    // 用于管理防抖定时任务的线程池
    private final ScheduledExecutorService debounceExecutor;

    public WorkflowExecutionScheduler(AiWorkflowService workflowService,
                                      ChatSessionRepository sessionRepository,
                                      SessionMessageGateway messageGateway) {
        this.workflowService = workflowService;
        this.sessionRepository = sessionRepository;
        this.messageGateway = messageGateway;
        // 创建单线程调度器用于防抖任务
        this.debounceExecutor = Executors.newScheduledThreadPool(10, r -> {
            Thread t = new Thread(r, "workflow-debounce-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 会话执行状态
     */
    private static class SessionExecutionState {
        UUID sessionId;
        boolean isExecuting = false;  // 是否正在执行工作流
        Queue<PendingMessage> messageQueue = new LinkedList<>();  // 消息队列
        List<PendingMessage> debounceBuffer = new ArrayList<>();  // 防抖缓冲区
        Instant lastMessageTime;  // 最后一条消息时间
        ScheduledFuture<?> debounceTask;  // 当前防抖任务（可取消）
        ReentrantLock lock = new ReentrantLock();  // 会话锁

        SessionExecutionState(UUID sessionId) {
            this.sessionId = sessionId;
        }
    }

    /**
     * 待处理消息
     */
    private static class PendingMessage {
        String content;
        Instant timestamp;
        UUID messageId;  // 触发工作流的消息ID

        PendingMessage(String content, Instant timestamp, UUID messageId) {
            this.content = content;
            this.timestamp = timestamp;
            this.messageId = messageId;
        }
    }

    /**
     * 提交消息执行工作流（带防抖和调度）
     * 
     * @param sessionId 会话ID
     * @param userMessage 用户消息
     * @param messageId 触发工作流的消息ID（可为null）
     */
    public void submitMessage(UUID sessionId, String userMessage, UUID messageId) {
        SessionExecutionState state = executionStates.computeIfAbsent(
                sessionId, SessionExecutionState::new);

        ReentrantLock lock = sessionLocks.computeIfAbsent(sessionId, k -> new ReentrantLock());
        Instant now = Instant.now();
        lock.lock();
        try {

            PendingMessage message = new PendingMessage(userMessage, now, messageId);
            
            if (state.isExecuting) {
                // 如果正在执行，消息入队
                state.messageQueue.offer(message);
                log.debug("工作流正在执行，消息入队: sessionId={}, messageId={}, queueSize={}, message={}",
                        sessionId, messageId, state.messageQueue.size(),
                        userMessage.length() > 50 ? userMessage.substring(0, 50) + "..." : userMessage);
            } else {
                // 如果未在执行，添加到防抖缓冲区并启动防抖定时器
                state.debounceBuffer.add(message);
                state.lastMessageTime = now;
                
                log.debug("收到消息，添加到防抖缓冲区: sessionId={}, messageId={}, message={}, bufferSize={}",
                        sessionId, messageId, userMessage.length() > 50 ? userMessage.substring(0, 50) + "..." : userMessage,
                        state.debounceBuffer.size());
                
                // 启动防抖定时器
                scheduleDebouncedExecution(state);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 调度防抖执行
     * 在防抖时间后，如果缓冲区有消息，取最新一条执行
     */
    private void scheduleDebouncedExecution(SessionExecutionState state) {
        // 取消之前的防抖任务（如果存在且未完成）
        if (state.debounceTask != null && !state.debounceTask.isDone()) {
            boolean cancelled = state.debounceTask.cancel(false);
            log.debug("取消之前的防抖任务: sessionId={}, cancelled={}", state.sessionId, cancelled);
        }

        // 创建新的防抖任务（延迟执行）
        // 注意：使用 cancel(false) 只会取消未开始执行的任务
        // 如果任务已经开始执行，会继续执行完成，锁会在 finally 中正常释放
        state.debounceTask = debounceExecutor.schedule(() -> {
            ReentrantLock lock = sessionLocks.get(state.sessionId);
            if (lock == null) {
                return;
            }
            
            // 在获取锁之前检查任务是否被取消
            // 虽然 cancel(false) 不会取消已开始的任务，但这里检查一下更安全
            if (state.debounceTask != null && state.debounceTask.isCancelled()) {
                log.debug("防抖任务已被取消，跳过执行: sessionId={}", state.sessionId);
                return;
            }
            
            lock.lock();
            try {
                // 再次检查任务是否被取消（获取锁后检查，防止在获取锁期间被取消）
                if (state.debounceTask != null && state.debounceTask.isCancelled()) {
                    log.debug("防抖任务在获取锁后已被取消: sessionId={}", state.sessionId);
                    return;
                }
                
                // 检查是否在执行中（可能被其他消息触发）
                if (state.isExecuting) {
                    log.debug("防抖期间工作流已开始执行，跳过: sessionId={}", state.sessionId);
                    return;
                }
                
                // 检查防抖缓冲区是否有消息
                if (state.debounceBuffer.isEmpty()) {
                    log.debug("防抖缓冲区为空，跳过执行: sessionId={}", state.sessionId);
                    return;
                }
                
                // 取最新一条消息（最后一条）
                PendingMessage latestMessage = state.debounceBuffer.get(state.debounceBuffer.size() - 1);
                String messageContent = latestMessage.content;
                UUID messageId = latestMessage.messageId;
                state.debounceBuffer.clear();
                
                // 设置执行标志（在锁内）
                state.isExecuting = true;
                
                // 执行工作流（异步，不在锁内执行）
                executeWorkflowAsync(state, messageContent, messageId);
                
            } catch (Exception e) {
                log.error("防抖任务执行失败: sessionId={}", state.sessionId, e);
            } finally {
                // 确保锁被释放（即使发生异常或提前返回）
                // 注意：ReentrantLock 的 unlock() 必须在持有锁的线程中调用
                // 由于我们使用 cancel(false)，已开始的任务不会被中断，所以这里总是安全的
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }, debounceSeconds, TimeUnit.SECONDS);
        
        log.debug("已调度防抖任务: sessionId={}, debounceSeconds={}", state.sessionId, debounceSeconds);
    }

    /**
     * 异步执行工作流
     */
    private void executeWorkflowAsync(SessionExecutionState state, String userMessage, UUID messageId) {
        UUID sessionId = state.sessionId;
        
        log.info("开始执行工作流: sessionId={}, messageId={}, message={}", sessionId, messageId,
                userMessage.length() > 50 ? userMessage.substring(0, 50) + "..." : userMessage);
        
        // 异步执行工作流
        CompletableFuture.runAsync(() -> {
            try {

                // 执行工作流
                AiWorkflowService.WorkflowExecutionResult result = 
                        workflowService.executeForSession(sessionId, userMessage, messageId);
                
                // 发送AI回复
                if (result.success() && result.reply() != null && !result.reply().isBlank()) {
                    messageGateway.sendAiMessage(sessionId, result.reply());
                    log.info("工作流执行成功: sessionId={}, reply长度={}", 
                            sessionId, result.reply().length());
                } else if (!result.success()) {
                    log.warn("工作流执行失败: sessionId={}, error={}", 
                            sessionId, result.errorMessage());
                    messageGateway.sendAiMessage(sessionId, "The system seems to be experiencing some issues. You can try again later");
                }
                
            } catch (Exception e) {
                log.error("工作流执行异常: sessionId={}", sessionId, e);
            } finally {
                // 执行完成，处理队列中的消息
                handleQueuedMessages(state);
            }
        });
    }

    /**
     * 处理队列中的消息
     * 出队逻辑：
     * 1. 取队列第一条消息，计算 X = 第一条消息的发送时间 + 防抖时间
     * 2. 取下一条消息，如果发送时间 < X，则出队，更新 X = 这条消息的发送时间 + 防抖时间
     * 3. 继续取下一条，如果发送时间 < X，则出队，更新 X
     * 4. 直到队列为空或下一条消息的发送时间 > X
     * 5. 最后取出的消息（时间最晚的）作为要执行的消息
     * 6. 没出队的消息留在队列中等待下一次执行
     */
    private void handleQueuedMessages(SessionExecutionState state) {
        ReentrantLock lock = sessionLocks.get(state.sessionId);
        if (lock == null) {
            return;
        }
        
        lock.lock();
        try {
            state.isExecuting = false;
            
            // 如果队列为空，直接返回
            if (state.messageQueue.isEmpty()) {
                log.debug("队列为空，无需处理: sessionId={}", state.sessionId);
                return;
            }
            
            // 出队的消息列表（用于记录）
            List<PendingMessage> dequeuedMessages = new ArrayList<>();
            PendingMessage selectedMessage = null;
            
            // 1. 取队列第一条消息
            PendingMessage firstMessage = state.messageQueue.peek();
            if (firstMessage == null) {
                return;
            }
            
            // 计算 X = 第一条消息的发送时间 + 防抖时间
            Instant X = firstMessage.timestamp.plusSeconds(debounceSeconds);
            
            // 2. 循环处理队列中的消息
            while (!state.messageQueue.isEmpty()) {
                PendingMessage currentMessage = state.messageQueue.peek();
                
                // 如果当前消息的发送时间 > X，停止出队
                if (currentMessage.timestamp.isAfter(X)) {
                    break;
                }
                
                // 出队当前消息
                PendingMessage dequeued = state.messageQueue.poll();
                dequeuedMessages.add(dequeued);
                selectedMessage = dequeued; // 更新选中的消息（最后取出的）
                
                // 更新 X = 这条消息的发送时间 + 防抖时间
                X = dequeued.timestamp.plusSeconds(debounceSeconds);
            }
            
            // 3. 如果有出队的消息，执行最后一条
            if (selectedMessage != null) {
                log.info("处理队列消息: sessionId={}, messageId={}, dequeuedCount={}, remainingInQueue={}, selectedMessage={}", 
                        state.sessionId, selectedMessage.messageId, dequeuedMessages.size(), 
                        state.messageQueue.size(),
                        selectedMessage.content.length() > 50 ? 
                        selectedMessage.content.substring(0, 50) + "..." : selectedMessage.content);
                
                // 设置执行标志（在锁内）
                state.isExecuting = true;
                
                // 执行工作流（异步，不在锁内执行）
                executeWorkflowAsync(state, selectedMessage.content, selectedMessage.messageId);
            } else {
                log.debug("队列中没有符合条件的消息需要处理: sessionId={}", state.sessionId);
            }
            
        } finally {
            lock.unlock();
        }
    }

    /**
     * 检查会话是否正在执行工作流
     */
    public boolean isExecuting(UUID sessionId) {
        SessionExecutionState state = executionStates.get(sessionId);
        return state != null && state.isExecuting;
    }

    /**
     * 清理会话的执行状态（会话结束时调用）
     */
    public void cleanupSession(UUID sessionId) {
        SessionExecutionState state = executionStates.remove(sessionId);
        sessionLocks.remove(sessionId);
        if (state != null) {
            log.info("清理会话执行状态: sessionId={}", sessionId);
        }
    }

    /**
     * 设置防抖时间（秒）
     */
    public void setDebounceSeconds(int seconds) {
        this.debounceSeconds = seconds;
        log.info("防抖时间已更新: {}秒", seconds);
    }

    /**
     * 获取防抖时间（秒）
     */
    public int getDebounceSeconds() {
        return debounceSeconds;
    }

    /**
     * 服务关闭时清理资源
     */
    @PreDestroy
    public void shutdown() {
        log.info("正在关闭工作流执行调度服务...");
        debounceExecutor.shutdown();
        try {
            if (!debounceExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("防抖线程池未在5秒内关闭，强制关闭");
                debounceExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            debounceExecutor.shutdownNow();
        }
        log.info("工作流执行调度服务已关闭");
    }
}

