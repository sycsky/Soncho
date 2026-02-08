# TranslationConfig

## Class Profile
`TranslationConfig` configures the AWS Translate service integration. It defines supported languages, default system language, and creates the `TranslateClient`.

## Method Deep Dive

### Properties
- `enabled`: Toggle switch.
- `targetLanguages`: List of supported languages (code/name).
- `defaultSystemLanguage`: Fallback language.

### `translateClient()`
- **Logic**: Creates the AWS `TranslateClient` if enabled and credentials exist.

### `isLanguageSupported(String code)`
- **Logic**: Validates if a language code is in the configured list.

## Usage Guide
Inject `TranslationConfig` to access language lists or `TranslateClient` for translation.

## Source Link
[TranslationConfig.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/config/TranslationConfig.java)
