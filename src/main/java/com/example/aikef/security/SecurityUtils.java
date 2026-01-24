package com.example.aikef.security;

import com.example.aikef.model.Agent;
import com.example.aikef.model.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SecurityUtils {

    /**
     * 从 Agent 对象生成 Spring Security 权限列表
     * 包含角色（带 ROLE_ 前缀）和具体权限项
     */
    public static Collection<? extends GrantedAuthority> getAuthorities(Agent agent) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        Role role = agent.getRole();
        if (role != null) {
            // 1. 添加角色权限
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()));
            
            // 2. 添加具体权限项
            Map<String, Object> permissions = role.getPermissions();
            if (permissions != null) {
                for (Map.Entry<String, Object> entry : permissions.entrySet()) {
                    if (entry.getValue() instanceof Boolean && (Boolean) entry.getValue()) {
                        authorities.add(new SimpleGrantedAuthority(entry.getKey()));
                    }
                }
            }
        }
        
        return authorities;
    }
}
