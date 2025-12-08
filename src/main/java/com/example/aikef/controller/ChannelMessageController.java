package com.example.aikef.controller;

import com.example.aikef.channel.ChannelRouter;
import com.example.aikef.model.Channel;
import com.example.aikef.dto.ChannelInboundRequest;
import com.example.aikef.dto.ChannelMessage;
import com.example.aikef.dto.ChatResponse;
import com.example.aikef.service.AiAssistantService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/channels")
public class ChannelMessageController {

    private final AiAssistantService aiAssistantService;
    private final ChannelRouter channelRouter;

    public ChannelMessageController(AiAssistantService aiAssistantService, ChannelRouter channelRouter) {
        this.aiAssistantService = aiAssistantService;
        this.channelRouter = channelRouter;
    }

    @PostMapping("/{channel}/messages")
    public ChatResponse receive(@PathVariable("channel") Channel Channel,
                                @Valid @RequestBody ChannelInboundRequest request) {
        ChannelMessage inboundMessage = request.toChannelMessage(Channel);
        ChatResponse response = aiAssistantService.reply(inboundMessage);
        ChannelMessage outbound = ChannelMessage.outbound(
                response.Channel(),
                response.conversationId(),
                "AI客服",
                inboundMessage.senderId(),
                response.reply(),
                Map.of("source", "ai-service"));
        channelRouter.route(outbound);
        return response;
    }
}
