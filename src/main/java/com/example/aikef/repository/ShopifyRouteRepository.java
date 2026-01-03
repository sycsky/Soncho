package com.example.aikef.repository;

import com.example.aikef.model.ShopifyRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShopifyRouteRepository extends JpaRepository<ShopifyRoute, UUID> {
    List<ShopifyRoute> findByDriver_Id(UUID driverId);
}
