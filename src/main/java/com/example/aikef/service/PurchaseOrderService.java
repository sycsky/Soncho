package com.example.aikef.service;

import com.example.aikef.model.ChatSession;
import com.example.aikef.model.Customer;
import com.example.aikef.model.PurchaseOrder;
import com.example.aikef.model.PurchaseOrderItem;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.repository.CustomerRepository;
import com.example.aikef.repository.PurchaseOrderItemRepository;
import com.example.aikef.repository.PurchaseOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository orderRepository;
    private final PurchaseOrderItemRepository itemRepository;
    private final CustomerRepository customerRepository;
    private final EventService eventService;
    private final ChatSessionRepository chatSessionRepository;

    @Transactional
    public PurchaseOrder createOrder(UUID initiatorId, UUID supplierId, List<PurchaseOrderItem> items) {
        return createOrder(initiatorId, supplierId, items, null);
    }

    @Transactional
    public PurchaseOrder createOrder(UUID initiatorId, UUID supplierId, List<PurchaseOrderItem> items, LocalDateTime deliveryDate) {
        Customer initiator = customerRepository.findById(initiatorId)
                .orElseThrow(() -> new EntityNotFoundException("Initiator not found"));
        Customer supplier = customerRepository.findById(supplierId)
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found"));

        PurchaseOrder order = new PurchaseOrder();
        order.setInitiator(initiator);
        order.setSupplier(supplier);
        order.setStatus("ORDERED");
        order.setDeliveryDate(deliveryDate);
        
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderItem item : items) {
            item.setPurchaseOrder(order);
            if (item.getTotalAmount() != null) {
                total = total.add(item.getTotalAmount());
            }
        }
        order.setItems(items);
        order.setTotalAmount(total);
        order.setPayableAmount(total); // Default
        
        PurchaseOrder saved = orderRepository.save(order);

        // Notify Supplier
        triggerEventForCustomer(supplierId, "EVENT_PO_CREATED", Map.of(
                "orderId", saved.getId(),
                "initiatorName", initiator.getName(),
                "message", "New Purchase Order from " + initiator.getName()
        ));

        return saved;
    }

    public List<PurchaseOrder> getOrdersByInitiator(UUID initiatorId, String status) {
        if (status != null && !status.isBlank()) {
            return orderRepository.findByInitiator_IdAndStatus(initiatorId, status);
        }
        return orderRepository.findByInitiator_Id(initiatorId);
    }

    public List<PurchaseOrder> getOrdersBySupplier(UUID supplierId, String status) {
        if (status != null && !status.isBlank()) {
            return orderRepository.findBySupplier_IdAndStatus(supplierId, status);
        }
        return orderRepository.findBySupplier_Id(supplierId);
    }
    
    public PurchaseOrder getOrderDetails(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
    }

    @Transactional
    public void updateItemShipped(String itemId, int quantity) {
        PurchaseOrderItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Item not found"));
        item.setQuantityShipped(quantity);
        itemRepository.save(item);
    }

    @Transactional
    public void updateItemReceived(String itemId, int quantity) {
        PurchaseOrderItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Item not found"));
        item.setQuantityReceived(quantity);
        itemRepository.save(item);
    }

    @Transactional
    public void updateStatus(String orderId, String newStatus, UUID operatorId) {
        PurchaseOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        String oldStatus = order.getStatus();
        if (oldStatus.equals(newStatus)) return;

        // Status Transition Logic
        // ORDERED -> SHIPPED -> RECEIVED
        
        // 1. Validation: Only SUPPLIER can set to SHIPPED
        if ("SHIPPED".equalsIgnoreCase(newStatus)) {
            if (!order.getSupplier().getId().equals(operatorId)) {
                throw new IllegalArgumentException("Only the SUPPLIER can set status to SHIPPED.");
            }
            if (!"ORDERED".equalsIgnoreCase(oldStatus)) {
                throw new IllegalStateException("Status can only transition to SHIPPED from ORDERED. Current: " + oldStatus);
            }
        }
        
        // 2. Validation: Only INITIATOR can set to RECEIVED
        else if ("RECEIVED".equalsIgnoreCase(newStatus)) {
            if (!order.getInitiator().getId().equals(operatorId)) {
                throw new IllegalArgumentException("Only the INITIATOR (Buyer) can set status to RECEIVED.");
            }
            if (!"SHIPPED".equalsIgnoreCase(oldStatus)) {
                throw new IllegalStateException("Status can only transition to RECEIVED from SHIPPED. Current: " + oldStatus);
            }
        } else {
             throw new IllegalArgumentException("Invalid status transition to: " + newStatus);
        }
        
        order.setStatus(newStatus);
        orderRepository.save(order);

        // Events
        if ("SHIPPED".equalsIgnoreCase(newStatus)) {
            // Notify Initiator
            triggerEventForCustomer(order.getInitiator().getId(), "EVENT_PO_SHIPPED", Map.of(
                    "orderId", order.getId(),
                    "supplierName", order.getSupplier().getName(),
                    "message", "Order #" + order.getId() + " has been SHIPPED."
            ));
        } else if ("RECEIVED".equalsIgnoreCase(newStatus)) {
            // Notify Supplier
            triggerEventForCustomer(order.getSupplier().getId(), "EVENT_PO_RECEIVED", Map.of(
                    "orderId", order.getId(),
                    "initiatorName", order.getInitiator().getName(),
                    "message", "Order #" + order.getId() + " has been RECEIVED."
            ));
            
            // Sync Inventory to Shopify
            syncInventoryToShopify(order);
        }
    }
    
    /**
     * 手动调整库存（增加或回退）
     * @param orderId 订单ID
     * @param isRevert true表示回退（减少库存），false表示增加（增加库存）
     */
    @Transactional
    public void manualInventoryAdjustment(String orderId, boolean isRevert) {
        PurchaseOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        // 复用 syncInventoryToShopify 逻辑，传入 multiplier
        int multiplier = isRevert ? -1 : 1;
        syncInventoryToShopify(order, multiplier);
    }

    @Transactional
    public void updateDeliveryDate(String orderId, LocalDateTime deliveryDate) {
        PurchaseOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        order.setDeliveryDate(deliveryDate);
        orderRepository.save(order);
    }
    
    // 修改原方法，支持 multiplier
    private void syncInventoryToShopify(PurchaseOrder order) {
        syncInventoryToShopify(order, 1);
    }

    private void syncInventoryToShopify(PurchaseOrder order, int multiplier) {
        String apiUrl = "https://47nl4pvyd1.execute-api.us-east-1.amazonaws.com/dev/admin/inventory/adjust";
        
        List<Map<String, Object>> adjustments = new java.util.ArrayList<>();
        for (PurchaseOrderItem item : order.getItems()) {
             if (item.getShopifyVariantId() != null && item.getQuantityReceived() != null && item.getQuantityReceived() > 0) {
                 adjustments.add(Map.of(
                     "variantId", item.getShopifyVariantId(),
                     "delta", item.getQuantityReceived() * multiplier
                 ));
             }
        }
        
        if (adjustments.isEmpty()) {
            log.info("No items to sync to Shopify for Order {}", order.getId());
            return;
        }
        
        try {
            // Use RestTemplate (assuming it's available or create new)
            // Ideally inject RestTemplate
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            
            // Send request for each item? Or batch? 
            // The requirement says: request demo [{ "variantId": "...", "delta": 2 }]
            // It seems the API accepts a list of objects.
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            
            org.springframework.http.HttpEntity<List<Map<String, Object>>> request = new org.springframework.http.HttpEntity<>(adjustments, headers);
            
            restTemplate.postForObject(apiUrl, request, String.class);
            log.info("Successfully synced inventory to Shopify for Order {} (Multiplier: {})", order.getId(), multiplier);
            
        } catch (Exception e) {
            log.error("Failed to sync inventory to Shopify for Order {}", order.getId(), e);
            // Non-blocking failure? Or should we throw?
            // Requirement doesn't specify failure handling, logging is safe.
        }
    }

    public void triggerEventForCustomer(UUID customerId, String eventName, Map<String, Object> data) {
        Runnable task = () -> {
            try {
                ChatSession session = chatSessionRepository.findFirstByCustomer_IdOrderByLastActiveAtDesc(customerId);
                if (session != null) {
                    eventService.triggerEventAsync(eventName, session.getId(), data);
                } else {
                    log.warn("No active session found for customer {}, cannot trigger event {}", customerId, eventName);
                }
            } catch (Exception e) {
                log.error("Failed to trigger event {} for customer {}", eventName, customerId, e);
            }
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }

        task.run();
    }
}
