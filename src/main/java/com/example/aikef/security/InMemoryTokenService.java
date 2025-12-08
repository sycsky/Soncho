package com.example.aikef.security;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的 Token 服务实现（已弃用）
 * 注意：重启服务后 Token 会丢失，建议使用 RedisTokenService
 * 
 * @deprecated 使用 {@link RedisTokenService} 代替
 */
@Deprecated
public class InMemoryTokenService implements TokenService {

    private final Map<String, AgentPrincipal> tokenStore = new ConcurrentHashMap<>();

    @Override
    public String issueToken(AgentPrincipal principal) {
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, principal);
        return token;
    }

    @Override
    public Optional<AgentPrincipal> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(tokenStore.get(token));
    }

    @Override
    public void revoke(String token) {
        if (token != null) {
            tokenStore.remove(token);
        }
    }
}
