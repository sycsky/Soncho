package com.example.aikef.service;

import com.example.aikef.dto.ChannelMessage;
import com.example.aikef.dto.ChatResponse;

public interface AiAssistantService {

    ChatResponse reply(ChannelMessage chatMessage);
}
