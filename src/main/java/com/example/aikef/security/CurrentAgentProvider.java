package com.example.aikef.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentAgentProvider {

    public Optional<AgentPrincipal> currentAgent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AgentPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }
}
