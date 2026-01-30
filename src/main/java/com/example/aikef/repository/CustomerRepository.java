package com.example.aikef.repository;

import com.example.aikef.model.Channel;
import com.example.aikef.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByWechatOpenId(String wechatOpenId);

    Optional<Customer> findByWhatsappId(String whatsappId);

    Optional<Customer> findByLineId(String lineId);

    Optional<Customer> findByTelegramId(String telegramId);

    Optional<Customer> findByFacebookId(String facebookId);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<Customer> findByShopifyCustomerId(String shopifyCustomerId);
}
