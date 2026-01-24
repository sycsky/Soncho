package com.example.aikef.shopify.repository;

import com.example.aikef.shopify.model.ShopifyObject;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShopifyObjectRepository extends JpaRepository<ShopifyObject, UUID> {
    Optional<ShopifyObject> findByShopDomainAndObjectTypeAndExternalId(
            String shopDomain,
            ShopifyObject.ObjectType objectType,
            String externalId
    );

    void deleteByShopDomain(String shopDomain);
}

