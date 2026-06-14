---
title: "OrchestratorAgent not found at runtime"
type: "bug-fix"
status: "active"
source_paths:
  - src/main/java/com/embabel/gekko/util/AgentUtils.java
  - src/main/java/com/embabel/gekko/web/TradingHtmxController.java
  - src/main/java/com/embabel/gekko/agent/OrchestratorAgent.java
updated_at: "2026-06-14"
---

# OrchestratorAgent Not Found at Runtime

## Problem

Calling `/research/plan` threw:

```
java.lang.IllegalStateException: No OrchestratorAgent found. Please ensure it is registered.
```

## Root Cause

`AgentUtils.findAgent()` used `clazz::isInstance` to filter agents:

```java
platform.agents()
    .stream()
    .filter(clazz::isInstance)  // always false
```

This fails because `platform.agents()` returns `com.embabel.agent.core.Agent` instances — immutable data wrappers created by the `@Agent` scanning infrastructure. `OrchestratorAgent` is the **source class** (annotated with `@Agent`), not a superclass of `Agent`. No `Agent` instance is ever an instance of `OrchestratorAgent.class`.

## Fix

Match agents by name instead. The scanning infrastructure sets the agent's name to the source class's simple name (e.g., `"OrchestratorAgent"`):

```java
platform.agents()
    .stream()
    .filter(a -> a.getName().equals(expectedName))
```

## Verification

After the fix, the `/research/plan` endpoint correctly finds `OrchestratorAgent` and starts the research workflow.
