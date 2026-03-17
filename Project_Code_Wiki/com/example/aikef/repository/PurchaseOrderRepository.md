# PurchaseOrderRepository.java

## 1. 类档案 (Class Profile)
- **全限定名**: `com.example.aikef.repository.PurchaseOrderRepository`
- **功能摘要**: 采购单数据访问接口。
- **关键注解**: `@Repository`
- **层级**: Repository Layer

## 2. 核心逻辑详解 (Logic Deep Dive)
1.  **基础查询**: `findByInitiator_Id`, `findBySupplier_Id`, `findByStatus`.
2.  **日期范围查询**:
    -   `findByStatusAndCreatedAtBetween`: 按创建时间查询。
    -   `findByDeliveryDateBetween`: **新增**，按配送日期查询。
    -   `findByDeliveryDateBetweenAndStatus`: **新增**，按配送日期和状态查询。

## 3. 依赖全景 (Dependency Graph)
-   `JpaRepository<PurchaseOrder, String>`

## 4. 调用指南 (Usage Guide)
```java
@Autowired
private PurchaseOrderRepository repo;

LocalDateTime start = LocalDateTime.now().minusDays(1);
LocalDateTime end = LocalDateTime.now();
List<PurchaseOrder> orders = repo.findByDeliveryDateBetween(start, end);
```

## 5. 架构师备注 (Architect's Notes)
-   `deliveryDate` 为 `LocalDateTime`，查询时需注意时间范围构建。
