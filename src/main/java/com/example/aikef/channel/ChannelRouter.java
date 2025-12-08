package com.example.aikef.channel;

import com.example.aikef.dto.ChannelMessage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.aikef.model.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ChannelRouter {

    private static final Logger log = LoggerFactory.getLogger(ChannelRouter.class);

    private final Map<Channel, List<ChannelAdapter>> registry;

    public ChannelRouter(List<ChannelAdapter> adapters) {
        this.registry = adapters.stream()
                .collect(Collectors.groupingBy(ChannelAdapter::Channel,
                        () -> new EnumMap<>(Channel.class),
                        Collectors.toList()));
    }

    public void route(ChannelMessage message) {
        List<ChannelAdapter> adapters = registry.get(message.Channel());
        if (adapters == null) {
            log.warn("未找到渠道 {} 的适配器，direction={}，content={}",
                    message.Channel(), message.direction(), message.content());
            return;
        }
        adapters.stream()
                .filter(adapter -> adapter.supports(message.direction()))
                .findFirst()
                .ifPresentOrElse(adapter -> {
                    log.debug("使用适配器 {} 分发 {} 消息", adapter.getClass().getSimpleName(), message.Channel());
                    adapter.deliver(message);
                }, () -> log.warn("渠道 {} 没有支持 {} 的适配器", message.Channel(), message.direction()));
    }
}
