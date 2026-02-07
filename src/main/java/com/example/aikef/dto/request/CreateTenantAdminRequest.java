package com.example.aikef.dto.request;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTenantAdminRequest extends CreateAgentRequest {

    public CreateTenantAdminRequest(String name, String email, String password, UUID roleId, String language, String tenantId) {
        super(name, email, password, roleId, language, tenantId);
    }
}

