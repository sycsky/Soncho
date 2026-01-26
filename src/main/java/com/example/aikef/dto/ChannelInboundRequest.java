package com.example.aikef.dto;

import com.example.aikef.model.Channel;
import com.example.aikef.channel.MessageDirection;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public record ChannelInboundRequest(
        @NotBlank String conversationId,
        @NotBlank String senderId,
        String content,
        Map<String, Object> metadata,
        List<String> mentions) {

    public ChannelMessage toChannelMessage(Channel Channel) {
        return new ChannelMessage(Channel, MessageDirection.INBOUND, conversationId, senderId, null, content, metadata, mentions);
    }
}
