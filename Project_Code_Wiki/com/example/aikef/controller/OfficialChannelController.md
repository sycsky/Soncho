# Class Profile: OfficialChannelController

**File Path**: `com/example/aikef/controller/OfficialChannelController.java`
**Type**: Controller (`@RestController`)
**Purpose**: Manages configurations for official communication channels (e.g., WeChat Official Account, WhatsApp Business, Line). It handles the storage of credentials (AppID, Secret) and webhook settings required to connect with these external platforms.

# Method Deep Dive

## Configuration Management
- **`getAllConfigs()`**: Lists all channel configurations.
- **`getConfig(channelType)`**: Retrieves credentials for a specific channel type.
- **`saveOrUpdateConfig(...)`**: Saves sensitive credentials (AppID, Secret, Tokens).
- **`toggleChannel(...)`**: Enables/Disables a channel integration.
- **`deleteConfig(...)`**: Removes the configuration.

## Metadata
- **`getChannelTypes()`**: Returns a list of supported channel types (WECHAT_OFFICIAL, LINE, WHATSAPP, etc.) for UI dropdowns.

# Dependency Graph

**Core Dependencies**:
- `OfficialChannelService`: Logic for encrypting and saving credentials.
- `OfficialChannelConfig`: Entity model.
- `OfficialChannelConfig.ChannelType`: Enum of supported platforms.

**Key Imports**:
```java
import com.example.aikef.service.OfficialChannelService;
import com.example.aikef.model.OfficialChannelConfig;
import org.springframework.web.bind.annotation.*;
```

# Usage Guide

## Configuring WeChat
`POST /api/v1/official-channels/configs`
```json
{
  "channelType": "WECHAT_OFFICIAL",
  "configData": {
    "appId": "wx123...",
    "appSecret": "abcdef...",
    "token": "mytoken",
    "encodingAESKey": "..."
  },
  "enabled": true
}
```

# Source Link
[OfficialChannelController.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/controller/OfficialChannelController.java)
