# ChannelAdapter

## Class Profile
`ChannelAdapter` is the core interface for the multi-channel message delivery system. It defines the contract that all specific channel implementations (Email, WhatsApp, SMS, etc.) must fulfill. It allows the system to be decoupled from specific channel protocols.

## Method Deep Dive

### `Channel()`
- **Returns**: The `Channel` enum value this adapter supports (e.g., `EMAIL`, `WHATSAPP`).

### `supports(MessageDirection direction)`
- **Returns**: `true` if the adapter can handle the given direction (INBOUND/OUTBOUND).

### `deliver(ChannelMessage message)`
- **Description**: Performs the actual message delivery.

## Usage Guide
Implement this interface to add a new channel.

```java
@Component
public class SmsAdapter implements ChannelAdapter {
    public Channel Channel() { return Channel.SMS; }
    // ...
}
```

## Source Link
[ChannelAdapter.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/channel/ChannelAdapter.java)
