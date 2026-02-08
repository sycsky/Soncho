# RestTemplateConfig

## Class Profile
`RestTemplateConfig` provides a configured `RestTemplate` bean for making HTTP requests. It sets custom timeouts to prevent external API calls (like LLM or Channel APIs) from hanging indefinitely.

## Method Deep Dive

### `restTemplate()`
- **Configuration**:
    - Connect Timeout: 10 seconds.
    - Read Timeout: 30 seconds.
- **Bean Name**: `restTemplate`

## Usage Guide
Inject `RestTemplate` wherever HTTP calls are needed.

```java
@Autowired
private RestTemplate restTemplate;
```

## Source Link
[RestTemplateConfig.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/config/RestTemplateConfig.java)
