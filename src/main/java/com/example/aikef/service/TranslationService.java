package com.example.aikef.service;

import com.example.aikef.config.TranslationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.TranslateTextRequest;
import software.amazon.awssdk.services.translate.model.TranslateTextResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 翻译服务
 * 使用 AWS Translate 进行多语言翻译
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationService {

    private final TranslateClient translateClient;
    private final TranslationConfig translationConfig;

    /**
     * 检测文本语言
     *
     * @param text 要检测的文本
     * @return 检测到的语言代码（如 zh, en, ja），如果检测失败返回 null
     */
    public String detectLanguage(String text) {
        if (!isEnabled() || text == null || text.isBlank()) {
            return null;
        }

        try {
            // AWS Translate 需要使用 Comprehend 来检测语言
            // 但 TranslateClient 也支持 auto 源语言检测
            // 这里我们通过翻译请求的方式检测语言
            TranslateTextRequest request = TranslateTextRequest.builder()
                    .text(text.length() > 500 ? text.substring(0, 500) : text)
                    .sourceLanguageCode("auto")
                    .targetLanguageCode("en") // 翻译到英文只是为了检测源语言
                    .build();

            TranslateTextResponse response = translateClient.translateText(request);
            String detectedLanguage = response.sourceLanguageCode();
            
            log.debug("检测到语言: text={}, language={}", 
                    text.length() > 30 ? text.substring(0, 30) + "..." : text, 
                    detectedLanguage);
            
            return detectedLanguage;
        } catch (Exception e) {
            log.warn("语言检测失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 翻译文本到指定语言
     *
     * @param text           要翻译的文本
     * @param sourceLanguage 源语言代码（如 zh, en, ja），可以为 "auto" 自动检测
     * @param targetLanguage 目标语言代码
     * @return 翻译后的文本，如果翻译失败返回原文
     */
    public String translate(String text, String sourceLanguage, String targetLanguage) {
        if (!isEnabled() || text == null || text.isBlank()) {
            return text;
        }

        // 源语言和目标语言相同，无需翻译
//        if (sourceLanguage != null && sourceLanguage.equalsIgnoreCase(targetLanguage)) {
//            return text;
//        }

        try {
            TranslateTextRequest request = TranslateTextRequest.builder()
                    .text(text)
                    .sourceLanguageCode("auto")
                    .targetLanguageCode(targetLanguage)
                    .build();

            TranslateTextResponse response = translateClient.translateText(request);
            String translatedText = response.translatedText();
            
            log.debug("翻译成功: {} -> {}, text={}", 
                    sourceLanguage != null ? sourceLanguage : "auto", 
                    targetLanguage,
                    text.length() > 30 ? text.substring(0, 30) + "..." : text);
            
            return translatedText;
        } catch (Exception e) {
            log.error("翻译失败: sourceLanguage={}, targetLanguage={}, error={}", 
                    sourceLanguage, targetLanguage, e.getMessage());
            return text;
        }
    }

    /**
     * 将文本翻译成所有配置的目标语言
     * 使用并行调用优化性能
     *
     * @param text           要翻译的文本
     * @param sourceLanguage 源语言代码，可以为 null（自动检测）
     * @return 翻译结果 Map，key 为语言代码，value 为翻译后的文本
     */
    public Map<String, String> translateToAllTargetLanguages(String text, String sourceLanguage) {
        // 使用线程安全的 Map
        Map<String, String> translations = Collections.synchronizedMap(new HashMap<>());
        
        if (!isEnabled() || text == null || text.isBlank()) {
            return new HashMap<>();
        }

        // 如果没有提供源语言，先检测
        String actualSourceLanguage = sourceLanguage;
        if (actualSourceLanguage == null || actualSourceLanguage.isBlank()) {
            actualSourceLanguage = detectLanguage(text);
        }

        // 保存原文和源语言信息
        if (actualSourceLanguage != null) {
            translations.put("sourceLanguage", actualSourceLanguage);
        }
        translations.put("originalText", text);

        final String finalSourceLanguage = actualSourceLanguage;
        List<String> targetLanguages = translationConfig.getTargetLanguageCodes();
        
        // 使用 CompletableFuture 并行执行翻译任务
        List<CompletableFuture<Void>> futures = targetLanguages.stream()
            .map(targetLang -> CompletableFuture.runAsync(() -> {
                String translated = translate(text, finalSourceLanguage, targetLang);
                translations.put(targetLang, translated);
            }))
            .collect(Collectors.toList());

        // 如果源语言不是系统默认语言，也翻译到系统默认语言
        String systemLang = translationConfig.getDefaultSystemLanguage();
        if (systemLang != null && !targetLanguages.contains(systemLang)) {
             futures.add(CompletableFuture.runAsync(() -> {
                 // 再次检查是否同源，避免不必要的翻译调用
                 if (!isSameLanguage(finalSourceLanguage, systemLang)) {
                     String translatedToSystem = translate(text, finalSourceLanguage, systemLang);
                     translations.put(systemLang, translatedToSystem);
                 }
             }));
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("并行批量翻译完成: sourceLanguage={}, targetCount={}", actualSourceLanguage, translations.size() - (actualSourceLanguage != null ? 2 : 1));
        // 返回普通 HashMap 以避免序列化问题（虽然 synchronizedMap 也是 Map，但为了保险）
        return new HashMap<>(translations);
    }

    /**
     * 翻译消息并生成 translationData
     * 用于在保存消息前进行翻译处理
     *
     * @param text             消息文本
     * @param customerLanguage 客户使用的语言（如果已知）
     * @return translationData Map，包含所有翻译结果
     */
    public Map<String, Object> translateMessage(String text, String customerLanguage) {
        Map<String, Object> translationData = new HashMap<>();
        
        if (!isEnabled() || text == null || text.isBlank()) {
            return translationData;
        }

        try {
            Map<String, String> translations = translateToAllTargetLanguages(text, customerLanguage);
            
            // 转换为 Object Map
            translationData.putAll(translations);
            
            return translationData;
        } catch (Exception e) {
            log.error("翻译消息失败: {}", e.getMessage());
            return translationData;
        }
    }

    /**
     * 判断两个语言代码是否表示同一种语言
     * 例如 zh 和 zh-CN, zh-TW 都是中文系列
     */
    private boolean isSameLanguage(String lang1, String lang2) {
        if (lang1 == null || lang2 == null) return false;
        
        // 完全匹配
        if (lang1.equalsIgnoreCase(lang2)) return true;
        
        // 提取主语言代码进行比较（如 zh-CN -> zh）
        String main1 = lang1.contains("-") ? lang1.split("-")[0] : lang1;
        String main2 = lang2.contains("-") ? lang2.split("-")[0] : lang2;
        
        // 对于中文的特殊处理：zh-CN 和 zh-TW 视为不同语言
        if (main1.equalsIgnoreCase("zh") && main2.equalsIgnoreCase("zh")) {
            return lang1.equalsIgnoreCase(lang2);
        }
        
        return main1.equalsIgnoreCase(main2);
    }

    /**
     * 检查翻译服务是否启用
     */
    public boolean isEnabled() {
        return translationConfig.isEnabled() && translateClient != null;
    }

    /**
     * 获取默认系统语言
     */
    public String getDefaultSystemLanguage() {
        return translationConfig.getDefaultSystemLanguage();
    }

    /**
     * 获取所有支持的目标语言
     */
    public List<TranslationConfig.TargetLanguage> getSupportedLanguages() {
        return translationConfig.getTargetLanguages();
    }
}

