package com.example.aikef.shopify.repository;

import com.example.aikef.shopify.model.ShopifyStore;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShopifyStoreRepository extends JpaRepository<ShopifyStore, UUID> {
    Optional<ShopifyStore> findByShopDomain(String shopDomain);
}

