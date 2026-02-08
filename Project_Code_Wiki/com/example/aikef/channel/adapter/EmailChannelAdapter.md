# EmailChannelAdapter

## Class Profile
`EmailChannelAdapter` is the specific implementation of `ChannelAdapter` for the **EMAIL** channel. It is responsible for handling outbound messages destined for email recipients. Currently, it logs the delivery action, serving as a placeholder for actual email sending logic (e.g., via SMTP or API).

## Method Deep Dive

### `deliver(ChannelMessage message)`
- **Description**: Delivers an email message.
- **Logic**: Logs the recipient, conversation ID, and content.
- **Support**: Only supports `OUTBOUND` messages.

## Dependency Graph
- None.

## Usage Guide
This component is automatically detected by `ChannelRouter` via Spring's component scanning.

```java
// Logic inside ChannelRouter
if (msg.Channel() == Channel.EMAIL) {
    emailAdapter.deliver(msg);
}
```

## Source Link
[EmailChannelAdapter.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/channel/adapter/EmailChannelAdapter.java)
