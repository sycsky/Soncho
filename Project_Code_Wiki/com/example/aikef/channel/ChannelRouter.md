# ChannelRouter

## Class Profile
`ChannelRouter` is the dispatcher service for the messaging system. It maintains a registry of all available `ChannelAdapter` beans and routes outgoing `ChannelMessage`s to the correct adapter based on the channel type and message direction.

## Method Deep Dive

### Constructor
- **Logic**: Automatically collects all `ChannelAdapter` beans from the Spring context and groups them by `Channel` type into a map.

### `route(ChannelMessage message)`
- **Description**: Routes a message to the appropriate adapter.
- **Logic**:
    1.  Looks up adapters for `message.Channel()`.
    2.  Filters for an adapter that `supports(message.direction())`.
    3.  Calls `adapter.deliver(message)`.
    4.  Logs a warning if no suitable adapter is found.

## Dependency Graph
- List of `ChannelAdapter` (auto-injected).

## Usage Guide
Inject `ChannelRouter` to send messages without knowing the underlying channel implementation.

```java
@Autowired
private ChannelRouter router;

public void sendMessage(ChannelMessage msg) {
    router.route(msg);
}
```

## Source Link
[ChannelRouter.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/channel/ChannelRouter.java)
