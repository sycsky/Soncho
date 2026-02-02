package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import com.example.aikef.saas.context.TenantContext;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "roles")
@Data
@Filter(name = "tenantFilter", condition = "(tenant_id = :tenantId OR tenant_id IS NULL)")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private UUID id;


    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "is_system", nullable = false)
    private boolean system;

    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    private Map<String, Object> permissions = new HashMap<>();


    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @PrePersist
    protected void onCreate() {
        if (this.tenantId == null) {
            try {
                String currentTenant = TenantContext.getTenantId();
                if (currentTenant != null) {
                    this.tenantId = currentTenant;
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public Map<String, Object> getPermissions() {
        return permissions;
    }

    public void setPermissions(Map<String, Object> permissions) {
        this.permissions = permissions;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
