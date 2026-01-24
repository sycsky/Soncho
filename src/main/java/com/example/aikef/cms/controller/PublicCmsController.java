package com.example.aikef.cms.controller;

import com.example.aikef.cms.model.CmsArticle;
import com.example.aikef.cms.model.CmsArticleType;
import com.example.aikef.cms.model.CmsCategory;
import com.example.aikef.cms.service.CmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/cms")
@RequiredArgsConstructor
public class PublicCmsController {

    private final CmsService cmsService;

    @GetMapping("/articles")
    public ResponseEntity<Page<CmsArticle>> getArticles(
            @RequestParam CmsArticleType type,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(cmsService.getPublicArticles(type, pageable));
    }

    @GetMapping("/articles/list")
    public List<CmsArticle> getArticlesList(
            @RequestParam CmsArticleType type) {
        return cmsService.getPublicArticlesList(type);
    }
    
    @GetMapping("/docs/{category}")
    public ResponseEntity<List<CmsArticle>> getDocsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(cmsService.getPublicDocsByCategory(category));
    }

    @GetMapping("/article/{slug}")
    public ResponseEntity<?> getArticleBySlug(@PathVariable String slug) {
        return cmsService.getPublicArticleBySlug(slug)
                .map(article -> {
                    try {
                        cmsService.incrementViewCount(slug);
                    } catch (Exception e) {
                        // ignore view count error
                    }
                    return ResponseEntity.ok(article);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/categories")
    public List<CmsCategory> getAllCategories() {
        return cmsService.getAllCategories();
    }
}
