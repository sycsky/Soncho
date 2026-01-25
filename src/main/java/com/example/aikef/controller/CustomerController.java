package com.example.aikef.controller;


import com.example.aikef.dto.CustomerDto;
import com.example.aikef.dto.CustomerTokenResponse;
import com.example.aikef.dto.request.CreateCustomerRequest;
import com.example.aikef.dto.request.UpdateCustomerRequest;
import com.example.aikef.model.Channel;
import com.example.aikef.service.CustomerService;
import com.example.aikef.shopify.service.ShopifyGraphQLService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final com.example.aikef.service.SpecialCustomerService specialCustomerService;
    private final ShopifyGraphQLService shopifyGraphQLService;

    public CustomerController(CustomerService customerService, 
                              com.example.aikef.service.SpecialCustomerService specialCustomerService,
                              ShopifyGraphQLService shopifyGraphQLService) {
        this.customerService = customerService;
        this.specialCustomerService = specialCustomerService;
        this.shopifyGraphQLService = shopifyGraphQLService;
    }

    @GetMapping
    public Page<CustomerDto> listCustomers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Channel channel,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CustomerDto> customers = customerService.listCustomers(name, channel, tag, active, pageable);
        return customers;
    }

    @GetMapping("/{id}")
    public CustomerDto getCustomer(@PathVariable UUID id) {
        CustomerDto customer = customerService.getCustomer(id);
        return customer;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDto createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerDto customer = customerService.createCustomer(request);
        return customer;
    }

    @PutMapping("/{id}")
    public CustomerDto updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        CustomerDto customer = customerService.updateCustomer(id, request);
        return customer;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomer(@PathVariable UUID id) {
        customerService.deleteCustomer(id);
    }

    /**
     * 为客户生成访客 Token
     */
    @PostMapping("/{id}/token")
    public CustomerTokenResponse generateToken(@PathVariable UUID id) {
        CustomerTokenResponse tokenResponse = customerService.generateCustomerToken(id);
        return tokenResponse;
    }

    /**
     * 为客户分配特殊角色
     * @param id 客户ID
     * @param roleCode 角色代码 (SUPPLIER, LOGISTICS, PROMOTER, WAREHOUSE)
     */
    @PostMapping("/{id}/role")
    public CustomerDto assignRole(@PathVariable UUID id, @RequestParam String roleCode) {
        specialCustomerService.assignRole(id, roleCode);
        return customerService.getCustomer(id);
    }

    /**
     * Get recent Shopify orders for a customer
     */
    @GetMapping("/{id}/shopify-orders")
    public JsonNode getCustomerShopifyOrders(@PathVariable UUID id) {
        CustomerDto customer = customerService.getCustomer(id);
        if (customer.shopifyCustomerId() == null) {
            return com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
        }
        return shopifyGraphQLService.getOrdersByCustomerId(customer.shopifyCustomerId(), 5);
    }
}
