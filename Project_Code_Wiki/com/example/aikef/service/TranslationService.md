# TranslationService

## 1. Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: `Class`
- **Modifiers**: `public`, `Service`, `Slf4j`
- **Extends**: `None`
- **Implements**: `None`
- **Description**: 多语言翻译服务。基于 AWS Translate 实现，提供文本的语言检测、单向翻译和批量多语言广播翻译功能。支持系统内消息的自动翻译流转。

## 2. Method Deep Dive

### `detectLanguage`
- **Signature**: `public String detectLanguage(String text)`
- **Description**: 检测文本的源语言代码（如 "zh", "en"）。

### `translate`
- **Signature**: `public String translate(String text, String sourceLanguage, String targetLanguage)`
- **Description**: 将文本从源语言翻译到目标语言。如果源语言为 "auto"，则自动检测。

### `translateToAllTargetLanguages`
- **Signature**: `public Map<String, String> translateToAllTargetLanguages(String text, String sourceLanguage)`
- **Description**: 将文本翻译为系统配置的所有目标语言。
- **Returns**: Map，Key 为语言代码，Value 为翻译结果。

### `translateMessage`
- **Signature**: `public Map<String, Object> translateMessage(String text, String customerLanguage)`
- **Description**: 专门用于消息处理的封装方法，生成用于存储的 `translationData` 结构。

## 3. Dependency Graph
- **Injected Dependencies**:
  - `TranslateClient`: AWS Translate SDK。
  - `TranslationConfig`: 翻译配置（目标语言列表、开关等）。

## 4. Usage Guide
### 场景：实时翻译
系统配置了支持 EN, ES, JP 三种语言。
当客户发送中文消息 "你好" 时：
1. `detectLanguage` 识别为 "zh"。
2. `translateToAllTargetLanguages` 生成：
   - en: "Hello"
   - es: "Hola"
   - jp: "こんにちは"
3. 这些翻译结果随消息一起存储和下发，前端根据当前用户的语言设置显示对应的翻译内容。
