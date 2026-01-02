package com.example.aikef.shopify.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "shopify_stores")
@Getter
@Setter
public class ShopifyStore extends AuditableEntity {

    @Column(name = "shop_domain", nullable = false, unique = true, length = 255)
    private String shopDomain;

    @Column(name = "access_token", nullable = false, length = 500)
    private String accessToken;

    @Column(name = "scopes", length = 1000)
    private String scopes;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "installed_at")
    private Instant installedAt;

    @Column(name = "uninstalled_at")
    private Instant uninstalledAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;
}

