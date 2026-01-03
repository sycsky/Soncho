package com.example.aikef.repository;

import com.example.aikef.model.CustomerRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRoleRepository extends JpaRepository<CustomerRole, UUID> {
    Optional<CustomerRole> findByCode(String code);
}
