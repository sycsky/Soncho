package com.example.aikef.repository;

import com.example.aikef.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, String> {
    List<PurchaseOrder> findByInitiator_Id(UUID initiatorId);
    List<PurchaseOrder> findBySupplier_Id(UUID supplierId);
    
    // Support status filtering
    List<PurchaseOrder> findByInitiator_IdAndStatus(UUID initiatorId, String status);
    List<PurchaseOrder> findBySupplier_IdAndStatus(UUID supplierId, String status);
    
    // Find all by status
    List<PurchaseOrder> findByStatus(String status);
}
