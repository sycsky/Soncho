# ChatSession

## 1. Class Profile
- **Class Name**: `ChatSession`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Persistence Entity
- **Purpose**: Represents a conversation thread between a customer and the system (AI or Agent). It aggregates messages, participants, and status.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `customer`: The customer initiating the chat.
- `status`: Current state (`QUEUED`, `AI_HANDLING`, `AGENT_HANDLING`, `RESOLVED`, `CLOSED`).
- `lastActiveAt`: Timestamp of the last activity (message or status change).
- `primaryAgent`: The main agent responsible for this session (if assigned).
- `supportAgentIds`: List of other agents collaborating on this session.
- `category`: Classification of the session topic.
- `note`: Internal notes for agents.
- `customerLanguage`: Detected or preferred language of the customer.
- `metadata`: Flexible JSON storage for channel-specific or custom data.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Customer`: The customer.
  - `com.example.aikef.model.Agent`: The assigned agent.
  - `com.example.aikef.model.SessionCategory`: The category.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
This is the central entity for the "Inbox" or "Chat" feature.
- **Workflow**: A new message creates a session with `AI_HANDLING`. If the AI fails, it transitions to `QUEUED` or `AGENT_HANDLING`.
- **UI**: The inbox list displays `ChatSession` objects sorted by `lastActiveAt`.

## 5. Source Link
[ChatSession.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/ChatSession.java)
