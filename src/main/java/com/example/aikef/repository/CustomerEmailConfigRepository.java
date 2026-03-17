package com.example.aikef.repository;

import com.example.aikef.model.CustomerEmailConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerEmailConfigRepository extends JpaRepository<CustomerEmailConfig, UUID> {
    List<CustomerEmailConfig> findByCustomerId(UUID customerId);
    Optional<CustomerEmailConfig> findByCustomerIdAndEmail(UUID customerId, String email);
    List<CustomerEmailConfig> findByEnabledTrue();
}
