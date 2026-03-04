package com.example.aikef.repository.mongo;

import com.example.aikef.model.mongo.CustomerCoreMemory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerCoreMemoryRepository extends MongoRepository<CustomerCoreMemory, String> {
    Optional<CustomerCoreMemory> findByCustomerId(String customerId);
}
