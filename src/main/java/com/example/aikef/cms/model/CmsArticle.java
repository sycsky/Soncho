package com.example.aikef.cms.model;

import com.example.aikef.model.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "cms_articles", indexes = {
    @Index(name = "idx_cms_slug", columnList = "slug", unique = true),
    @Index(name = "idx_cms_type", columnList = "type"),
    @Index(name = "idx_cms_status", columnList = "status")
})
public class CmsArticle extends AuditableEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content; // Markdown content

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CmsArticleType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CmsArticleStatus status = CmsArticleStatus.DRAFT;

    @Column(name = "seo_title")
    private String seoTitle;

    @Column(name = "seo_description")
    private String seoDescription;

    @Column(name = "seo_keywords")
    private String seoKeywords; // Comma separated

    @Column(name = "view_count")
    private Long viewCount = 0L;

    // Optional category for docs/blogs
    private String category;
    
    // For documentation ordering
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
