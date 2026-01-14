package com.example.aikef.controller;

import com.example.aikef.tool.internal.impl.ShopifyCustomerServiceTools;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/shopify")
@RequiredArgsConstructor
@Slf4j
public class ShopifyPublicController {

    private final ShopifyCustomerServiceTools shopifyTools;
    private final ObjectMapper objectMapper;

    /**
     * 获取可更换的商品变体
     * @param variantId 当前变体ID
     * @return 可更换的变体列表
     */
    @GetMapping("/exchangeable-variants")
    public Object getExchangeableVariants(@RequestParam String variantId) {
        String result = shopifyTools.getExchangeableVariants(variantId);
        try {
            // 如果返回的是 JSON 数组字符串，解析它以便 ResultWrapper 能正确包装
            if (result.startsWith("[")) {
                return objectMapper.readValue(result, Object.class);
            }
        } catch (Exception e) {
            log.error("Failed to parse variants JSON: {}", result, e);
        }
        return result;
    }
}
