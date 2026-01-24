package com.example.aikef.security;

import com.example.aikef.model.Agent;
import com.example.aikef.repository.AgentRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AgentAuthenticationProvider implements AuthenticationProvider {

    private final AgentRepository agentRepository;
    private final PasswordEncoder passwordEncoder;

    public AgentAuthenticationProvider(AgentRepository agentRepository, PasswordEncoder passwordEncoder) {
        this.agentRepository = agentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String rawPassword = authentication.getCredentials() != null ? authentication.getCredentials().toString() : "";
        Agent agent = agentRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("账号或密码错误"));
        if (!passwordEncoder.matches(rawPassword, agent.getPasswordHash())) {
            throw new BadCredentialsException("账号或密码错误");
        }
        AgentPrincipal principal = new AgentPrincipal(agent, SecurityUtils.getAuthorities(agent));
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
