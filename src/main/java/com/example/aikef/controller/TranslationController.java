package com.example.aikef.controller;

import com.example.aikef.config.TranslationConfig;
import com.example.aikef.service.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 翻译服务 API
 * 提供翻译配置查询和手动翻译功能
 */
@RestController
@RequestMapping("/api/v1/translation")
@RequiredArgsConstructor
public class TranslationController {

    private final TranslationService translationService;
    private final TranslationConfig translationConfig;

    /**
     * 获取翻译服务配置信息
     * GET /api/v1/translation/config
     * 
     * @return 翻译服务配置，包含是否启用、支持的语言列表、默认系统语言
     */
    @GetMapping("/config")
    public Map<String, Object> getTranslationConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", translationService.isEnabled());
        config.put("defaultSystemLanguage", translationConfig.getDefaultSystemLanguage());
        config.put("supportedLanguages", translationConfig.getTargetLanguages());
        return config;
    }

    /**
     * 获取支持的目标语言列表
     * GET /api/v1/translation/languages
     * 
     * @return 语言列表，每个语言包含 code 和 name
     */
    @GetMapping("/languages")
    public List<TranslationConfig.TargetLanguage> getSupportedLanguages() {
        return translationService.getSupportedLanguages();
    }

    /**
     * 检测文本语言
     * POST /api/v1/translation/detect
     * 
     * @param request 包含 text 字段的请求体
     * @return 检测到的语言代码
     */
    @PostMapping("/detect")
    public Map<String, String> detectLanguage(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        String detectedLanguage = translationService.detectLanguage(text);
        
        Map<String, String> result = new HashMap<>();
        result.put("detectedLanguage", detectedLanguage);
        return result;
    }

    /**
     * 翻译文本
     * POST /api/v1/translation/translate
     * 
     * @param request 请求体，包含：
     *                - text: 要翻译的文本（必填）
     *                - sourceLanguage: 源语言代码（可选，默认自动检测）
     *                - targetLanguage: 目标语言代码（必填）
     * @return 翻译结果
     */
    @PostMapping("/translate")
    public Map<String, String> translate(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        String sourceLanguage = request.get("sourceLanguage");
        String targetLanguage = request.get("targetLanguage");
        
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text 不能为空");
        }
        if (targetLanguage == null || targetLanguage.isBlank()) {
            throw new IllegalArgumentException("targetLanguage 不能为空");
        }
        
        String translatedText = translationService.translate(text, sourceLanguage, targetLanguage);
        
        Map<String, String> result = new HashMap<>();
        result.put("originalText", text);
        result.put("translatedText", translatedText);
        result.put("sourceLanguage", sourceLanguage != null ? sourceLanguage : "auto");
        result.put("targetLanguage", targetLanguage);
        return result;
    }

    /**
     * 将文本翻译成所有配置的目标语言
     * POST /api/v1/translation/translate-all
     * 
     * @param request 请求体，包含：
     *                - text: 要翻译的文本（必填）
     *                - sourceLanguage: 源语言代码（可选，默认自动检测）
     * @return 所有目标语言的翻译结果
     */
    @PostMapping("/translate-all")
    public Map<String, String> translateToAll(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        String sourceLanguage = request.get("sourceLanguage");
        
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text 不能为空");
        }
        
        return translationService.translateToAllTargetLanguages(text, sourceLanguage);
    }
}

