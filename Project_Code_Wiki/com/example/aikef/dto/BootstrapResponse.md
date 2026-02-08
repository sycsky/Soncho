# BootstrapResponse

## 1. Class Profile
- **Class Name**: `BootstrapResponse`
- **Package**: `com.example.aikef.dto`
- **Type**: `Record` (DTO)
- **Role**: Data Transfer Object
- **Purpose**: Encapsulates all necessary initial data required by the frontend application upon startup. This reduces the number of API calls needed to initialize the application state.

## 2. Method Deep Dive
### Fields
- `sessionGroups`: List of session groups available to the agent.
- `agent`: The currently logged-in agent's profile.
- `roles`: List of available roles (for admin purposes or permission checks).
- `quickReplies`: List of pre-configured quick replies.
- `knowledgeBase`: List of available knowledge bases.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.dto.SessionGroupDto`
  - `com.example.aikef.dto.AgentDto`
  - `com.example.aikef.dto.RoleDto`
  - `com.example.aikef.dto.QuickReplyDto`
  - `com.example.aikef.dto.KnowledgeEntryDto`

## 4. Usage Guide
Returned by `BootstrapService.getBootstrapData()`.
- **Frontend Initialization**: The frontend calls `/api/bootstrap` once after login to populate stores (Vuex/Pinia/Redux) with user info, settings, and configuration data.

## 5. Source Link
[BootstrapResponse.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/dto/BootstrapResponse.java)
