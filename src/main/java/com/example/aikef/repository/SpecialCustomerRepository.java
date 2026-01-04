package com.example.aikef.repository;

import com.example.aikef.model.SpecialCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpecialCustomerRepository extends JpaRepository<SpecialCustomer, UUID> {
    List<SpecialCustomer> findByRole_Code(String roleCode);
    Optional<SpecialCustomer> findByCustomer_Id(UUID customerId);
}