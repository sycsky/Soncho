# Class Profile: TranslationController

**File Path**: `com/example/aikef/controller/TranslationController.java`
**Type**: Controller (`@RestController`)
**Purpose**: Provides real-time translation and language detection services. It supports both single-target translation and multi-target translation (broadcasting a message in multiple languages), as well as exposing the system's supported language configuration.

# Method Deep Dive

## Configuration
- **`getTranslationConfig()`**: Returns global settings (enabled status, default language).
- **`getSupportedLanguages()`**: Lists available target languages (e.g., en, zh, es).

## Core Functions
- **`detectLanguage(...)`**: Identifies the language of a given text string.
- **`translate(...)`**: Translates text from Source (optional) to Target language.
- **`translateToAll(...)`**: Translates text into *all* configured target languages at once. Useful for broadcasting system announcements or multilingual support.

# Dependency Graph

**Core Dependencies**:
- `TranslationService`: Integration with translation providers (e.g., Google, DeepL, Azure).
- `TranslationConfig`: Configuration properties.

**Key Imports**:
```java
import com.example.aikef.service.TranslationService;
import com.example.aikef.config.TranslationConfig;
import org.springframework.web.bind.annotation.*;
```

# Usage Guide

## Detecting Language
`POST /api/v1/translation/detect`
```json
{ "text": "Hola mundo" }
```
**Response**: `{ "detectedLanguage": "es" }`

## Translating
`POST /api/v1/translation/translate`
```json
{
  "text": "Hello",
  "targetLanguage": "zh"
}
```
**Response**: `{ "translatedText": "你好", ... }`

# Source Link
[TranslationController.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/controller/TranslationController.java)
