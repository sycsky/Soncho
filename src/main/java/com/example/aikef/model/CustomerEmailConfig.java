package com.example.aikef.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "customer_email_configs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"customer_id", "email"})
})
public class CustomerEmailConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String configJson; // Stores EmailAdapter.EmailConfig as JSON

    @Column(nullable = false)
    private Boolean enabled = true;
    
    @Column(name = "last_check_time")
    private LocalDateTime lastCheckTime;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
