package com.example.aikef.service;

import com.example.aikef.model.Customer;
import com.example.aikef.model.CustomerRole;
import com.example.aikef.model.SpecialCustomer;
import com.example.aikef.repository.CustomerRepository;
import com.example.aikef.repository.CustomerRoleRepository;
import com.example.aikef.repository.SpecialCustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SpecialCustomerService {

    private final SpecialCustomerRepository specialCustomerRepository;
    private final CustomerRoleRepository customerRoleRepository;
    private final CustomerRepository customerRepository;

    public List<SpecialCustomer> getCustomersByRole(String roleCode) {
        return specialCustomerRepository.findByRole_Code(roleCode);
    }

    @Transactional
    public SpecialCustomer assignRole(UUID customerId, String roleCode) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));

        CustomerRole role = customerRoleRepository.findByCode(roleCode)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleCode));

        // Check if already assigned
        SpecialCustomer existing = specialCustomerRepository.findByCustomer_Id(customerId).orElse(null);
        if (existing != null) {
            existing.setRole(role);
            return specialCustomerRepository.save(existing);
        }

        SpecialCustomer special = new SpecialCustomer();
        special.setCustomer(customer);
        special.setRole(role);
        return specialCustomerRepository.save(special);
    }

    // Role Management

    public List<CustomerRole> getAllRoles() {
        return customerRoleRepository.findAll();
    }

    @Transactional
    public CustomerRole createRole(String code, String name, String description) {
        if (customerRoleRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Role code already exists: " + code);
        }

        CustomerRole role = new CustomerRole();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        return customerRoleRepository.save(role);
    }
}
