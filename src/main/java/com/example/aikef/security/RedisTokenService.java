package com.example.aikef.security;

import com.example.aikef.model.Agent;
import com.example.aikef.repository.AgentRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * 基于 Redis 的 Token 服务实现
 * 将客服 Token 存储在 Redis 中，支持服务重启后 Token 仍然有效
 */
@Component
public class RedisTokenService implements TokenService {

    private static final Duration DEFAULT_TTL = Duration.ofDays(30);
    private static final String KEY_PREFIX = "agent_token:";

    private final StringRedisTemplate redisTemplate;
    private final AgentRepository agentRepository;

    public RedisTokenService(StringRedisTemplate redisTemplate, AgentRepository agentRepository) {
        this.redisTemplate = redisTemplate;
        this.agentRepository = agentRepository;
    }

    @Override
    public String issueToken(AgentPrincipal principal) {
        String token = UUID.randomUUID().toString();
        String key = KEY_PREFIX + token;
        // 存储 agentId
        redisTemplate.opsForValue().set(key, principal.getId().toString(), DEFAULT_TTL);
        return token;
    }

    @Override
    public Optional<AgentPrincipal> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        
        String key = KEY_PREFIX + token;
        String agentIdStr = redisTemplate.opsForValue().get(key);
        
        if (agentIdStr == null || agentIdStr.isBlank()) {
            return Optional.empty();
        }
        
        try {
            UUID agentId = UUID.fromString(agentIdStr);
            // 使用 findByIdWithRole 预加载 Role，避免懒加载异常
            return agentRepository.findByIdWithRole(agentId)
                    .map(this::createPrincipal);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public void revoke(String token) {
        if (token != null) {
            String key = KEY_PREFIX + token;
            redisTemplate.delete(key);
        }
    }

    /**
     * 刷新 Token 过期时间
     */
    public void refreshToken(String token) {
        if (token != null) {
            String key = KEY_PREFIX + token;
            redisTemplate.expire(key, DEFAULT_TTL);
        }
    }

    /**
     * 从 Agent 创建 AgentPrincipal
     */
    private AgentPrincipal createPrincipal(Agent agent) {
        // 获取角色权限
        var authorities = agent.getRole() != null 
                ? Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + agent.getRole().getName().toUpperCase()))
                : Collections.<SimpleGrantedAuthority>emptyList();
        
        return new AgentPrincipal(agent, authorities);
    }
}

