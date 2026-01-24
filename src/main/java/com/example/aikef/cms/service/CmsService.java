package com.example.aikef.cms.service;

import com.example.aikef.cms.model.CmsArticle;
import com.example.aikef.cms.model.CmsArticleStatus;
import com.example.aikef.cms.model.CmsArticleType;
import com.example.aikef.cms.model.CmsCategory;
import com.example.aikef.cms.repository.CmsArticleRepository;
import com.example.aikef.cms.repository.CmsCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CmsService {

    private final CmsArticleRepository articleRepository;
    private final CmsCategoryRepository categoryRepository;

    public Page<CmsArticle> getPublicArticles(CmsArticleType type, Pageable pageable) {
        return articleRepository.findByTypeAndStatus(type, CmsArticleStatus.PUBLISHED, pageable);
    }

    public List<CmsArticle> getPublicArticlesList(CmsArticleType type) {
        return articleRepository.findByTypeAndStatusOrderBySortOrderAsc(type, CmsArticleStatus.PUBLISHED);
    }
    
    public List<CmsArticle> getPublicDocsByCategory(String category) {
        return articleRepository.findByCategoryAndTypeAndStatusOrderBySortOrderAsc(category, CmsArticleType.DOCUMENTATION, CmsArticleStatus.PUBLISHED);
    }

    public Optional<CmsArticle> getPublicArticleBySlug(String slug) {
        return articleRepository.findBySlugAndStatus(slug, CmsArticleStatus.PUBLISHED);
    }

    @Transactional
    public CmsArticle createArticle(CmsArticle article) {
        if (articleRepository.existsBySlug(article.getSlug())) {
            throw new IllegalArgumentException("Slug already exists: " + article.getSlug());
        }
        return articleRepository.save(article);
    }

    @Transactional
    public CmsArticle updateArticle(UUID id, CmsArticle updated) {
        CmsArticle existing = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found: " + id));
        
        existing.setTitle(updated.getTitle());
        existing.setContent(updated.getContent());
        existing.setType(updated.getType());
        existing.setStatus(updated.getStatus());
        existing.setSeoTitle(updated.getSeoTitle());
        existing.setSeoDescription(updated.getSeoDescription());
        existing.setSeoKeywords(updated.getSeoKeywords());
        existing.setCategory(updated.getCategory());
        existing.setSortOrder(updated.getSortOrder());
        
        // Update slug only if changed and not taken
        if (!existing.getSlug().equals(updated.getSlug())) {
             if (articleRepository.existsBySlug(updated.getSlug())) {
                throw new IllegalArgumentException("Slug already exists: " + updated.getSlug());
            }
            existing.setSlug(updated.getSlug());
        }

        return articleRepository.save(existing);
    }

    @Transactional
    public void deleteArticle(UUID id) {
        articleRepository.deleteById(id);
    }

    @Transactional
    public void incrementViewCount(String slug) {
        articleRepository.findBySlug(slug).ifPresent(article -> {
            article.setViewCount(article.getViewCount() + 1);
            articleRepository.save(article);
        });
    }
    
    // Admin methods
    public Page<CmsArticle> getAllArticles(Pageable pageable) {
        return articleRepository.findAll(pageable);
    }

    // Category Methods
    public List<CmsCategory> getAllCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc();
    }

    @Transactional
    public CmsCategory createCategory(CmsCategory category) {
        if (categoryRepository.existsBySlug(category.getSlug())) {
            throw new IllegalArgumentException("Category slug already exists: " + category.getSlug());
        }
        category.setSortOrder((int) categoryRepository.count());
        return categoryRepository.save(category);
    }

    @Transactional
    public CmsCategory updateCategory(UUID id, CmsCategory updated) {
        CmsCategory existing = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        
        existing.setName(updated.getName());
        
        // Update slug if changed and check uniqueness
        if (updated.getSlug() != null && !existing.getSlug().equals(updated.getSlug())) {
             if (categoryRepository.existsBySlug(updated.getSlug())) {
                throw new IllegalArgumentException("Slug already exists: " + updated.getSlug());
            }
            existing.setSlug(updated.getSlug());
        }
        
        return categoryRepository.save(existing);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        categoryRepository.deleteById(id);
    }

    @Transactional
    public void reorderCategories(List<UUID> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            final int index = i;
            categoryRepository.findById(id).ifPresent(cat -> {
                cat.setSortOrder(index);
                categoryRepository.save(cat);
            });
        }
    }

    @Transactional
    public void reorderArticles(List<UUID> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            final int index = i;
            articleRepository.findById(id).ifPresent(article -> {
                article.setSortOrder(index);
                articleRepository.save(article);
            });
        }
    }
}
