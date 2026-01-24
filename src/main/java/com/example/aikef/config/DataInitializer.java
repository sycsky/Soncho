package com.example.aikef.config;

import com.example.aikef.model.Agent;
import com.example.aikef.model.Role;
import com.example.aikef.model.PermissionConstants;
import com.example.aikef.model.enums.AgentStatus;
import com.example.aikef.repository.AgentRepository;
import com.example.aikef.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final AgentRepository agentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AgentRepository agentRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.agentRepository = agentRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        logger.info("Checking and initializing default roles and admin user...");
        
        // 1. 初始化 Administrator 角色
        Role adminRole = roleRepository.findByName("Administrator").orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName("Administrator");
            newRole.setDescription("System-wide administrator with all permissions.");
            newRole.setSystem(true);
            newRole.setPermissions(PermissionConstants.createAllPermissionsMap());
            return roleRepository.save(newRole);
        });
        
        if (adminRole.getPermissions() == null || adminRole.getPermissions().isEmpty()) {
            adminRole.setPermissions(PermissionConstants.createAllPermissionsMap());
            adminRole = roleRepository.save(adminRole);
        }

        // 2. 初始化 Admin 用户
        String adminEmail = "admin@nexus.com";
        String adminPassword = "Admin@123";

        Optional<Agent> existingAdmin = agentRepository.findByEmail(adminEmail);

        if (existingAdmin.isPresent()) {
            Agent admin = existingAdmin.get();
            // Check if password needs to be updated
            if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(adminPassword));
                agentRepository.save(admin);
                logger.info("Admin user password has been reset.");
            } else {
                logger.info("Admin user password is up to date.");
            }
        } else {
            Agent admin = new Agent();
            admin.setName("Nexus Admin");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setStatus(AgentStatus.ONLINE);
            admin.setRole(adminRole);
            agentRepository.save(admin);
            logger.info("Admin user created with default password.");
        }
    }
}
