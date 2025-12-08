package com.example.aikef.security;

import java.security.Principal;
import java.util.UUID;

/**
 * 客户身份信息
 */
public class CustomerPrincipal implements Principal {

    private final UUID id;
    private final String name;
    private final String channel;

    public CustomerPrincipal(UUID id, String name, String channel) {
        this.id = id;
        this.name = name;
        this.channel = channel;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getChannel() {
        return channel;
    }
}
