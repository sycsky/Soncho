# ChannelInboundRequest

## 1. Class Profile
- **Class Name**: `ChannelInboundRequest`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object / API Request Body
- **Purpose**: Represents an incoming message payload from an external channel (e.g., a custom chat widget or third-party integration).

## 2. Method Deep Dive
### Fields
- `conversationId`: Unique identifier for the conversation on the external platform.
- `senderId`: Unique identifier for the user on the external platform.
- `content`: The text content of the message.
- `metadata`: Arbitrary key-value pairs for additional context (e.g., user browser info, current page).
- `mentions`: List of user/agent IDs mentioned in the message.

### Methods
- `toChannelMessage(Channel Channel)`: Converts this request into a standardized `ChannelMessage` domain object.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.dto.ChannelMessage`: The target domain object.
  - `com.example.aikef.model.Channel`: Enum representing the communication channel.
  - `com.example.aikef.channel.MessageDirection`: Enum for message direction.

## 4. Usage Guide
Used by `ChannelController` (or similar entry points) to receive messages.
- **Webhook Handling**: When an external system posts a message to the agent system, it uses this format.
- **Validation**: Fields like `conversationId`, `senderId`, and `content` are mandatory (`@NotBlank`).

## 5. Source Link
[ChannelInboundRequest.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/ChannelInboundRequest.java)
