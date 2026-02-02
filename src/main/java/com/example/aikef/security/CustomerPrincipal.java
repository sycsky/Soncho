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
    private final String tenantId;

    public CustomerPrincipal(UUID id, String name, String channel, String tenantId) {
        this.id = id;
        this.name = name;
        this.channel = channel;
        this.tenantId = tenantId;
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

    public String getTenantId() {
        return tenantId;
    }
}
