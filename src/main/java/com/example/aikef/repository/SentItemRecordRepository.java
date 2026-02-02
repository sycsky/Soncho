package com.example.aikef.repository;

import com.example.aikef.model.SentItemRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface SentItemRecordRepository extends JpaRepository<SentItemRecord, UUID>, JpaSpecificationExecutor<SentItemRecord> {
    List<SentItemRecord> findByCustomerId(UUID customerId);
}
