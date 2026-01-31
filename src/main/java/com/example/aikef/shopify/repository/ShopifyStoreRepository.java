package com.example.aikef.shopify.repository;

import com.example.aikef.shopify.model.ShopifyStore;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ShopifyStoreRepository extends JpaRepository<ShopifyStore, UUID> {
    Optional<ShopifyStore> findByShopDomain(String shopDomain);

    @Modifying
    @Query("UPDATE ShopifyStore s SET s.lastSyncedAt = :lastSyncedAt WHERE s.shopDomain = :shopDomain")
    void updateLastSyncedAt(String shopDomain, Instant lastSyncedAt);
}

