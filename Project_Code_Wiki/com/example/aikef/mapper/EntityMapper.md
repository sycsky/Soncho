# EntityMapper

## Class Profile
`EntityMapper` is a utility component responsible for converting Database Entities (`Agent`, `ChatSession`, `Message`, etc.) into Data Transfer Objects (DTOs) for API responses. It encapsulates complex mapping logic, such as resolving lazy relations, calculating timestamps, and formatting metadata.

## Method Deep Dive

### `toChatSessionDto(ChatSession session)`
- **Logic**:
    - Converts Customer, Last Message, and Agents.
    - Calculates `lastActive` timestamp.
    - **Note**: This is the "General View" mapper.

### `toChatSessionDtoForAgent(ChatSession session, UUID agentId)`
- **Logic**:
    - Similar to general view but fetches **agent-specific** data like `SessionGroup` mapping (which folder is this session in for this specific agent?).

### `toMessageDto(Message message)`
- **Logic**:
    - Maps attachments and mentions.
    - Handles `agentMetadata`.
    - **Core Memory Reference**: [EntityMapper Workflow Log Injection Removal](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/Project_Code_Wiki/core_memories.xml) - It explicitly *does not* inject workflow logs into metadata anymore.

### `toCustomerDtoFromSession(...)`
- **Logic**:
    - Resolves customer details.
    - Merges `UserProfile` notes/tags if available.
    - Identifies `SpecialCustomer` roles.

## Dependency Graph
- `SessionGroupMappingRepository`: To find agent-specific session groups.
- `MessageRepository`: To find last message.
- `AgentRepository`: To hydrate agent details.
- `ObjectMapper`: For JSON metadata parsing.

## Usage Guide
Inject `EntityMapper` in Controllers or Services to transform entities before returning them.

```java
return entityMapper.toChatSessionDto(session);
```

## Source Link
[EntityMapper.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/mapper/EntityMapper.java)
