package com.example.aikef.service;

import com.example.aikef.model.ChatSession;
import com.example.aikef.model.Customer;
import com.example.aikef.model.ShopifyRoute;
import com.example.aikef.model.ShopifyRouteStop;
import com.example.aikef.repository.ChatSessionRepository;
import com.example.aikef.repository.CustomerRepository;
import com.example.aikef.repository.ShopifyRouteRepository;
import com.example.aikef.repository.ShopifyRouteStopRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShopifyLogisticsService {

    private final ShopifyRouteRepository routeRepository;
    private final ShopifyRouteStopRepository stopRepository;
    private final CustomerRepository customerRepository;
    private final EventService eventService;
    private final ChatSessionRepository chatSessionRepository;

    @Transactional
    public ShopifyRoute createRoute(String name, UUID driverId) {
        Customer driver = customerRepository.findById(driverId)
                .orElseThrow(() -> new EntityNotFoundException("Driver not found"));
        
        ShopifyRoute route = new ShopifyRoute();
        route.setName(name);
        route.setDriver(driver);
        route.setStatus("PLANNED");
        route.setOptimized(false);
        return routeRepository.save(route);
    }

    @Transactional
    public ShopifyRouteStop addStop(UUID routeId, String shopifyOrderId, String location, String customerInfo) {
        ShopifyRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new EntityNotFoundException("Route not found"));

        ShopifyRouteStop stop = new ShopifyRouteStop();
        stop.setRoute(route);
        stop.setShopifyOrderId(shopifyOrderId);
        stop.setLocation(location);
        stop.setCustomerInfo(customerInfo);
        stop.setStatus("PENDING");
        
        // Auto sequence
        List<ShopifyRouteStop> stops = stopRepository.findByRoute_IdOrderBySequenceAsc(routeId);
        stop.setSequence(stops.size() + 1);

        return stopRepository.save(stop);
    }

    @Transactional
    public void optimizeRoute(UUID routeId) {
        ShopifyRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new EntityNotFoundException("Route not found"));

        // Mock optimization: Just re-save status or re-order (no-op for now but set flag)
        log.info("Optimizing route {} via Google Maps (Mock)...", routeId);
        route.setOptimized(true);
        routeRepository.save(route);

        // Trigger Event to Driver
        if (route.getDriver() != null) {
            triggerEventForCustomer(route.getDriver().getId(), "EVENT_ROUTE_OPTIMIZED", Map.of(
                    "routeId", route.getId(),
                    "routeName", route.getName(),
                    "message", "Route " + route.getName() + " has been optimized."
            ));
        }
    }

    @Transactional
    public void updateStopStatus(UUID stopId, String status) {
        ShopifyRouteStop stop = stopRepository.findById(stopId)
                .orElseThrow(() -> new EntityNotFoundException("Stop not found"));
        
        stop.setStatus(status);
        stopRepository.save(stop);

        // Trigger Event to Driver (if needed, e.g. confirmation) or Initiator?
        // Requirement: "Stop change also triggers event". Assuming to Driver for now or System.
        if (stop.getRoute().getDriver() != null) {
            triggerEventForCustomer(stop.getRoute().getDriver().getId(), "EVENT_STOP_UPDATED", Map.of(
                    "stopId", stop.getId(),
                    "status", status,
                    "message", "Stop " + stop.getLocation() + " status updated to " + status
            ));
        }
    }

    private void triggerEventForCustomer(UUID customerId, String eventName, Map<String, Object> data) {
        try {
            ChatSession session = chatSessionRepository.findFirstByCustomer_IdOrderByLastActiveAtDesc(customerId);
            if (session != null) {
                eventService.triggerEvent(eventName, session.getId(), data);
            } else {
                log.warn("No active session found for customer {}, cannot trigger event {}", customerId, eventName);
            }
        } catch (Exception e) {
            log.error("Failed to trigger event {} for customer {}", eventName, customerId, e);
        }
    }
}
