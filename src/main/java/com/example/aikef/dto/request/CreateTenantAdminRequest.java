package com.example.aikef.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTenantAdminRequest extends CreateAgentRequest {
    private String tenantId;

    public CreateTenantAdminRequest(String name, String email, String password, java.util.UUID roleId, String language, String tenantId) {
        super(name, email, password, roleId, language);
        this.tenantId = tenantId;
    }
}
