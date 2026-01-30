package com.example.aikef.constant;

public final class ShopifyEventConstants {

    private ShopifyEventConstants() {
        // Prevent instantiation
    }

    public static final String ORDER_CREATED = "shopify.orders.create";
    public static final String ORDER_UPDATED = "shopify.orders.updated";
    public static final String ORDER_CANCELLED = "shopify.orders.cancelled";
    
    public static final String CUSTOMER_CREATED = "shopify.customers.create";
    public static final String CUSTOMER_UPDATED = "shopify.customers.update";
    
    public static final String PRODUCT_CREATED = "shopify.products.create";
    public static final String PRODUCT_UPDATED = "shopify.products.update";
    
    public static final String INVENTORY_LEVEL_UPDATED = "shopify.inventory_levels.update";
    
    public static final String REFUND_CREATED = "shopify.refunds.create";
    
    public static final String FULFILLMENT_CREATED = "shopify.fulfillments.create";
    public static final String FULFILLMENT_UPDATED = "shopify.fulfillments.update";
    public static final String FULFILLMENT_EVENT_CREATED = "shopify.fulfillment_events.create";
    
    public static final String CHECKOUT_CREATED = "shopify.checkouts.create";
    public static final String CHECKOUT_UPDATED = "shopify.checkouts.update";
    public static final String CHECKOUT_DELETED = "shopify.checkouts.delete";
    
    public static final String DRAFT_ORDER_CREATED = "shopify.draft_orders.create";
    public static final String DRAFT_ORDER_UPDATED = "shopify.draft_orders.update";
    
    public static final String COLLECTION_CREATED = "shopify.collections.create";
    public static final String COLLECTION_UPDATED = "shopify.collections.update";
    
    public static final String THEME_PUBLISH = "shopify.themes.publish";
    
    public static final String APP_UNINSTALLED = "shopify.app.uninstalled";
}
