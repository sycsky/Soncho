# PurchaseOrderService.java

## 1. 类档案 (Class Profile)
- **全限定名**: `com.example.aikef.service.PurchaseOrderService`
- **功能摘要**: 采购单业务逻辑处理，包括创建、查询、状态更新、库存同步等。
- **关键注解**: `@Service`, `@Transactional`
- **层级**: Service Layer

## 2. 核心逻辑详解 (Logic Deep Dive)
1.  **创建订单 (`createOrder`)**:
    -   验证发起人和供应商。
    -   初始化订单状态为 `ORDERED`。
    -   保存订单并触发供应商通知事件。

2.  **查询订单**:
    -   `getOrdersByInitiator`, `getOrdersBySupplier`, `getAllOrders`。
    -   `getOrdersByDeliveryDate`: **新增**，按配送日期查询订单。

3.  **状态流转 (`updateStatus`)**:
    -   验证状态流转合法性 (ORDERED -> SHIPPED -> RECEIVED)。
    -   验证操作人权限。
    -   更新状态并触发相应事件（发货通知、收货通知）。
    -   收货时自动同步库存到 Shopify。

## 3. 依赖全景 (Dependency Graph)
- **Repositories**: `PurchaseOrderRepository`, `PurchaseOrderItemRepository`, `CustomerRepository`, `ChatSessionRepository`
- **Services**: `EventService`

## 4. 调用指南 (Usage Guide)
```java
@Autowired
private PurchaseOrderService purchaseOrderService;

// Query by delivery date
LocalDate date = LocalDate.of(2023, 10, 27);
List<PurchaseOrder> orders = purchaseOrderService.getOrdersByDeliveryDate(date, "ORDERED");
```

## 5. 架构师备注 (Architect's Notes)
-   **新增查询**: `getOrdersByDeliveryDate` 使用 `startOfDay` 和 `endOfDay` 构建时间范围查询，确保覆盖全天。
