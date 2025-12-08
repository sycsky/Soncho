package com.example.aikef.channel;

import com.example.aikef.dto.ChannelMessage;
import com.example.aikef.model.Channel;

public interface ChannelAdapter {

    Channel Channel();

    boolean supports(MessageDirection direction);

    void deliver(ChannelMessage message);
}
