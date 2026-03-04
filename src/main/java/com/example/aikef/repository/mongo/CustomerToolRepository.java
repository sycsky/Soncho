package com.example.aikef.repository.mongo;

import com.example.aikef.model.mongo.CustomerTool;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerToolRepository extends MongoRepository<CustomerTool, String> {
    List<CustomerTool> findByCustomerId(String customerId);
    Optional<CustomerTool> findByCustomerIdAndToolName(String customerId, String toolName);
}
