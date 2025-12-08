package com.example.aikef.service.impl;

import com.example.aikef.dto.ChannelMessage;
import com.example.aikef.dto.ChatResponse;
import com.example.aikef.service.AiAssistantService;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class SimpleAiAssistantService implements AiAssistantService {

    @Override
    public ChatResponse reply(ChannelMessage chatMessage) {
        String reply = generateReply(chatMessage.content());
        return new ChatResponse(
                chatMessage.Channel(),
                chatMessage.conversationId(),
                chatMessage.senderId(),
                reply,
                Instant.now());
    }

    private String generateReply(String content) {
        String normalized = content.toLowerCase();
        if (normalized.contains("退货") || normalized.contains("售后")) {
            return "可以为您安排售后处理，请提供订单号和问题描述";
        }
        if (normalized.contains("价格") || normalized.contains("优惠")) {
            return "当前正在进行限时优惠，下单即可自动享受折扣";
        }
        if (normalized.contains("发货") || normalized.contains("物流")) {
            return "系统显示订单已打包，预计24小时内发出，发货后会推送物流单号";
        }
        return "您好，我是AI客服小智，已收到您的消息，我们会尽快给到更详细的解决方案";
    }
}
