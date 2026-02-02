package com.example.aikef.saas.listener;

import com.example.aikef.model.base.AuditableEntity;
import com.example.aikef.saas.context.TenantContext;
import jakarta.persistence.PrePersist;
import org.springframework.stereotype.Component;

@Component
public class TenantEntityListener {

    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof AuditableEntity) {
            AuditableEntity auditable = (AuditableEntity) entity;
            // Only set if not already set (allows manual override)
            if (auditable.getTenantId() == null) {
                String tenantId = TenantContext.getTenantId();
                if (tenantId != null) {
                    auditable.setTenantId(tenantId);
                }
            }
        }
    }
}
