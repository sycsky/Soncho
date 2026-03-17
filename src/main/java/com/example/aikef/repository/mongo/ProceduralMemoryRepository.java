package com.example.aikef.repository.mongo;

import com.example.aikef.model.mongo.ProceduralMemory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProceduralMemoryRepository extends MongoRepository<ProceduralMemory, String> {
    Optional<ProceduralMemory> findTopByCustomerIdAndScenarioKeyAndArchivedFalseOrderByLastUpdatedDesc(String customerId, String scenarioKey);
    Optional<ProceduralMemory> findTopByCustomerIdAndStatusAndArchivedFalseOrderByLastUpdatedDesc(String customerId, String status);
    Optional<ProceduralMemory> findTopByCustomerIdAndReusableTrueAndArchivedFalseOrderByLastUsedAtDesc(String customerId);
    Optional<ProceduralMemory> findTopByCustomerIdAndArchivedFalseOrderByLastUpdatedDesc(String customerId);
    List<ProceduralMemory> findTop5ByCustomerIdAndArchivedFalseOrderByLastUpdatedDesc(String customerId);
    List<ProceduralMemory> findTop20ByCustomerIdAndArchivedFalseOrderByLastUpdatedDesc(String customerId);
}
