package com.example.aikef.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "customer_roles")
@Data
public class CustomerRole extends AuditableEntity {

    @Column(nullable = false, unique = true)
    private String code; // SUPPLIER, LOGISTICS, etc.

    @Column(nullable = false)
    private String name;

    private String description;
}
