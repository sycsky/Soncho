package com.example.aikef.shopify.controller;

import com.example.aikef.shopify.service.ShopifyThemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/shopify/extension")
@RequiredArgsConstructor
public class ShopifyExtensionController {

    private final ShopifyThemeService themeService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getAppEmbedStatus(
            @RequestParam String shop,
            @RequestParam(defaultValue = "chat_widget") String handle) {

        boolean enabled = themeService.isAppEmbedEnabled(shop, handle);
        return ResponseEntity.ok(Collections.singletonMap("enabled", enabled));
    }
}
