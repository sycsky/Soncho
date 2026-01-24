package com.example.aikef.controller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.example.aikef.dto.request.AiRewriteRequest;
import com.example.aikef.dto.request.AiSummaryRequest;
import com.example.aikef.dto.request.AiSuggestTagsRequest;
import com.example.aikef.dto.response.AiRewriteResponse;
import com.example.aikef.dto.response.AiSuggestTagsResponse;
import com.example.aikef.dto.response.AiSummaryResponse;
import com.example.aikef.model.enums.SubscriptionPlan;
import com.example.aikef.security.AgentPrincipal;
import com.example.aikef.service.AgentService;
import com.example.aikef.service.AiKnowledgeService;
import com.example.aikef.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@Slf4j
public class AiController {

    private final AiKnowledgeService aiKnowledgeService;
    private final SubscriptionService subscriptionService;
    private final AgentService agentService;

    public AiController(AiKnowledgeService aiKnowledgeService, SubscriptionService subscriptionService, AgentService agentService) {
        this.aiKnowledgeService = aiKnowledgeService;
        this.subscriptionService = subscriptionService;
        this.agentService = agentService;
    }

    @PostMapping("/summary")
    public AiSummaryResponse summary(@Valid @RequestBody AiSummaryRequest request, Authentication authentication) {
        checkFeature(authentication, SubscriptionPlan::isSupportSmartSummary, "Smart Summary");
        return aiKnowledgeService.summarize(request.sessionId());
    }

    @PostMapping("/rewrite")
    public AiRewriteResponse rewrite(@Valid @RequestBody AiRewriteRequest request, Authentication authentication) {
        checkFeature(authentication, SubscriptionPlan::isSupportMagicRewrite, "Magic Rewrite");
        String language = request.language();
        if ((language == null || language.isBlank()) && authentication != null && authentication.getPrincipal() instanceof AgentPrincipal principal) {
            language = agentService.getAgent(principal.getId()).language();
        }
        return aiKnowledgeService.rewrite(request.text(), request.tone(), request.sessionId(), language);
    }

    @PostMapping("/suggest-tags")
    public AiSuggestTagsResponse suggestTags(@Valid @RequestBody AiSuggestTagsRequest request, Authentication authentication) throws ExecutionException, InterruptedException {
        checkFeature(authentication, SubscriptionPlan::isSupportAiTags, "AI Tags");
        return aiKnowledgeService.suggestTags(request.sessionId()).get();
    }

    private void checkFeature(Authentication authentication, java.util.function.Predicate<SubscriptionPlan> featureCheck, String featureName) {
        if (authentication != null && authentication.getPrincipal() instanceof AgentPrincipal) {
            String tenantId = ((AgentPrincipal) authentication.getPrincipal()).getTenantId();
            SubscriptionPlan plan = subscriptionService.getPlan(tenantId);
            if (!featureCheck.test(plan)) {
                throw new IllegalStateException("Feature '" + featureName + "' is not available in your current plan (" + plan.getDisplayName() + "). Please upgrade.");
            }
        } else {
            // Should be handled by security filter, but safe guard
            throw new IllegalStateException("Authentication required");
        }
    }


    @PostMapping("/test1")
    public JSONArray test1(@RequestBody JSONObject req) {

        log.info("test1 req: {}", req);

        JSONArray jsonArray = new JSONArray();
        JSONObject response = new JSONObject();
        JSONObject response1 = new JSONObject();
        response.put("orderId", "2025234122");
        response.put("name", "杨行1");
        response.put("checkInDate", "2023-12-01");
        response.put("checkOutDate", "2023-12-31");
        response.put("roomType", "单人间");
        response.put("totalPrice", 2000.00);

        response1.put("orderId", "2025234122");
        response1.put("name", "小明");
        response1.put("checkInDate", "2023-12-01");
        response1.put("checkOutDate", "2023-12-31");
        response1.put("roomType", "单人间");
        response1.put("totalPrice", 2400.00);

        jsonArray.add(response);
        jsonArray.add(response1);
        return jsonArray;
    }


    @PostMapping("/orderHotPot")
    public JSONObject orderHotPot(@RequestBody JSONObject req) {

        log.info("test1 id: {}", req);
        JSONObject payment = new JSONObject();
        payment.put("paymentUrl","http://www.payments.com/123");
        return payment;
    }
    @PostMapping("/test1/{id}")
    public JSONArray hotpot(@PathVariable String id, @RequestBody JSONObject req) {


        log.info("hotpot id: {}", req);
        JSONArray jsonArray3 = new JSONArray("[\n" +
                "  {\n" +
                "    \"id\": 8205012398101,\n" +
                "    \"title\": \"重庆老火锅·鲜毛肚鸭血七件套 (2人食)\",\n" +
                "    \"body_html\": \"<p><strong>无辣不欢，地道重庆味！</strong></p><p>套餐包含：极品黑千层毛肚(200g)、鲜鸭血(300g)、去骨鸭掌(6只)、贡菜、手工宽粉以及特辣牛油锅底。</p>\",\n" +
                "    \"vendor\": \"辣妹子火锅\",\n" +
                "    \"product_type\": \"火锅套餐\",\n" +
                "    \"handle\": \"sichuan-spicy-tripe-combo\",\n" +
                "    \"tags\": \"火锅, 川味, 麻辣, 毛肚, 鸭血\",\n" +
                "    \"status\": \"active\",\n" +
                "    \"variants\": [\n" +
                "      {\n" +
                "        \"id\": 44102938475101,\n" +
                "        \"product_id\": 8205012398101,\n" +
                "        \"title\": \"标准辣度\",\n" +
                "        \"price\": \"158.00\",\n" +
                "        \"sku\": \"HP-SPICY-STD\",\n" +
                "        \"grams\": 1800,\n" +
                "        \"inventory_quantity\": 30,\n" +
                "        \"requires_shipping\": true\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 44102938475102,\n" +
                "        \"product_id\": 8205012398101,\n" +
                "        \"title\": \"特辣加麻\",\n" +
                "        \"price\": \"158.00\",\n" +
                "        \"sku\": \"HP-SPICY-EXT\",\n" +
                "        \"grams\": 1850,\n" +
                "        \"inventory_quantity\": 15,\n" +
                "        \"requires_shipping\": true\n" +
                "      }\n" +
                "    ],\n" +
                "    \"images\": [\n" +
                "      {\n" +
                "        \"id\": 7102938475101,\n" +
                "        \"product_id\": 8205012398101,\n" +
                "        \"src\": \"https://example.com/images/spicy-tripe-pot.jpg\",\n" +
                "        \"alt\": \"重庆毛肚火锅套餐\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 8205012398102,\n" +
                "    \"title\": \"蓝鳍金枪鱼现切刺身 (Bluefin Tuna)\",\n" +
                "    \"body_html\": \"<p>来自深海的红宝石。精选蓝鳍金枪鱼，口感细腻，油脂丰富。</p>\",\n" +
                "    \"vendor\": \"大洋世家\",\n" +
                "    \"product_type\": \"刺身\",\n" +
                "    \"handle\": \"bluefin-tuna-sashimi\",\n" +
                "    \"tags\": \"海鲜, 刺身, 金枪鱼, 高端\",\n" +
                "    \"status\": \"active\",\n" +
                "    \"options\": [\n" +
                "      {\n" +
                "        \"name\": \"部位\",\n" +
                "        \"values\": [\"赤身 (Akami)\", \"中腹 (Chutoro)\"]\n" +
                "      }\n" +
                "    ],\n" +
                "    \"variants\": [\n" +
                "      {\n" +
                "        \"id\": 44102938475103,\n" +
                "        \"product_id\": 8205012398102,\n" +
                "        \"title\": \"赤身 (Akami) - 200g\",\n" +
                "        \"price\": \"188.00\",\n" +
                "        \"sku\": \"SF-TUNA-AKAMI\",\n" +
                "        \"grams\": 200,\n" +
                "        \"inventory_quantity\": 10,\n" +
                "        \"requires_shipping\": true\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 44102938475104,\n" +
                "        \"product_id\": 8205012398102,\n" +
                "        \"title\": \"中腹 (Chutoro) - 200g\",\n" +
                "        \"price\": \"288.00\",\n" +
                "        \"sku\": \"SF-TUNA-CHUTORO\",\n" +
                "        \"grams\": 200,\n" +
                "        \"inventory_quantity\": 5,\n" +
                "        \"requires_shipping\": true\n" +
                "      }\n" +
                "    ],\n" +
                "    \"images\": [\n" +
                "      {\n" +
                "        \"id\": 7102938475102,\n" +
                "        \"product_id\": 8205012398102,\n" +
                "        \"src\": \"https://example.com/images/tuna-sashimi.jpg\",\n" +
                "        \"alt\": \"蓝鳍金枪鱼刺身\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 8205012398103,\n" +
                "    \"title\": \"0添加鲜榨生椰乳 (1L装)\",\n" +
                "    \"body_html\": \"<p>选用东南亚进口老椰皇，鲜榨冷灌。口感浓郁顺滑，是调制生椰拿铁或搭配麻辣火锅的绝佳选择。</p>\",\n" +
                "    \"vendor\": \"热带工坊\",\n" +
                "    \"product_type\": \"饮料\",\n" +
                "    \"handle\": \"fresh-coconut-milk\",\n" +
                "    \"tags\": \"饮料, 椰奶, 甜品, 解辣\",\n" +
                "    \"status\": \"active\",\n" +
                "    \"variants\": [\n" +
                "      {\n" +
                "        \"id\": 44102938475105,\n" +
                "        \"product_id\": 8205012398103,\n" +
                "        \"title\": \"1升畅饮装\",\n" +
                "        \"price\": \"18.50\",\n" +
                "        \"sku\": \"DRK-COCO-1L\",\n" +
                "        \"grams\": 1100,\n" +
                "        \"inventory_quantity\": 150,\n" +
                "        \"requires_shipping\": true\n" +
                "      }\n" +
                "    ],\n" +
                "    \"images\": [\n" +
                "      {\n" +
                "        \"id\": 7102938475103,\n" +
                "        \"product_id\": 8205012398103,\n" +
                "        \"src\": \"https://example.com/images/coconut-milk.jpg\",\n" +
                "        \"alt\": \"鲜榨生椰乳\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": 8205012398104,\n" +
                "    \"title\": \"便携式防风卡式炉 (含收纳箱)\",\n" +
                "    \"body_html\": \"<p>户外露营、家庭火锅两用。2.9KW大火力，聚能防风环设计。<strong>注意：气罐需单独购买。</strong></p>\",\n" +
                "    \"vendor\": \"山野装备\",\n" +
                "    \"product_type\": \"厨具\",\n" +
                "    \"handle\": \"portable-gas-stove\",\n" +
                "    \"tags\": \"厨具, 炉具, 户外, 露营\",\n" +
                "    \"status\": \"active\",\n" +
                "    \"options\": [\n" +
                "      {\n" +
                "        \"name\": \"颜色\",\n" +
                "        \"values\": [\"磨砂黑\", \"奶油黄\"]\n" +
                "      }\n" +
                "    ],\n" +
                "    \"variants\": [\n" +
                "      {\n" +
                "        \"id\": 44102938475106,\n" +
                "        \"product_id\": 8205012398104,\n" +
                "        \"title\": \"磨砂黑\",\n" +
                "        \"price\": \"129.00\",\n" +
                "        \"sku\": \"TOOL-GAS-BLK\",\n" +
                "        \"grams\": 1500,\n" +
                "        \"inventory_quantity\": 25,\n" +
                "        \"requires_shipping\": true\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 44102938475107,\n" +
                "        \"product_id\": 8205012398104,\n" +
                "        \"title\": \"奶油黄\",\n" +
                "        \"price\": \"129.00\",\n" +
                "        \"sku\": \"TOOL-GAS-YLW\",\n" +
                "        \"grams\": 1500,\n" +
                "        \"inventory_quantity\": 20,\n" +
                "        \"requires_shipping\": true\n" +
                "      }\n" +
                "    ],\n" +
                "    \"images\": [\n" +
                "      {\n" +
                "        \"id\": 7102938475104,\n" +
                "        \"product_id\": 8205012398104,\n" +
                "        \"src\": \"https://example.com/images/gas-stove.jpg\",\n" +
                "        \"alt\": \"便携式卡式炉\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]");
        JSONArray jsonArray = new JSONArray("[\n" +
                "  {\n" +
                "    \"id\": \"hp_combo_001\",\n" +
                "    \"name\": \"经典双人牛羊肉畅享餐\",\n" +
                "    \"price\": 168.00,\n" +
                "    \"currency\": \"CNY\",\n" +
                "    \"suitable_for\": \"2 People\",\n" +
                "    \"tags\": [\"性价比\", \"肉食主义\", \"经典款\"],\n" +
                "    \"description\": \"最受欢迎的经典搭配，一次尝遍精选肥牛与草原羊肉，适合情侣或好友聚餐。\",\n" +
                "    \"items\": {\n" +
                "      \"soup_base\": [\n" +
                "        \"鸳鸯锅底 (牛油麻辣 + 骨汤)\",\n" +
                "        \"番茄浓汤锅\"\n" +
                "      ],\n" +
                "      \"meats\": [\n" +
                "        \"精品雪花肥牛 (200g)\",\n" +
                "        \"草原高钙羊肉卷 (200g)\",\n" +
                "        \"手工虾滑 (100g)\"\n" +
                "      ],\n" +
                "      \"vegetables\": [\n" +
                "        \"时蔬大拼盘 (包含娃娃菜/生菜/菠菜)\",\n" +
                "        \"金针菇\",\n" +
                "        \"冻豆腐\"\n" +
                "      ],\n" +
                "      \"staples\": [\n" +
                "        \"手擀面\",\n" +
                "        \"宽粉\"\n" +
                "      ],\n" +
                "      \"drinks\": [\n" +
                "        \"酸梅汤 (一扎)\"\n" +
                "      ]\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": \"hp_combo_002\",\n" +
                "    \"name\": \"至尊海陆盛宴四人餐\",\n" +
                "    \"price\": 488.00,\n" +
                "    \"currency\": \"CNY\",\n" +
                "    \"suitable_for\": \"4-5 People\",\n" +
                "    \"tags\": [\"豪华\", \"海鲜\", \"聚会首选\"],\n" +
                "    \"description\": \"甄选澳洲和牛与深海鲜活海鲜，为商务宴请或家庭聚会提供极致的味蕾享受。\",\n" +
                "    \"items\": {\n" +
                "      \"soup_base\": [\n" +
                "        \"奔驰三味锅 (菌汤 + 辣锅 + 番茄)\",\n" +
                "        \"花胶鸡汤锅\"\n" +
                "      ],\n" +
                "      \"meats\": [\n" +
                "        \"M6澳洲和牛 (150g)\",\n" +
                "        \"极品鲜毛肚\",\n" +
                "        \"深海鲍鱼 (4只)\",\n" +
                "        \"黑虎虾 (8只)\",\n" +
                "        \"雪花猪颈肉\"\n" +
                "      ],\n" +
                "      \"vegetables\": [\n" +
                "        \"有机菌菇拼盘\",\n" +
                "        \"山药\",\n" +
                "        \"鲜笋片\",\n" +
                "        \"响铃卷\"\n" +
                "      ],\n" +
                "      \"staples\": [\n" +
                "        \"海鲜炒饭\",\n" +
                "        \"蔬菜面\"\n" +
                "      ],\n" +
                "      \"drinks\": [\n" +
                "        \"鲜榨西瓜汁\",\n" +
                "        \"以及 4 份自选软饮\"\n" +
                "      ]\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": \"hp_combo_003\",\n" +
                "    \"name\": \"田园轻食低脂素食餐\",\n" +
                "    \"price\": 128.00,\n" +
                "    \"currency\": \"CNY\",\n" +
                "    \"suitable_for\": \"2 People\",\n" +
                "    \"tags\": [\"健康\", \"低卡\", \"素食\"],\n" +
                "    \"description\": \"专为健康饮食人群设计，选用当季新鲜时蔬与豆制品，清爽不油腻。\",\n" +
                "    \"items\": {\n" +
                "      \"soup_base\": [\n" +
                "        \"野山菌养生锅\",\n" +
                "        \"番茄维C锅\"\n" +
                "      ],\n" +
                "      \"meats\": [],\n" +
                "      \"vegetables\": [\n" +
                "        \"田园时蔬桶 (5种季节蔬菜)\",\n" +
                "        \"七彩豆类拼盘 (包含嫩豆腐/油豆皮/腐竹)\",\n" +
                "        \"竹荪\",\n" +
                "        \"魔芋丝\",\n" +
                "        \"莲藕片\"\n" +
                "      ],\n" +
                "      \"staples\": [\n" +
                "        \"杂粮面\",\n" +
                "        \"红糖糍粑\"\n" +
                "      ],\n" +
                "      \"drinks\": [\n" +
                "        \"无糖乌龙茶 (2瓶)\"\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "]}");

        JSONArray jsonArray1 = new JSONArray("[\n" +
                "  {\n" +
                "    \"id\": \"hp_combo_001\",\n" +
                "    \"name\": \"经典双人牛羊肉畅享餐\",\n" +
                "    \"price\": 168.00,\n" +
                "    \"currency\": \"CNY\",\n" +
                "    \"suitable_for\": \"2 People\",\n" +
                "    \"tags\": [\"性价比\", \"肉食主义\", \"经典款\"],\n" +
                "    \"description\": \"最受欢迎的经典搭配，一次尝遍精选肥牛与草原羊肉，适合情侣或好友聚餐。\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": \"hp_combo_002\",\n" +
                "    \"name\": \"至尊海陆盛宴四人餐\",\n" +
                "    \"price\": 488.00,\n" +
                "    \"currency\": \"CNY\",\n" +
                "    \"suitable_for\": \"4-5 People\",\n" +
                "    \"tags\": [\"豪华\", \"海鲜\", \"聚会首选\"],\n" +
                "    \"description\": \"甄选澳洲和牛与深海鲜活海鲜，为商务宴请或家庭聚会提供极致的味蕾享受。\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"id\": \"hp_combo_003\",\n" +
                "    \"name\": \"田园轻食低脂素食餐\",\n" +
                "    \"price\": 128.00,\n" +
                "    \"currency\": \"CNY\",\n" +
                "    \"suitable_for\": \"2 People\",\n" +
                "    \"tags\": [\"健康\", \"低卡\", \"素食\"],\n" +
                "    \"description\": \"专为健康饮食人群设计，选用当季新鲜时蔬与豆制品，清爽不油腻。\"\n" +
                "  }\n" +
                "]");


        if("1".equals(id)) {
            return jsonArray1;
        }else if("2".equals(id)) {
            return jsonArray;
        }else {
            return jsonArray3;
        }

    }


    @PostMapping("/test2")
    public String test2(@RequestBody JSONObject req) {


        log.info("test2 req: {}", req);
        String city =  req.get("city").toString();


        return String.format("城市:%s天气为晴天",city);
    }

    @PostMapping("/test3")
    public String test3(@RequestBody JSONObject req) {


        log.info("test2 req: {}", req);
        String city =  req.get("city").toString();


        return String.format("城市:%s天气为晴天",city);
    }
}
