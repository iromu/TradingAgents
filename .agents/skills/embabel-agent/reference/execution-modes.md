# Execution Modes Reference

Embabel provides two orthogonal modes of operation: **process execution mode** (how actions within an agent run) and **autonomy mode** (how the LLM selects and assembles agents).

## Process Execution

### SIMPLE (default)

Sequential: one action at a time. Most agents use this mode.

```yaml
embabel:
  agent:
    platform:
      process-type: SIMPLE
```

### CONCURRENT

All achievable actions run in parallel. Use for independent sub-tasks with fan-out/fan-in patterns.

```yaml
embabel:
  agent:
    platform:
      process-type: CONCURRENT
```

## Autonomy Modes

### Closed Mode

The LLM picks one agent; that agent runs in isolation. Use when you want strict agent boundaries.

```java
autonomy.chooseAndRunAgent(userIntent, ProcessOptions.DEFAULT, agentPlatform, bindings);
```

### Open Mode

The LLM picks a goal and assembles an agent from all available actions. Use for maximum flexibility.

```java
autonomy.chooseAndAccomplishGoal(ProcessOptions.DEFAULT, approver, agentPlatform, bindings);
```

## Key Points

- Execution mode (SIMPLE/CONCURRENT) controls how actions run within an agent
- Autonomy mode (CLOSED/OPEN) controls how the LLM selects agents
- These operate at different levels — don't confuse them
- SIMPLE is the default and works for most agents
- CONCURRENT is for independent sub-tasks with fan-out/fan-in patterns
- CLOSED mode gives strict agent boundaries
- OPEN mode gives maximum flexibility by assembling agents from all actions
