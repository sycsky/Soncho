package com.example.aikef.tool.internal.impl;

import com.example.aikef.model.ShopifyRoute;
import com.example.aikef.model.ShopifyRouteStop;
import com.example.aikef.service.ShopifyLogisticsService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LogisticsTools {

    private final ShopifyLogisticsService logisticsService;

    @Tool("Create a new delivery route")
    public String createRoute(
            @P(value = "Route Name", required = true) String name,
            @P(value = "Driver Customer ID", required = true) String driverId
    ) {
        ShopifyRoute route = logisticsService.createRoute(name, UUID.fromString(driverId));
        return "Route created with ID: " + route.getId();
    }

    @Tool("Add a stop to a route")
    public String addStop(
            @P(value = "Route ID", required = true) String routeId,
            @P(value = "Shopify Order ID", required = true) String shopifyOrderId,
            @P(value = "Location/Address", required = true) String location,
            @P(value = "Customer Info (JSON or Text)", required = true) String customerInfo
    ) {
        ShopifyRouteStop stop = logisticsService.addStop(
                UUID.fromString(routeId),
                shopifyOrderId,
                location,
                customerInfo
        );
        return "Stop added with ID: " + stop.getId();
    }

    @Tool("Optimize a route (via Google Maps)")
    public String optimizeRoute(
            @P(value = "Route ID", required = true) String routeId
    ) {
        logisticsService.optimizeRoute(UUID.fromString(routeId));
        return "Route optimization started. Driver will be notified.";
    }

    @Tool("Update stop status")
    public String updateStopStatus(
            @P(value = "Stop ID", required = true) String stopId,
            @P(value = "New Status (PENDING, COMPLETED, SKIPPED)", required = true) String status
    ) {
        logisticsService.updateStopStatus(UUID.fromString(stopId), status);
        return "Stop status updated to " + status;
    }
}
