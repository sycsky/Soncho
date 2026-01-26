package com.example.aikef.dto;

import com.example.aikef.model.Channel;
import com.example.aikef.channel.MessageDirection;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record ChannelMessage(
        Channel Channel,
        MessageDirection direction,
        String conversationId,
        String senderId,
        String recipientId,
        String content,
        Map<String, Object> metadata,
        List<String> mentions) {

    public ChannelMessage {
        if (Channel == null) {
            Channel = Channel.WEB;
        }
        if (direction == null) {
            direction = MessageDirection.INBOUND;
        }
        if (senderId == null || senderId.isBlank()) {
            senderId = "anonymous";
        }
        // 允许 content 为空 (例如纯附件消息)
        if (content == null) {
            content = "";
        }
        content = content.trim();
        metadata = metadata == null ? Collections.emptyMap() : Map.copyOf(metadata);
        mentions = mentions == null ? List.of() : List.copyOf(mentions);
    }

    public static ChannelMessage inbound(Channel Channel,
                                          String conversationId,
                                          String senderId,
                                          String content,
                                          Map<String, Object> metadata) {
        return new ChannelMessage(Channel, MessageDirection.INBOUND, conversationId, senderId, null, content, metadata, List.of());
    }

    public static ChannelMessage outbound(Channel Channel,
                                          String conversationId,
                                          String senderId,
                                          String recipientId,
                                          String content,
                                          Map<String, Object> metadata) {
        return new ChannelMessage(Channel, MessageDirection.OUTBOUND, conversationId, senderId, recipientId, content, metadata, List.of());
    }
}
