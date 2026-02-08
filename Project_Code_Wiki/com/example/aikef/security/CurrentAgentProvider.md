# CurrentAgentProvider

## Class Profile
`CurrentAgentProvider` is a helper component to easily retrieve the currently authenticated agent from the security context.

## Method Deep Dive

### `currentAgent()`
- **Returns**: `Optional<AgentPrincipal>`.
- **Logic**: Safely retrieves the authentication object from `SecurityContextHolder`, checks if it is an instance of `AgentPrincipal`, and returns it.

## Dependency Graph
- `SecurityContextHolder`.

## Usage Guide
Inject this bean into services to get the current user without manually casting from the security context.

```java
@Autowired
private CurrentAgentProvider currentAgentProvider;

public void doSomething() {
    currentAgentProvider.currentAgent().ifPresent(agent -> {
        // ...
    });
}
```

## Source Link
[CurrentAgentProvider.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/security/CurrentAgentProvider.java)
