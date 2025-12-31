package com.example.aikef.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CreateAgentRequest {
    @NotBlank private String name;
    @Email @NotBlank private String email;
    @NotBlank private String password;
    @NotNull private UUID roleId;
    private String language;
    private String tenantId; // SAAS 租户 ID，仅在创建租户管理员时使用

    public CreateAgentRequest() {}

    public CreateAgentRequest(String name, String email, String password, UUID roleId, String language) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.roleId = roleId;
        this.language = language;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    // 兼容 record 的访问方式（如果是 record 调用）
    public String name() { return name; }
    public String email() { return email; }
    public String password() { return password; }
    public UUID roleId() { return roleId; }
    public String language() { return language; }
}
