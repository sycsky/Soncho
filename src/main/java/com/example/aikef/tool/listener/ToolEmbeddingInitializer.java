package com.example.aikef.tool.listener;

import com.example.aikef.tool.model.AiTool;
import com.example.aikef.tool.repository.AiToolRepository;
import com.example.aikef.tool.service.AiToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 应用启动监听器：检查并修复缺失向量的工具
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolEmbeddingInitializer {

    private final AiToolRepository toolRepository;
    private final AiToolService aiToolService;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initToolEmbeddings() {
//        log.info("开始检查工具向量化状态...");
//
//        List<AiTool> toolsMissingEmbedding = toolRepository.findByEmbeddingIsNull();
//
//        if (toolsMissingEmbedding.isEmpty()) {
//            log.info("所有工具均已向量化，无需处理。");
//            return;
//        }
//
//        log.info("发现 {} 个工具缺少向量，开始生成...", toolsMissingEmbedding.size());
//
//        int successCount = 0;
//        for (AiTool tool : toolsMissingEmbedding) {
//            try {
//                // 调用 service 方法生成并保存
//                aiToolService.generateAndSaveEmbedding(tool);
//                successCount++;
//                // 简单的限流，避免触发 API 限制
//                Thread.sleep(200);
//            } catch (Exception e) {
//                log.error("工具向量化失败: id={}, name={}", tool.getId(), tool.getName(), e);
//            }
//        }
//
//        log.info("工具向量化补全完成。成功: {}/{}", successCount, toolsMissingEmbedding.size());
    }
}
