package com.example.aikef.controller;

import com.example.aikef.model.CustomerRole;
import com.example.aikef.service.SpecialCustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer-roles")
@RequiredArgsConstructor
public class CustomerRoleController {

    private final SpecialCustomerService specialCustomerService;

    @GetMapping
    public List<CustomerRole> listRoles() {
        return specialCustomerService.getAllRoles();
    }

    @PostMapping
    public CustomerRole createRole(@Valid @RequestBody CreateRoleRequest request) {
        return specialCustomerService.createRole(request.code(), request.name(), request.description());
    }

    public record CreateRoleRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description
    ) {}
}
