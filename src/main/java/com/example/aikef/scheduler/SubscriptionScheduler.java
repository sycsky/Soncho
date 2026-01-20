package com.example.aikef.scheduler;

import com.example.aikef.shopify.model.ShopifyStore;
import com.example.aikef.shopify.repository.ShopifyStoreRepository;
import com.example.aikef.shopify.service.ShopifyBillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionScheduler {

    private final ShopifyStoreRepository storeRepository;
    private final ShopifyBillingService billingService;

    /**
     * Daily sync of Shopify subscriptions to:
     * 1. Detect renewals (and reset usage limits)
     * 2. Detect cancellations (and expire/downgrade to Free)
     * 
     * Runs every day at 01:00 AM server time.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void syncSubscriptions() {
        log.info("Starting daily subscription sync...");
        List<ShopifyStore> stores = storeRepository.findAll();
        for (ShopifyStore store : stores) {
            try {
                billingService.syncSubscription(store.getShopDomain());
            } catch (Exception e) {
                log.error("Failed to sync subscription for shop: {}", store.getShopDomain(), e);
            }
        }
        log.info("Daily subscription sync completed.");
    }
}
