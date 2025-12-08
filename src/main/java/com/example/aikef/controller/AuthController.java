package com.example.aikef.controller;

import com.example.aikef.dto.AgentDto;
import com.example.aikef.dto.request.LoginRequest;
import com.example.aikef.dto.response.LoginResponse;
import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.security.TokenService;
import com.example.aikef.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final AgentService agentService;

    public AuthController(AuthenticationManager authenticationManager,
                          TokenService tokenService,
                          AgentService agentService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.agentService = agentService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        AgentPrincipal principal = (AgentPrincipal) authentication.getPrincipal();
        String token = tokenService.issueToken(principal);
        return new LoginResponse(token, agentService.getAgent(principal.getId()));
    }

    @GetMapping("/me")
    public AgentDto me() {
        return agentService.currentAgent();
    }
}
