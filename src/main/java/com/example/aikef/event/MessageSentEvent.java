package com.example.aikef.event;

import com.example.aikef.model.Message;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MessageSentEvent extends ApplicationEvent {

    private final Message message;

    public MessageSentEvent(Object source, Message message) {
        super(source);
        this.message = message;
    }
}
