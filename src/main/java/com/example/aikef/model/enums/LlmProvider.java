package com.example.aikef.model.enums;

/**
 * LLM 提供商枚举
 */
public enum LlmProvider {
    /**
     * OpenAI（GPT 系列）
     */
    OPENAI("OpenAI", "https://api.openai.com/v1"),
    
    /**
     * Azure OpenAI
     */
    AZURE_OPENAI("Azure OpenAI", null),
    
    /**
     * Ollama（本地模型）
     */
    OLLAMA("Ollama", "http://localhost:11434"),
    
    /**
     * 智谱 AI（GLM 系列）
     */
    ZHIPU("智谱AI", "https://open.bigmodel.cn/api/paas/v4"),
    
    /**
     * 通义千问
     */
    DASHSCOPE("通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
    
    /**
     * 月之暗面（Moonshot/Kimi）
     */
    MOONSHOT("月之暗面", "https://api.moonshot.cn/v1"),
    
    /**
     * DeepSeek
     */
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1"),
    
    /**
     * Google Gemini
     */
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com/v1beta"),
    
    /**
     * 自定义 OpenAI 兼容接口
     */
    CUSTOM("自定义", null);

    private final String displayName;
    private final String defaultBaseUrl;

    LlmProvider(String displayName, String defaultBaseUrl) {
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }
}

