# Agent

## 1. Class Profile
- **Class Name**: `Agent`
- **Package**: `com.example.aikef.model`
- **Type**: `Class` (JPA Entity)
- **Role**: Data Model / Persistence Entity
- **Purpose**: Represents a support agent or administrator in the system. Agents can log in, handle chat sessions, and have assigned roles and permissions.

## 2. Method Deep Dive
### Fields
- `id`: Unique identifier (UUID, inherited from AuditableEntity).
- `name`: Display name of the agent.
- `email`: Login email address (unique).
- `passwordHash`: BCrypt hashed password.
- `avatarUrl`: URL to the agent's profile picture.
- `status`: Current availability status (`ONLINE`, `BUSY`, `OFFLINE`).
- `role`: The security role assigned to this agent (Many-to-One).
- `language`: Preferred interface language (e.g., "zh-CN").
- `tenantId`: Multi-tenancy identifier.

## 3. Dependency Graph
- **Internal Dependencies**:
  - `com.example.aikef.model.Role`: Security role definition.
  - `com.example.aikef.model.base.AuditableEntity`: Base class for timestamp auditing.
  - `com.example.aikef.model.enums.AgentStatus`: Status enum.
- **External Dependencies**:
  - `jakarta.persistence.*`: ORM mapping.

## 4. Usage Guide
This is the core user entity for the back-office application.
- **Authentication**: Used by `AgentAuthenticationProvider` to verify credentials.
- **Assignment**: Used by `AgentAssignmentStrategy` to find available agents for incoming chats.

## 5. Source Link
[Agent.java](file:///D:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/model/Agent.java)
