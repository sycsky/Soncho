# Customer

## 1. Class Profile
- **Class Name**: `Customer`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Persistence Entity
- **Purpose**: Represents an end-user interacting with the system via various channels (Email, WhatsApp, WeChat, etc.).

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID).
- `name`: Customer's name.
- `primaryChannel`: The main channel this customer uses.
- `email` / `phone`: Contact details.
- `wechatOpenId` / `whatsappId` / `lineId` / `telegramId` / `facebookId`: Channel-specific identifiers.
- `avatarUrl`: Profile picture.
- `location`: Geographic location.
- `notes`: Internal notes about the customer.
- `customFields`: Dynamic JSON map for CRM integration (e.g., "membership_level", "total_spent").
- `active`: Status flag.
- `lastInteractionAt`: Timestamp of last contact.
- `tags`: Manual tags (e.g., "VIP").
- `aiTags`: Auto-generated tags from AI analysis (e.g., "Angry", "Price Sensitive").

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Channel`: Enum for channels.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
This entity is the "User Profile" in the CRM sense.
- **Identity Resolution**: The system tries to match incoming messages to a `Customer` record based on channel IDs (e.g., matching `From: +12345` to `phone`).
- **Context**: Used by the AI to personalize responses (e.g., "Hello [Name]").

## 5. Source Link
[Customer.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/Customer.java)
