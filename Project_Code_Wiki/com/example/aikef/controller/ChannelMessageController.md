# Class Profile: ChannelMessageController

**File Path**: `com/example/aikef/controller/ChannelMessageController.java`
**Type**: Controller (`@RestController`)
**Purpose**: The entry point for receiving external messages from various communication channels (e.g., WeChat, WhatsApp). It normalizes inbound requests into `ChannelMessage` objects, delegates processing to the `AiAssistantService`, and routes the response back to the appropriate channel.

# Method Deep Dive

## `receive(Channel Channel, ChannelInboundRequest request)`
- **Endpoint**: `POST /api/v1/channels/{channel}/messages`
- **Description**: Receives a message from a specific channel.
- **Logic**:
  1. Converts the `ChannelInboundRequest` DTO into a normalized `ChannelMessage` domain object.
  2. Calls `aiAssistantService.reply(inboundMessage)` to generate a response (via AI or logic).
  3. Constructs an outbound `ChannelMessage` from the response.
  4. Uses `ChannelRouter` to dispatch the outbound message back to the external platform.
  5. Returns the `ChatResponse`.

# Dependency Graph

**Core Dependencies**:
- `AiAssistantService`: Core service for processing messages and generating replies.
- `ChannelRouter`: Router for sending messages to external channels.
- `ChannelMessage`, `ChannelInboundRequest`, `ChatResponse`: Data transfer objects.
- `com.example.aikef.model.Channel`: Enum defining supported channels.

**Key Imports**:
```java
import com.example.aikef.channel.ChannelRouter;
import com.example.aikef.service.AiAssistantService;
import com.example.aikef.model.Channel;
import org.springframework.web.bind.annotation.*;
```

# Usage Guide

This endpoint acts as a webhook receiver for external platforms.

## Example: Receiving a message from WhatsApp
`POST /api/v1/channels/WHATSAPP/messages`
```json
{
  "senderId": "+1234567890",
  "content": "Where is my order?",
  "messageId": "msg_123_abc",
  "timestamp": 1678901234
}
```

# Source Link
[ChannelMessageController.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/controller/ChannelMessageController.java)
