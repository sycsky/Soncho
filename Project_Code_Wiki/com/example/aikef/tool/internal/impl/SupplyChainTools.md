# SupplyChainTools.java

## 1. 类档案 (Class Profile)
- **全限定名**: `com.example.aikef.tool.internal.impl.SupplyChainTools`
- **功能摘要**: 提供供应链相关的工具方法，包括创建采购单、查询采购单、更新物流状态、库存调整等。
- **关键注解**: `@Component`, `@Transactional(propagation = Propagation.REQUIRES_NEW)`
- **层级**: Tool Implementation Layer

## 2. 核心逻辑详解 (Logic Deep Dive)
1.  **采购单创建 (`createPurchaseOrder`)**:
    -   接收发起人ID、商品列表和配送日期。
    -   按供应商分组商品。
    -   调用 `PurchaseOrderService.createOrder` 为每个供应商创建采购单。
    -   返回创建的订单ID列表。

2.  **采购单查询**:
    -   提供多种维度的查询：按发起人 (`getMyPurchaseOrders`)、按供应商 (`getMySupplyOrders`)、管理员全量查询 (`getAllPurchaseOrders`)。
    -   支持按状态过滤。
    -   **新增**: 支持按配送日期查询 (`getPurchaseOrdersByDeliveryDate`)。

3.  **状态流转**:
    -   `updatePurchaseOrderStatus`: 处理订单状态变更 (ORDERED -> SHIPPED -> RECEIVED)。
    -   包含权限校验（供应商发货，买家收货）和事件触发。

4.  **库存与结算**:
    -   `adjustInventoryForOrder`: 手动同步或回退库存到 Shopify。
    -   `getAllSettlementStats`: 计算供应商结算数据。

## 3. 依赖全景 (Dependency Graph)
- **Injected Services**:
    -   `SpecialCustomerService`: 获取供应商列表。
    -   `PurchaseOrderService`: 核心业务逻辑处理。
- **Data Models**:
    -   `PurchaseOrder`, `PurchaseOrderItem`, `SpecialCustomer`

## 4. 调用指南 (Usage Guide)
```java
@Autowired
private SupplyChainTools supplyChainTools;

// Example 1: Get orders by delivery date
String deliveryDate = "2023-10-27";
String status = "ORDERED";
List<PurchaseOrderDetailDto> orders = supplyChainTools.getPurchaseOrdersByDeliveryDate(deliveryDate, status);

// Example 2: Create purchase order
List<OrderItemRequest> items = new ArrayList<>();
items.add(new OrderItemRequest("supplier-uuid", "Product A", "variant-123", BigDecimal.TEN, BigDecimal.ONE));
String result = supplyChainTools.createPurchaseOrder("initiator-uuid", items, "2023-10-30 10:00:00");
```

## 5. 架构师备注 (Architect's Notes)
-   **事务传播**: 使用 `REQUIRES_NEW` 确保工具执行在独立事务中，避免长事务阻塞。
-   **日期处理**: 输入日期字符串统一解析为 `LocalDateTime` 或 `LocalDate`，需注意时区一致性（目前默认系统时区）。
-   **扩展性**: 新增的按配送日查询功能支持管理员进行物流调度规划。
