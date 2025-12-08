package com.example.aikef.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.example.aikef.dto.request.AiRewriteRequest;
import com.example.aikef.dto.request.AiSummaryRequest;
import com.example.aikef.dto.request.AiSuggestTagsRequest;
import com.example.aikef.dto.response.AiRewriteResponse;
import com.example.aikef.dto.response.AiSuggestTagsResponse;
import com.example.aikef.dto.response.AiSummaryResponse;
import com.example.aikef.service.AiKnowledgeService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@Slf4j
public class AiController {

    private final AiKnowledgeService aiKnowledgeService;

    public AiController(AiKnowledgeService aiKnowledgeService) {
        this.aiKnowledgeService = aiKnowledgeService;
    }

    @PostMapping("/summary")
    public AiSummaryResponse summary(@Valid @RequestBody AiSummaryRequest request) {
        return aiKnowledgeService.summarize(request.sessionId());
    }

    @PostMapping("/rewrite")
    public AiRewriteResponse rewrite(@Valid @RequestBody AiRewriteRequest request) {
        return aiKnowledgeService.rewrite(request.text());
    }

    @PostMapping("/suggest-tags")
    public AiSuggestTagsResponse suggestTags(@Valid @RequestBody AiSuggestTagsRequest request) {
        return aiKnowledgeService.suggestTags(request.sessionId());
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
