# Class Profile: AgentAssignmentStrategy

**File Path**: `com/example/aikef/service/strategy/AgentAssignmentStrategy.java`
**Type**: Abstract Class
**Purpose**: Defines the contract for agent assignment strategies. Subclasses implement specific logic (e.g., Round Robin, Random, Load Balanced) to determine which agent should handle a new customer session.

# Method Deep Dive

## `assignPrimaryAgent(Customer customer, Channel channel)`
- **Description**: Abstract method to select the main agent responsible for a customer.
- **Returns**: `Agent` entity.

## `assignSupportAgents(...)`
- **Description**: Optional method to assign secondary/support agents.
- **Default**: Returns an empty list.

## `getStrategyName()`
- **Description**: Returns the unique identifier for the strategy (e.g., "RANDOM").

# Dependency Graph

**Core Dependencies**:
- `Agent`, `Customer`, `Channel`: Domain models.

**Key Imports**:
```java
import com.example.aikef.model.Agent;
import java.util.List;
```

# Usage Guide

Extend this class to create new assignment logic.

```java
public class MyStrategy extends AgentAssignmentStrategy {
    @Override
    public Agent assignPrimaryAgent(...) {
        // custom logic
    }
}
```

# Source Link
[AgentAssignmentStrategy.java](file:///d:/ai_agent_work/ai_agent_workflow/ai_kef/src/main/java/com/example/aikef/service/strategy/AgentAssignmentStrategy.java)
