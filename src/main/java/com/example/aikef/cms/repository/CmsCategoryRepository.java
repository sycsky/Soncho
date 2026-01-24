package com.example.aikef.cms.repository;

import com.example.aikef.cms.model.CmsCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface CmsCategoryRepository extends JpaRepository<CmsCategory, UUID> {
    boolean existsBySlug(String slug);
    List<CmsCategory> findAllByOrderBySortOrderAsc();
}
