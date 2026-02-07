package com.example.aikef.workflow.util;

import com.example.aikef.model.Message;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 历史消息加载工具类
 * 用于统一加载工作流节点所需的历史消息记录
 */
@Slf4j
@Component
public class HistoryMessageLoader {

    private final MessageRepository messageRepository;

    public HistoryMessageLoader(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * 加载历史消息
     * 
     * @param sessionId 会话ID
     * @param readCount 读取数量（排除 SYSTEM 消息和空消息后的数量）
     * @param messageId 触发工作流的消息ID（可为null，用于时间过滤）
     * @return 历史消息列表（按时间正序，已过滤 SYSTEM 消息和空消息）
     */
    public List<Message> loadHistoryMessages(UUID sessionId, int readCount, UUID messageId) {
        List<Message> historyMessages = new ArrayList<>();
        //+1，除历史消息外，还包括了触发工作流的用户消息
        readCount++;
        try {
            // 获取触发工作流的消息的创建时间（如果提供了messageId）
            Instant maxCreatedAt;
            Message triggerMessage = null;
            if (messageId != null) {
                triggerMessage = messageRepository.findById(messageId).orElse(null);
                if (triggerMessage != null) {
                    maxCreatedAt = triggerMessage.getCreatedAt();
                    log.debug("根据触发消息时间过滤历史记录: sessionId={}, messageId={}, createdAt={}", 
                            sessionId, messageId, maxCreatedAt);
                } else {
                    log.warn("无法找到触发消息: sessionId={}, messageId={}，将查询所有历史消息", 
                            sessionId, messageId);
                    maxCreatedAt = null;
                }
            } else {
                maxCreatedAt = null;
            }

            // 分页查询消息，每次查询一部分，直到满足条件或没有更多消息
            // 初始查询数量适当放大，以减少查询次数
            int pageSize = Math.max(readCount * 2, 50); 
            int pageNum = 0;
            List<Message> candidateMessages = new ArrayList<>();
            boolean stopCollecting = false;
            int effectiveCount = 0; // 记录有效消息数量（不含 TOOL 消息）

            while (!stopCollecting) {
                List<Message> batchMessages;
                PageRequest pageRequest = PageRequest.of(pageNum, pageSize);

                if (maxCreatedAt != null) {
                    // 查询 <= maxCreatedAt 的消息，倒序
                    // 使用 maxCreatedAt + 1秒 作为上限，然后在内存过滤
                    Instant queryUpperBound = maxCreatedAt.plusSeconds(1);
                    batchMessages = messageRepository.findBySession_IdAndInternalFalseAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
                            sessionId, queryUpperBound, pageRequest);
                    
                    // 过滤掉晚于 maxCreatedAt 的消息
                    batchMessages = batchMessages.stream()
                            .filter(msg -> !msg.getCreatedAt().isAfter(maxCreatedAt))
                            .collect(Collectors.toList());
                } else {
                    // 查询最近消息，倒序
                    Page<Message> messagePage = messageRepository.findBySession_IdAndInternalFalseOrderByCreatedAtDesc(
                            sessionId, pageRequest);
                    batchMessages = messagePage.getContent();
                }

                if (batchMessages.isEmpty()) {
                    break; // 没有更多消息了
                }

                // 处理当前批次
                for (Message msg : batchMessages) {
                    // 遇到 SYSTEM 消息，停止收集之前的所有消息（包括本条 SYSTEM 及其之后的消息都不需要）
                    // 题目要求：遇到 SYSTEM 就截止，只查 SYSTEM 之前的消息
                    if (msg.getSenderType() == SenderType.SYSTEM) {
                        stopCollecting = true;
                        break;
                    }


                    if (msg.getText() == null || msg.getText().isEmpty()) {
                        continue;
                    }


                    // 计数逻辑：TOOL 消息不算 readCount
                    if (msg.getSenderType() != SenderType.TOOL) {
                        readCount--;
                    }

                    candidateMessages.add(msg);

                    // 为什么不是0,因为读取到最后一个普通消息后，它之前可能不是普通消息，可能是调用的工具记录，所以继续往前读取，尽量取把数据读取完整，包括完整的调用环
                    if(readCount<-1){
                        stopCollecting = true;
                        break;
                    }

                    

                }

                if (stopCollecting) {
                    break;
                }

                pageNum++; // 继续下一页
                
                // 防止无限循环（比如数据库全是 TOOL 消息且没有 SYSTEM 消息）
                if (pageNum > 20) { // 最多查 20 * 50 = 1000 条
                    log.warn("历史消息查询超过深度限制，强制停止");
                    break;
                }
            }

            // 如果触发消息存在且符合条件，且不在结果中，需要添加进去？
            // 之前的逻辑是这样的。现在的逻辑是按时间倒序查的。
            // 如果触发消息在时间范围内，它应该已经被查到了。
            // 如果触发消息是 SYSTEM，那它会触发截止。
            
            // 反转列表，按时间正序排列
            java.util.Collections.reverse(candidateMessages);
            historyMessages = candidateMessages;
            
            log.debug("加载历史消息: sessionId={}, messageId={}, 请求readCount={}, 实际返回={}, 有效条数={}", 
                    sessionId, messageId, readCount, historyMessages.size(), effectiveCount);
                    
        } catch (Exception e) {
            log.warn("加载历史消息失败: sessionId={}, messageId={}, error={}", 
                    sessionId, messageId, e.getMessage(), e);
        }
        
        return historyMessages;
    }

    /**
     * 加载历史消息并转换为 ChatMessage 格式
     *
     * @param sessionId 会话ID
     * @param readCount 读取数量
     * @param messageId 触发工作流的消息ID
     * @return ChatMessage 列表
     */
    public List<dev.langchain4j.data.message.ChatMessage> loadChatMessages(UUID sessionId, int readCount, UUID messageId) {
        List<dev.langchain4j.data.message.ChatMessage> historyMessages = new ArrayList<>();
        List<Message> dbMessages = loadHistoryMessages(sessionId, readCount, messageId);

        for (Message msg : dbMessages) {
            if (msg.getSenderType() == SenderType.USER) {
                historyMessages.add(dev.langchain4j.data.message.UserMessage.from(msg.getText()));
            } else if (msg.getSenderType() == SenderType.TOOL) {
                Map<String, Object> toolCallData = msg.getToolCallData();
                if (toolCallData != null && toolCallData.containsKey("aiMessage")) {
                    Object aiObj = toolCallData.get("aiMessage");
                    if (aiObj instanceof Map<?, ?> aiMapRaw) {
                        String aiText = toStringOrNull(aiMapRaw.get("text"));
                        String aiThinking = toStringOrNull(aiMapRaw.get("thinking"));

                        List<dev.langchain4j.agent.tool.ToolExecutionRequest> requests = new ArrayList<>();
                        Object reqsObj = aiMapRaw.get("requests");
                        if (reqsObj instanceof List<?> reqListRaw) {
                            for (Object reqItem : reqListRaw) {
                                if (reqItem instanceof Map<?, ?> reqMapRaw) {
                                    String reqId = toStringOrNull(reqMapRaw.get("id"));
                                    String reqName = toStringOrNull(reqMapRaw.get("name"));
                                    String reqArgs = toStringOrNull(reqMapRaw.get("arguments"));
                                    if (reqId == null || reqName == null) {
                                        continue;
                                    }
                                    requests.add(dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                                            .id(reqId)
                                            .name(reqName)
                                            .arguments(reqArgs)
                                            .build());
                                }
                            }
                        }

                        dev.langchain4j.data.message.AiMessage.Builder aiBuilder = dev.langchain4j.data.message.AiMessage.builder()
                                .text(aiText)
                                .toolExecutionRequests(requests);
//                        if (aiThinking != null) {
//                            aiBuilder.thinking(aiThinking);
//                        }
                        historyMessages.add(aiBuilder.build());

                        Object resultsObj = toolCallData.get("results");
                        if (resultsObj instanceof List<?> resultsListRaw) {
                            for (Object resItem : resultsListRaw) {
                                if (resItem instanceof Map<?, ?> resMapRaw) {
                                    String toolCallId = toStringOrNull(resMapRaw.get("toolCallId"));
                                    String toolName = toStringOrNull(resMapRaw.get("toolName"));
                                    String toolResult = toStringOrNull(resMapRaw.get("result"));
                                    if (toolCallId != null && toolName != null) {
                                        historyMessages.add(dev.langchain4j.data.message.ToolExecutionResultMessage.from(
                                                toolCallId,
                                                toolName,
                                                "[State_Old]:"+(toolResult != null ? toolResult : "")
                                        ));
                                    }
                                }
                            }
                            continue;
                        }
                    }
                }

                String text = msg.getText();
                String toolName = "UnknownTool";
                String toolResult = "[State_Old]"+text;

                if (text != null && text.contains("#TOOL#")) {
                    String[] parts = text.split("#TOOL#", 2);
                    if (parts.length >= 2) {
                        toolName = parts[0];
                        toolResult = "[State_Old]:"+parts[1];
                    }
                }

                String toolCallId = "unknown_call_id";
                if (toolCallData != null) {
                    if (toolCallData.containsKey("toolCallId")) {
                        Object idObj = toolCallData.get("toolCallId");
                        if (idObj != null) toolCallId = idObj.toString();
                    }

                    if (toolCallData.containsKey("request")) {
                        Object reqObj = toolCallData.get("request");
                        if (reqObj instanceof java.util.Map) {
                            java.util.Map<?, ?> reqMap = (java.util.Map<?, ?>) reqObj;
                            String reqId = (String) reqMap.get("id");
                            String reqName = (String) reqMap.get("name");
                            String reqArgs = (String) reqMap.get("arguments");

                            dev.langchain4j.agent.tool.ToolExecutionRequest request = dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                                    .id(reqId)
                                    .name(reqName)
                                    .arguments(reqArgs)
                                    .build();

                            historyMessages.add(dev.langchain4j.data.message.AiMessage.from(request));

                            if (reqId != null) toolCallId = reqId;
                            if (reqName != null) toolName = reqName;
                        }
                    }
                }

                historyMessages.add(dev.langchain4j.data.message.ToolExecutionResultMessage.from(toolCallId, toolName, toolResult));

            } else {
                String thinking = msg.getToolCallData() != null ? toStringOrNull(msg.getToolCallData().get("thinking")) : null;
                if (thinking != null) {
                    historyMessages.add(dev.langchain4j.data.message.AiMessage.builder()
                            .text(msg.getText())
                            .thinking(thinking)
                            .build());
                } else {
                    historyMessages.add(dev.langchain4j.data.message.AiMessage.from(msg.getText()));
                }
            }
        }
        return historyMessages;
    }

    private static String toStringOrNull(Object value) {
        if (value == null) {
            return null;
        }
        String s = value.toString();
        if (s.isBlank()) {
            return null;
        }
        return s;
    }
}
