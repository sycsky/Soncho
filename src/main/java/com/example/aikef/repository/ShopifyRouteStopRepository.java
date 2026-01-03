package com.example.aikef.repository;

import com.example.aikef.model.ShopifyRouteStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShopifyRouteStopRepository extends JpaRepository<ShopifyRouteStop, UUID> {
    List<ShopifyRouteStop> findByRoute_IdOrderBySequenceAsc(UUID routeId);
}
