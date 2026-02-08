# ChannelMessage

## 1. Class Profile
- **Class Name**: `ChannelMessage`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object / Domain Object
- **Purpose**: A standardized internal representation of a message flowing through the multi-channel system, regardless of its source (Web, Email, WhatsApp, etc.).

## 2. Method Deep Dive
### Fields
- `Channel`: The source/destination channel (default: WEB).
- `direction`: INBOUND (from customer) or OUTBOUND (to customer).
- `conversationId`: External conversation ID.
- `senderId`: External sender ID (default: "anonymous").
- `recipientId`: External recipient ID.
- `content`: Message text.
- `metadata`: Additional context.
- `mentions`: Mentioned entities.

### Constructors & Factories
- **Compact Constructor**: Validates `content`, sets defaults for `Channel`, `direction`, `senderId`, and ensures collections are immutable.
- `inbound(...)`: Factory method for creating incoming messages.
- `outbound(...)`: Factory method for creating outgoing messages.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Channel`: Channel enum.
  - `com.example.aikef.channel.MessageDirection`: Direction enum.

## 4. Usage Guide
This is the "lingua franca" of the messaging subsystem.
- **Normalization**: All external requests are converted to `ChannelMessage` before processing.
- **Routing**: The system routes `ChannelMessage` objects to the appropriate handler or workflow.

## 5. Source Link
[ChannelMessage.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/ChannelMessage.java)
