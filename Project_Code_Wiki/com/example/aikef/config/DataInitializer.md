# DataInitializer

## Class Profile
`DataInitializer` is a Spring Boot `CommandLineRunner` that runs on application startup. It is responsible for ensuring the system has a minimal viable configuration, specifically creating the default "Administrator" role and the default "Admin" user if they don't exist.

## Method Deep Dive

### `run(String... args)`
- **Logic**:
    1.  Checks/Creates "Administrator" Role.
    2.  Checks if `admin@nexus.com` exists.
    3.  If not, creates it with default password `Admin@123`.
    4.  If exists, checks if password matches default; if not (and logic implies reset), it resets it (Note: Current logic resets it if *not* matching, which might be intended to enforce a known state in dev/test, but dangerous in prod. Be careful).

## Dependency Graph
- `AgentRepository`
- `RoleRepository`
- `PasswordEncoder`

## Usage Guide
Automatic execution on startup.

## Source Link
[DataInitializer.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/config/DataInitializer.java)
