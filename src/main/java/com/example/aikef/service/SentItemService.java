package com.example.aikef.service;

import com.example.aikef.model.Customer;
import com.example.aikef.model.SentItemRecord;
import com.example.aikef.model.enums.SenderType;
import com.example.aikef.model.enums.SentItemType;
import com.example.aikef.repository.CustomerRepository;
import com.example.aikef.repository.SentItemRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class SentItemService {

    private final SentItemRecordRepository sentItemRecordRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<SentItemRecord> searchSentItems(String customerIdStr, LocalDateTime start, LocalDateTime end) {
        Specification<SentItemRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (customerIdStr != null && !customerIdStr.isBlank()) {
                try {
                    UUID customerId = UUID.fromString(customerIdStr);
                    predicates.add(cb.equal(root.get("customer").get("id"), customerId));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid customer ID format for search: {}", customerIdStr);
                    // If invalid ID is provided, return empty or ignore? 
                    // Let's treat it as a condition that will match nothing if strict, 
                    // or maybe just ignore it if we want to be lenient.
                    // But usually invalid ID means no match.
                    return cb.disjunction();
                }
            }

            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            }

            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return sentItemRecordRepository.findAll(spec);
    }

    @Transactional
    public void recordSentItem(String customerIdStr, SentItemType itemType, String itemValue, String amount, SenderType sentBy, String note) {
        if (customerIdStr == null) {
            log.warn("Cannot record sent item: customerId is null");
            return;
        }

        try {
            UUID customerId = UUID.fromString(customerIdStr);
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));

            SentItemRecord record = new SentItemRecord();
            record.setCustomer(customer);
            record.setItemType(itemType);
            record.setItemValue(itemValue);
            record.setAmount(amount);
            record.setSentBy(sentBy);
            record.setNote(note);

            sentItemRecordRepository.save(record);
            log.info("Recorded sent item: type={}, value={}, to customer={}", itemType, itemValue, customerId);
        } catch (Exception e) {
            log.error("Failed to record sent item", e);
            // Don't throw, just log, so we don't block the main flow
        }
    }
}
