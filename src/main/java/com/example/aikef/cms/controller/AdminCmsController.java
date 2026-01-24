package com.example.aikef.cms.controller;

import com.example.aikef.cms.model.CmsArticle;
import com.example.aikef.cms.model.CmsCategory;
import com.example.aikef.cms.service.CmsService;
import com.example.aikef.cms.service.SeoArticleGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/cms")
@RequiredArgsConstructor
public class AdminCmsController {

    private final CmsService cmsService;
    private final SeoArticleGeneratorService seoService;
    
    // Hardcoded token as requested by user
    private static final String ADMIN_TOKEN = "soncho-2026";

    private void validateToken(String token) {
        if (token == null || !ADMIN_TOKEN.equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid CMS Token");
        }
    }

    @GetMapping("/articles")
    public Page<CmsArticle> getAllArticles(
            @RequestHeader(value = "X-CMS-Token", required = false) String token,
            Pageable pageable) {

        return cmsService.getAllArticles(pageable);
    }

    @PostMapping("/articles")
    public ResponseEntity<CmsArticle> createArticle(
            @RequestHeader(value = "X-CMS-Token", required = false) String token,
            @RequestBody CmsArticle article) {
        validateToken(token);
        return ResponseEntity.ok(cmsService.createArticle(article));
    }

    @PutMapping("/articles/{id}")
    public ResponseEntity<CmsArticle> updateArticle(
            @RequestHeader(value = "X-CMS-Token", required = false) String token,
            @PathVariable UUID id, 
            @RequestBody CmsArticle article) {

        return ResponseEntity.ok(cmsService.updateArticle(id, article));
    }

    @DeleteMapping("/articles/{id}")
    public ResponseEntity<Void> deleteArticle(
            @RequestHeader(value = "X-CMS-Token", required = false) String token,
            @PathVariable UUID id) {
        validateToken(token);
        cmsService.deleteArticle(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/generate")
    public ResponseEntity<CmsArticle> generateArticle(
            @RequestHeader(value = "X-CMS-Token", required = false) String token,
            @RequestParam String topic) {

        return ResponseEntity.ok(seoService.generateAndPublishDailyArticle(topic));
    }

    // Category Endpoints
    @GetMapping("/categories")
    public List<CmsCategory> getAllCategories(@RequestHeader(value = "X-CMS-Token", required = false) String token) {
        return cmsService.getAllCategories();
    }

    @PostMapping("/categories")
    public ResponseEntity<CmsCategory> createCategory(
            @RequestHeader(value = "X-CMS-Token", required = false) String token,
            @RequestBody CmsCategory category) {
        validateToken(token);
        return ResponseEntity.ok(cmsService.createCategory(category));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<CmsCategory> updateCategory(
            @RequestHeader(value = "X-CMS-Token", required = false) String token,
            @PathVariable UUID id,
            @RequestBody CmsCategory category) {
        validateToken(token);
        return ResponseEntity.ok(cmsService.updateCategory(id, category));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(
            @RequestHeader(value = "X-CMS-Token", required = false) String token,
            @PathVariable UUID id) {
        validateToken(token);
        cmsService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/categories/reorder")
    public ResponseEntity<Void> reorderCategories(
            @RequestHeader(value = "X-CMS-Token", required = false) String token,
            @RequestBody List<UUID> orderedIds) {
        validateToken(token);
        cmsService.reorderCategories(orderedIds);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/articles/reorder")
    public ResponseEntity<Void> reorderArticles(
            @RequestHeader(value = "X-CMS-Token", required = false) String token,
            @RequestBody List<UUID> orderedIds) {
        validateToken(token);
        cmsService.reorderArticles(orderedIds);
        return ResponseEntity.ok().build();
    }
}
