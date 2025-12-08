package com.example.aikef.controller;

import com.example.aikef.dto.BootstrapResponse;
import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.service.BootstrapService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bootstrap")
public class BootstrapController {

    private final BootstrapService bootstrapService;

    public BootstrapController(BootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @GetMapping
    public BootstrapResponse load(Authentication authentication) {
        // 获取当前客服ID
        UUID agentId = null;
        if (authentication != null && authentication.getPrincipal() instanceof AgentPrincipal) {
            agentId = ((AgentPrincipal) authentication.getPrincipal()).getId();
        }
        
        if (agentId == null) {
            throw new IllegalStateException("需要客服认证");
        }
        
        return bootstrapService.bootstrap(agentId);
    }
}
