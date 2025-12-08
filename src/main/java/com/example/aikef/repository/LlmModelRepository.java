package com.example.aikef.repository;

import com.example.aikef.model.LlmModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * LLM 模型 Repository
 */
public interface LlmModelRepository extends JpaRepository<LlmModel, UUID> {

    /**
     * 根据编码查找模型
     */
    Optional<LlmModel> findByCode(String code);

    /**
     * 检查编码是否存在
     */
    boolean existsByCode(String code);

    /**
     * 查找所有启用的模型
     */
    List<LlmModel> findByEnabledTrueOrderBySortOrderAsc();

    /**
     * 根据提供商查找启用的模型
     */
    List<LlmModel> findByProviderAndEnabledTrueOrderBySortOrderAsc(String provider);

    /**
     * 查找默认模型
     */
    Optional<LlmModel> findByIsDefaultTrueAndEnabledTrue();

    /**
     * 查找所有模型（按排序）
     */
    List<LlmModel> findAllByOrderBySortOrderAsc();

    /**
     * 根据模型类型查找启用的模型
     */
    List<LlmModel> findByModelTypeAndEnabledTrueOrderBySortOrderAsc(LlmModel.ModelType modelType);

    /**
     * 查找嵌入模型的默认模型
     */
    Optional<LlmModel> findByModelTypeAndIsDefaultTrueAndEnabledTrue(LlmModel.ModelType modelType);
}

