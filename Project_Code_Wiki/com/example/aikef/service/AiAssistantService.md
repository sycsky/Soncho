# AiAssistantService

## 1. 类档案 (Class Profile)
- **功能定义**：AI 助手服务接口，定义了处理消息并生成回复的抽象契约。
- **注解与配置**：
  - `public interface`: 这是一个接口。
- **继承/实现**：
  - 实现类通常为 `SimpleAiAssistantService` 或集成 LangChain 的实现。

## 2. 核心方法详解 (Method Deep Dive)

| 方法名 | 入参/出参 | 逻辑流程 | 特殊处理 |
|--------|-----------|----------|----------|
| `reply` | In: `ChannelMessage`<br>Out: `ChatResponse` | 接收标准化的渠道消息，返回 AI 生成的回复。 | 具体的 AI 逻辑（规则、LLM、工作流）由实现类决定。 |

## 3. 依赖全景 (Dependency Graph)
- 无依赖（纯接口定义）。

## 4. 调用指南 (Usage Guide)
```java
@Autowired
private AiAssistantService aiAssistantService;

ChatResponse response = aiAssistantService.reply(channelMessage);
```
