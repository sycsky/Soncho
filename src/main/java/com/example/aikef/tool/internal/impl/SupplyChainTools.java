package com.example.aikef.tool.internal.impl;

import com.example.aikef.model.PurchaseOrder;
import com.example.aikef.model.PurchaseOrderItem;
import com.example.aikef.model.SpecialCustomer;
import com.example.aikef.service.PurchaseOrderService;
import com.example.aikef.service.SpecialCustomerService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class SupplyChainTools {

    private final SpecialCustomerService specialCustomerService;
    private final PurchaseOrderService purchaseOrderService;

    @Tool("Get list of suppliers")
    public List<SupplierDto> getSupplierList() {
        return specialCustomerService.getCustomersByRole("SUPPLIER").stream()
                .map(sc -> new SupplierDto(
                        sc.getCustomer().getId().toString(),
                        sc.getCustomer().getName(),
                        sc.getCustomer().getEmail()
                ))
                .collect(Collectors.toList());
    }

    @Tool("Create purchase orders (supports multiple suppliers)")
    public String createPurchaseOrder(
            @P(value = "Initiator Customer ID", required = true) String initiatorId,
            @P(value = "List of items to purchase (must include supplierId)", required = true) List<OrderItemRequest> items,
            @P(value = "Delivery date (yyyy-MM-dd HH:mm:ss)", required = true) String deliveryDate
    ) {
        LocalDateTime parsedDeliveryDate = null;
        if (deliveryDate != null && !deliveryDate.isBlank()) {
            parsedDeliveryDate = LocalDateTime.parse(deliveryDate.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        // Group items by supplierId
        java.util.Map<String, List<OrderItemRequest>> itemsBySupplier = items.stream()
                .collect(Collectors.groupingBy(OrderItemRequest::supplierId));
        
        List<String> createdOrderIds = new ArrayList<>();
        
        for (java.util.Map.Entry<String, List<OrderItemRequest>> entry : itemsBySupplier.entrySet()) {
            String supplierId = entry.getKey();
            List<OrderItemRequest> supplierItems = entry.getValue();
            
            if (supplierId == null || supplierId.isBlank()) {
                log.warn("Skipping items with missing supplierId: {}", supplierItems);
                continue;
            }
            
            List<PurchaseOrderItem> orderItems = new ArrayList<>();
            for (OrderItemRequest req : supplierItems) {
                PurchaseOrderItem item = new PurchaseOrderItem();
                item.setProductName(req.productName());
                item.setShopifyVariantId(req.shopifyVariantId());
                item.setQuantityRequested(req.quantity());
                item.setQuantityShipped(0);
                item.setQuantityReceived(0);
                item.setUnitPrice(req.unitPrice() != null ? req.unitPrice() : BigDecimal.ZERO);
                item.setTotalAmount(item.getUnitPrice().multiply(BigDecimal.valueOf(req.quantity())));
                orderItems.add(item);
            }
            
            try {
                PurchaseOrder order = purchaseOrderService.createOrder(
                        UUID.fromString(initiatorId),
                        UUID.fromString(supplierId),
                        orderItems,
                        parsedDeliveryDate
                );
                createdOrderIds.add(order.getId());
                log.info("Created Purchase Order {} for Supplier {}", order.getId(), supplierId);
            } catch (Exception e) {
                log.error("Failed to create order for supplier {}: {}", supplierId, e.getMessage());
                // Optionally throw or continue. Here we continue to try other suppliers.
            }
        }

        String ids ="";
        for (String createdOrderId : createdOrderIds) {
            ids += createdOrderId +",";
        }

        return "进货单创建成功,这是订单编号:"+ids.substring(0,ids.length()-1);
    }

    @Tool("Get my purchase orders (as initiator) with optional status filter")
    public List<PurchaseOrderDto> getMyPurchaseOrders(
            @P(value = "My Customer ID", required = true) String customerId,
            @P(value = "Status (ORDERED, SHIPPED, RECEIVED)", required = false) String status
    ) {
        return purchaseOrderService.getOrdersByInitiator(UUID.fromString(customerId), status).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Tool("Get my supply orders (as supplier) with optional status filter")
    public List<PurchaseOrderDto> getMySupplyOrders(
            @P(value = "My Customer ID", required = true) String customerId,
            @P(value = "Status (ORDERED, SHIPPED, RECEIVED)", required = false) String status
    ) {
        return purchaseOrderService.getOrdersBySupplier(UUID.fromString(customerId), status).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Tool("Get all purchase orders (Admin view) with optional status filter")
    public List<PurchaseOrderDto> getAllPurchaseOrders(
            @P(value = "Status (ORDERED, SHIPPED, RECEIVED, CANCELLED)", required = false) String status
    ) {
        return purchaseOrderService.getAllOrders(status).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Tool("Get all purchase orders details (Admin view) with items")
    public List<PurchaseOrderDetailDto> getAllPurchaseOrderDetails(
            @P(value = "Status (ORDERED, SHIPPED, RECEIVED, CANCELLED)", required = false) String status
    ) {
        return purchaseOrderService.getAllOrders(status).stream()
                .map(this::toDetailDto)
                .collect(Collectors.toList());
    }

    @Tool("Get supply order details including items")
    public PurchaseOrderDetailDto getSupplyOrderDetails(
            @P(value = "Order ID", required = true) String orderId
    ) {
        PurchaseOrder order = purchaseOrderService.getOrderDetails(orderId);
        return toDetailDto(order);
    }


    @Tool("Get purchase order details including items")
    public PurchaseOrderDetailDto getPurchaseOrderDetails(
            @P(value = "Order ID", required = true) String orderId
    ) {
        PurchaseOrder order = purchaseOrderService.getOrderDetails(orderId);
        return toDetailDto(order);
    }

    @Tool("Update shipped quantity for items (Supplier)")
    public String updatePurchaseOrderShipped(
            @P(value = "List of item updates", required = true) List<ItemUpdate> updates
    ) {
        log.info("Updating shipped quantities for items: {}", updates);
        for (ItemUpdate update : updates) {
            purchaseOrderService.updateItemShipped(update.itemId(), update.quantity());
        }
        return "Updated shipped quantities.";


    }

    @Tool("Update received quantity for items (Initiator)")
    public String updatePurchaseOrderReceived(
            @P(value = "List of item updates", required = true) List<ItemUpdate> updates
    ) {
        for (ItemUpdate update : updates) {
            purchaseOrderService.updateItemReceived(update.itemId(), update.quantity());
        }
        return "Updated received quantities.";
    }

    @Tool("Update purchase order status")
    public String updatePurchaseOrderStatus(
            @P(value = "Order ID", required = true) String orderId,
            @P(value = "New Status (SHIPPED, RECEIVED)", required = true) String status,
            @P(value = "Operator ID", required = true) String operatorId
    ) {
        purchaseOrderService.updateStatus(orderId, status, UUID.fromString(operatorId));
        return "Order status updated to " + status;
    }

    @Tool("Update purchase order delivery date")
    public String updatePurchaseOrderDeliveryDate(
            @P(value = "Order ID", required = true) String orderId,
            @P(value = "Delivery date (yyyy-MM-dd HH:mm:ss)", required = true) String deliveryDate
    ) {
        purchaseOrderService.updateDeliveryDate(orderId, LocalDateTime.parse(deliveryDate.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return "Delivery date updated to " + deliveryDate;
    }

    @Tool("Cancel purchase order")
    public String cancelPurchaseOrder(
            @P(value = "Order ID", required = true) String orderId,
            @P(value = "Cancellation Reason", required = false) String reason,
            @P(value = "Operator Customer ID", required = true) String operatorId
    ) {
        purchaseOrderService.cancelOrder(orderId, reason, UUID.fromString(operatorId));
        return "Order " + orderId + " has been cancelled.";
    }

    @Tool("Manually adjust inventory for purchase order (Sync or Revert)")
    public String adjustInventoryForOrder(
            @P(value = "Order ID", required = true) String orderId,
            @P(value = "Action Flag (SYNC: Add inventory, REVERT: Subtract inventory)", required = true) String flag
    ) {
        boolean isRevert;
        if ("REVERT".equalsIgnoreCase(flag)) {
            isRevert = true;
        } else if ("SYNC".equalsIgnoreCase(flag)) {
            isRevert = false;
        } else {
            return "Error: Invalid flag. Use 'SYNC' or 'REVERT'.";
        }

        try {
            purchaseOrderService.manualInventoryAdjustment(orderId, isRevert);
            return isRevert ? "Successfully reverted inventory." : "Successfully synced inventory.";
        } catch (Exception e) {
            return "Error adjusting inventory: " + e.getMessage();
        }
    }

    @Tool("Get all suppliers settlement statistics (Admin)")
    public SupplierSettlementDto getAllSettlementStats(
            @P(value = "Start Date (yyyy-MM-dd)", required = true) String startDate,
            @P(value = "End Date (yyyy-MM-dd)", required = true) String endDate,
            @P(value = "Status (ORDERED, SHIPPED, RECEIVED)", required = true) String status
    ) {
        // Parse dates to Instant (assuming start of day for start date, end of day for end date in system zone)
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        java.time.Instant startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
        java.time.Instant endInstant = end.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        // Only include orders with specified status
        List<PurchaseOrder> orders = purchaseOrderService.getOrdersByStatusAndDateRange(status, startInstant, endInstant);

        return calculateSettlement(orders);
    }

    @Tool("Get supplier settlement statistics")
    public SupplierSettlementDto getSupplierSettlementStats(
            @P(value = "Supplier Customer ID", required = true) String supplierId,
            @P(value = "Start Date (yyyy-MM-dd)", required = true) String startDate,
            @P(value = "End Date (yyyy-MM-dd)", required = true) String endDate,
            @P(value = "Status (ORDERED, SHIPPED, RECEIVED)", required = true) String status
    ) {
        // Parse dates to Instant (assuming start of day for start date, end of day for end date in system zone)
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        
        java.time.Instant startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
        java.time.Instant endInstant = end.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        
        // Only include orders with specified status
        List<PurchaseOrder> orders = purchaseOrderService.getOrdersBySupplierAndStatusAndDateRange(UUID.fromString(supplierId), status, startInstant, endInstant);
        
        return calculateSettlement(orders);
    }

    private SupplierSettlementDto calculateSettlement(List<PurchaseOrder> orders) {
        int orderCount = orders.size();
        int totalQuantityRequested = 0;
        int totalQuantityShipped = 0;
        int totalQuantityReceived = 0;
        BigDecimal totalReceivedAmount = BigDecimal.ZERO;

        List<SettlementItemDto> items = new ArrayList<>();
        List<String> returnOrderIds = new ArrayList<>();

        for (PurchaseOrder order : orders) {
            boolean hasReturnInOrder = false;

            for (PurchaseOrderItem item : order.getItems()) {
                int requested = item.getQuantityRequested() != null ? item.getQuantityRequested() : 0;
                int shipped = item.getQuantityShipped() != null ? item.getQuantityShipped() : 0;
                int received = item.getQuantityReceived() != null ? item.getQuantityReceived() : 0;
                int returned = Math.max(0, requested - received);

                // Check if this item has return (received != shipped) as per user requirement: "清单item实收数!=实发数"
                if (received != shipped) {
                    hasReturnInOrder = true;
                }

                totalQuantityRequested += requested;
                totalQuantityShipped += shipped;
                totalQuantityReceived += received;

                // Calculate amount for received items
                BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(received));
                totalReceivedAmount = totalReceivedAmount.add(itemTotal);

                items.add(new SettlementItemDto(
                        item.getProductName(),
                        order.getSupplier().getName(),
                        requested,
                        shipped,
                        received,
                        returned,
                        item.getUnitPrice()
                ));
            }

            if (hasReturnInOrder) {
                returnOrderIds.add(order.getId());
            }
        }

        BigDecimal taxRate = new BigDecimal("0.10");
        BigDecimal taxAmount = totalReceivedAmount.multiply(taxRate);
        BigDecimal totalBillAmount = totalReceivedAmount.add(taxAmount);

        return new SupplierSettlementDto(
                orderCount,
                totalQuantityRequested,
                totalQuantityShipped,
                totalQuantityReceived,
                totalReceivedAmount,
                taxAmount,
                totalBillAmount,
                items,
                returnOrderIds
        );
    }

    private PurchaseOrderDetailDto toDetailDto(PurchaseOrder order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(item -> new OrderItemDto(
                        item.getId().toString(),
                        item.getProductName(),
                        order.getSupplier().getName(),
                        item.getQuantityRequested(),
                        item.getQuantityShipped(),
                        item.getQuantityReceived(),
                        item.getUnitPrice(),
                        item.getTotalAmount(),
                        (item.getQuantityRequested() == null ? 0 : item.getQuantityRequested()) > (item.getQuantityReceived() == null ? 0 : item.getQuantityReceived()) ? "YES" : "NO"
                ))
                .collect(Collectors.toList());

        return new PurchaseOrderDetailDto(
                order.getId(),
                order.getStatus(),
                formatStatusInChinese(order.getStatus()),
                order.getTotalAmount(),
                order.getInitiator().getName(),
                order.getSupplier().getName(),
                order.getDeliveryDate() != null ? order.getDeliveryDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "",
                items
        );
    }

    // DTOs
    public record SupplierDto(String id, String name, String email) {}
    public record OrderItemRequest(String supplierId, String productName, String shopifyVariantId, int quantity, BigDecimal unitPrice) {}
    public record ItemUpdate(String itemId, int quantity) {}
    public record PurchaseOrderDto(String id, String status, String statusDescription, BigDecimal totalAmount, String supplierName, String createdAt, String deliveryDate) {}
    
    public record SupplierSettlementDto(
            int orderCount,
            int totalQuantityRequested,
            int totalQuantityShipped,
            int totalQuantityReceived,
            BigDecimal totalReceivedAmount,
            BigDecimal taxAmount,
            BigDecimal totalBillAmount,
            List<SettlementItemDto> items,
            List<String> returnOrderIds
    ) {}
    
    public record SettlementItemDto(
            String productName,
            String supplierName,
            int quantityRequested,
            int quantityShipped,
            int quantityReceived,
            int quantityReturned,
            BigDecimal unitPrice
    ) {}
    
    public record PurchaseOrderDetailDto(
        String id, 
        String status, 
        String statusDescription,
        BigDecimal totalAmount, 
        String initiatorName, 
        String supplierName,
        String deliveryDate,
        List<OrderItemDto> items
    ) {}

    public record OrderItemDto(
        String id,
        String productName,
        String supplierName,
        int quantityRequested,
        int quantityShipped,
        int quantityReceived,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        String hasReturn
    ) {}

    private PurchaseOrderDto toDto(PurchaseOrder po) {
        String formattedDate = "";
        if (po.getCreatedAt() != null) {
            formattedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    .withZone(ZoneId.systemDefault())
                    .format(po.getCreatedAt());
        }
        String formattedDeliveryDate = "";
        if (po.getDeliveryDate() != null) {
            formattedDeliveryDate = po.getDeliveryDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return new PurchaseOrderDto(
                po.getId(), 
                po.getStatus(), 
                formatStatusInChinese(po.getStatus()), 
                po.getTotalAmount(), 
                po.getSupplier().getName(),
                formattedDate,
                formattedDeliveryDate
        );
    }

    private String formatStatusInChinese(String status) {
        if (status == null) return "";
        return switch (status.toUpperCase()) {
            case "ORDERED" -> "已下单";
            case "SHIPPED" -> "已发货";
            case "RECEIVED" -> "已收货";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }
}
