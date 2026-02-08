# Class Profile: RandomAgentAssignmentStrategy

**File Path**: `com/example/aikef/service/strategy/RandomAgentAssignmentStrategy.java`
**Type**: Component (`@Component`)
**Purpose**: A concrete implementation of `AgentAssignmentStrategy` that assigns customers to agents randomly. It prioritizes "ONLINE" agents but falls back to any available agent if none are online.

# Method Deep Dive

## `assignPrimaryAgent(...)`
- **Logic**:
  1. Queries `AgentRepository` for agents with `status = ONLINE`.
  2. If no online agents found, queries for all agents (excluding `OFFLINE`).
  3. If still no agents, throws exception.
  4. Randomly selects one agent from the list.

# Dependency Graph

**Core Dependencies**:
- `AgentRepository`: For fetching agent lists.
- `AgentStatus`: Enum for filtering.

**Key Imports**:
```java
import com.example.aikef.repository.AgentRepository;
import org.springframework.stereotype.Component;
import java.util.Random;
```

# Usage Guide

This strategy is automatically picked up by Spring's component scan and can be injected into the `CustomerService` or `ChatSessionService`.

# Source Link
[RandomAgentAssignmentStrategy.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/service/strategy/RandomAgentAssignmentStrategy.java)
