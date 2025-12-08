package com.example.aikef.channel.adapter;

import com.example.aikef.channel.ChannelAdapter;
import com.example.aikef.model.Channel;
import com.example.aikef.channel.MessageDirection;
import com.example.aikef.dto.ChannelMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WhatsappChannelAdapter implements ChannelAdapter {

    private static final Logger log = LoggerFactory.getLogger(WhatsappChannelAdapter.class);

    @Override
    public Channel Channel() {
        return Channel.WHATSAPP;
    }

    @Override
    public boolean supports(MessageDirection direction) {
        return direction == MessageDirection.OUTBOUND;
    }

    @Override
    public void deliver(ChannelMessage message) {
        log.info("[WhatsApp] 转发对话 {} 给 {}, 内容: {}", message.conversationId(), message.recipientId(), message.content());
    }
}
