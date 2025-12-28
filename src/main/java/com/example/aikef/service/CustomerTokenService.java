package com.example.aikef.service;

import com.example.aikef.model.Customer;
import com.example.aikef.repository.CustomerRepository;
import com.example.aikef.security.CustomerPrincipal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CustomerTokenService {

    private static final Logger log = LoggerFactory.getLogger(CustomerTokenService.class);
    private static final Duration DEFAULT_TTL = Duration.ofDays(30); // 30天 = 2,592,000秒
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
        log.debug("客户Token已创建: customerId={}, token前缀={}, TTL={}天 ({}秒)", 
                customer.getId(), token.substring(0, Math.min(20, token.length())), 
                DEFAULT_TTL.toDays(), DEFAULT_TTL.getSeconds());
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

    /**
     * 刷新 Token 过期时间
     * 每次验证成功后调用，延长 token 有效期
     */
    public void refreshToken(String token) {
        if (token != null && token.startsWith("cust_")) {
            String key = KEY_PREFIX + token;
            redisTemplate.expire(key, DEFAULT_TTL);
        }
    }

    public void cleanup() {
    }
}
