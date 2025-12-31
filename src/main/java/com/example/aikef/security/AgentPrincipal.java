package com.example.aikef.security;

import com.example.aikef.model.Agent;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AgentPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final String name;
    private final String tenantId;
    private final Collection<? extends GrantedAuthority> authorities;

    public AgentPrincipal(Agent agent, Collection<? extends GrantedAuthority> authorities) {
        this.id = agent.getId();
        this.email = agent.getEmail();
        this.password = agent.getPasswordHash();
        this.name = agent.getName();
        this.tenantId = agent.getTenantId();
        this.authorities = authorities == null ? Collections.emptyList() : authorities;
    }

    public UUID getId() {
        return id;
    }
    
    public String getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
