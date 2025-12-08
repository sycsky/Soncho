package com.example.aikef.security;

import java.util.Optional;

public interface TokenService {

    String issueToken(AgentPrincipal principal);

    Optional<AgentPrincipal> resolve(String token);

    void revoke(String token);
}
