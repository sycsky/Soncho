# MessageDirection

## Class Profile
`MessageDirection` is an enumeration that defines the direction of a message flow within the system.

## Values
- `INBOUND`: Message coming from an external channel to the system (Customer -> Agent).
- `OUTBOUND`: Message sent from the system to an external channel (Agent -> Customer).

## Usage Guide
Used in `ChannelMessage` and `ChannelAdapter` to determine how to process a message.

```java
if (direction == MessageDirection.OUTBOUND) {
    // Send to external API
}
```

## Source Link
[MessageDirection.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/channel/MessageDirection.java)
