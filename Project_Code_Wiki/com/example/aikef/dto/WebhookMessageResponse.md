# WebhookMessageResponse

## 1. Class Profile
- **Class Name**: `WebhookMessageResponse`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object / API Response
- **Purpose**: Standard response format for webhook message ingestion endpoints. Tells the external system the result of the message processing.

## 2. Method Deep Dive
### Fields
- `success`: Boolean indicating if the message was accepted.
- `messageId`: The internal ID assigned to the message.
- `sessionId`: The internal session ID the message was routed to.
- `newSession`: Boolean indicating if a new session was created for this message.
- `customerId`: The internal customer ID.
- `errorMessage`: Description of failure (if any).

### Factory Methods
- `success(...)`: Creates a success response.
- `error(String errorMessage)`: Creates an error response.

## 3. Dependency Graph
- **Internal Dependencies**: None.

## 4. Usage Guide
Returned by `WebhookController` endpoints.
- **Ack**: Provides an acknowledgment to the webhook caller that the message has been queued or processed.
- **Context**: Returns created IDs so the external system can map its own IDs to the agent system's IDs.

## 5. Source Link
[WebhookMessageResponse.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/WebhookMessageResponse.java)
