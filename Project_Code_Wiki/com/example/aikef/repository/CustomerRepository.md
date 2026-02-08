# CustomerRepository

## 1. Class Profile
- **Class Name**: `CustomerRepository`
- **Package**: `com.example.aikef.repository`
- **Type**: `Interface`
- **Role**: Data Access Object (Repository)
- **Purpose**: Provides database access methods for `Customer` entities, specifically for identity resolution across channels.

## 2. Method Deep Dive
### Query Methods
- `findByEmail(String email)`: Lookup by email.
- `findByPhone(String phone)`: Lookup by phone number.
- `findByWechatOpenId(String openId)`: Lookup by WeChat ID.
- `findByWhatsappId(String whatsappId)`: Lookup by WhatsApp ID.
- ... and so on for other channels.
- `existsByEmail(...)` / `existsByPhone(...)`: duplicacy checks.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Customer`: The entity.
- **External Dependencies**:
  - `org.springframework.data.jpa.repository.JpaRepository`: Base interface.

## 4. Usage Guide
Used by `CustomerService` and webhook handlers.
- **Webhook Handling**: When a message arrives from Telegram with ID `12345`, `findByTelegramId("12345")` is called. If found, the message is linked to that customer. If not, a new `Customer` record is created.

## 5. Source Link
[CustomerRepository.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/repository/CustomerRepository.java)
