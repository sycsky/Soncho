package com.example.aikef.cms.repository;

import com.example.aikef.cms.model.CmsArticle;
import com.example.aikef.cms.model.CmsArticleStatus;
import com.example.aikef.cms.model.CmsArticleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import java.util.UUID;

@Repository
public interface CmsArticleRepository extends JpaRepository<CmsArticle, UUID> {
    
    Optional<CmsArticle> findBySlug(String slug);
    
    Optional<CmsArticle> findBySlugAndStatus(String slug, CmsArticleStatus status);
    
    Page<CmsArticle> findByTypeAndStatus(CmsArticleType type, CmsArticleStatus status, Pageable pageable);
    
    List<CmsArticle> findByTypeAndStatusOrderBySortOrderAsc(CmsArticleType type, CmsArticleStatus status);

    List<CmsArticle> findByCategoryAndTypeAndStatusOrderBySortOrderAsc(String category, CmsArticleType type, CmsArticleStatus status);
    
    boolean existsBySlug(String slug);
}
