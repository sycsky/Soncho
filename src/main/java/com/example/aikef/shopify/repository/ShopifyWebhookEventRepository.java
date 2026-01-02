package com.example.aikef.shopify.repository;

import com.example.aikef.shopify.model.ShopifyWebhookEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShopifyWebhookEventRepository extends JpaRepository<ShopifyWebhookEvent, UUID> {
    Optional<ShopifyWebhookEvent> findByShopDomainAndWebhookId(String shopDomain, String webhookId);
}
