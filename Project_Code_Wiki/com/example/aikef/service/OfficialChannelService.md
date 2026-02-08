# OfficialChannelService

## Class Profile
- **Package**: `com.example.aikef.service`
- **Type**: Class
- **Description**: Service for managing configurations of Official Channels (e.g., WhatsApp Business, WeChat).
- **Key Features**:
  - Config CRUD.
  - JSON Config Serialization/Deserialization.
  - Channel Toggling.

## Method Deep Dive

### `saveOrUpdateConfig`
- **Description**: Creates or updates a channel configuration.
- **Signature**: `public OfficialChannelConfig saveOrUpdateConfig(ChannelType channelType, Map<String, Object> configData, ...)`
- **Logic**:
  1. Checks for existing config by `channelType`.
  2. Serializes `configData` map to JSON string.
  3. Sets Webhook URL (standardized pattern).
  4. Saves to repository.

### `toggleChannel`
- **Description**: Enables or disables a channel.
- **Signature**: `public OfficialChannelConfig toggleChannel(ChannelType channelType, boolean enabled)`
- **Logic**: Updates `enabled` flag.

### `parseConfigJson`
- **Description**: Helper to parse the stored JSON config back to a Map.
- **Signature**: `public Map<String, Object> parseConfigJson(OfficialChannelConfig config)`
- **Logic**: Uses `ObjectMapper` to deserialize.

## Dependency Graph
- **Injected Services**:
  - `OfficialChannelConfigRepository`
  - `ObjectMapper`: For JSON handling.
- **DTOs**:
  - `OfficialChannelConfig`

## Usage Guide
```java
officialChannelService.saveOrUpdateConfig(
    ChannelType.WHATSAPP_OFFICIAL,
    Map.of("token", "xyz"),
    "secret",
    true,
    "Main WhatsApp",
    null
);
```
