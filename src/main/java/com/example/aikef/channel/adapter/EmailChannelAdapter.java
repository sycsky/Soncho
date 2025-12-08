package com.example.aikef.channel.adapter;

import com.example.aikef.channel.ChannelAdapter;
import com.example.aikef.model.Channel;
import com.example.aikef.channel.MessageDirection;
import com.example.aikef.dto.ChannelMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailChannelAdapter implements ChannelAdapter {

    private static final Logger log = LoggerFactory.getLogger(EmailChannelAdapter.class);

    @Override
    public Channel Channel() {
        return Channel.EMAIL;
    }

    @Override
    public boolean supports(MessageDirection direction) {
        return direction == MessageDirection.OUTBOUND;
    }

    @Override
    public void deliver(ChannelMessage message) {
        log.info("[Email] 发送给 {}, 主题: 会话{}，正文: {}", message.recipientId(), message.conversationId(), message.content());
    }
}
