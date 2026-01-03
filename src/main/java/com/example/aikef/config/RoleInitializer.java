package com.example.aikef.config;

import com.example.aikef.model.CustomerRole;
import com.example.aikef.repository.CustomerRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class RoleInitializer {

    private final CustomerRoleRepository roleRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initRoles() {
        createRoleIfNotFound("SUPPLIER", "供货商", "Supplier Role");
        createRoleIfNotFound("LOGISTICS", "物流人员", "Logistics/Driver Role");
        createRoleIfNotFound("PROMOTER", "推广者", "Promoter Role");
        createRoleIfNotFound("WAREHOUSE", "仓库管理员", "Warehouse Admin Role");
    }

    private void createRoleIfNotFound(String code, String name, String description) {
        if (roleRepository.findByCode(code).isEmpty()) {
            CustomerRole role = new CustomerRole();
            role.setId(UUID.randomUUID());
            role.setCode(code);
            role.setName(name);
            role.setDescription(description);
            roleRepository.save(role);
            log.info("Initialized role: {}", code);
        }
    }
}
