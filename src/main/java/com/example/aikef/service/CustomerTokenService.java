package com.example.aikef.service;

import com.example.aikef.model.Customer;
import com.example.aikef.repository.CustomerRepository;
import com.example.aikef.security.CustomerPrincipal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CustomerTokenService {

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "cust_token:";

    private final CustomerRepository customerRepository;
    private final StringRedisTemplate redisTemplate;

    public CustomerTokenService(CustomerRepository customerRepository, StringRedisTemplate redisTemplate) {
        this.customerRepository = customerRepository;
        this.redisTemplate = redisTemplate;
    }

    public String issueToken(Customer customer) {
        String token = "cust_" + UUID.randomUUID();
        String key = KEY_PREFIX + token;
        redisTemplate.opsForValue().set(key, customer.getId().toString(), DEFAULT_TTL);
        return token;
    }

    public Optional<CustomerPrincipal> resolve(String token) {
        if (token == null || !token.startsWith("cust_")) {
            return Optional.empty();
        }
        String key = KEY_PREFIX + token;
        String customerIdStr = redisTemplate.opsForValue().get(key);
        if (customerIdStr == null || customerIdStr.isBlank()) {
            return Optional.empty();
        }
        UUID customerId = UUID.fromString(customerIdStr);
        return customerRepository.findById(customerId)
                .filter(Customer::isActive)
                .map(customer -> new CustomerPrincipal(
                        customer.getId(),
                        customer.getName(),
                        customer.getPrimaryChannel().name()
                ));
    }

    public void revoke(String token) {
        if (token == null) {
            return;
        }
        String key = KEY_PREFIX + token;
        redisTemplate.delete(key);
    }

    public void cleanup() {
    }
}
