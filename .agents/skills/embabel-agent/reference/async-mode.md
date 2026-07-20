# Async Mode & Threading Reference

Embabel uses a dedicated executor for all agent operations with an inheritance-based threading model.

## Threading Overview

Embabel's threading behavior is controlled by two orthogonal properties that determine both the threading model (virtual vs. platform) and whether the executor is shared or isolated.

**Upgrade note:** Embabel no longer forces `spring.threads.virtual.enabled=true` by default. It inherits the host application's threading model. To enable virtual threads, set it explicitly in your own configuration.

## Virtual Threads

Virtual threads (Java 21+) provide lightweight, many-to-one mapped threads that dramatically reduce the cost of concurrency. Embabel uses them when `spring.threads.virtual.enabled=true` and the override is not flipped.

**When to use virtual threads:**
- I/O-heavy workloads (LLM calls, database queries, HTTP requests)
- High-concurrency scenarios with many concurrent agent actions
- Environments where thread creation cost matters

**When to use platform threads:**
- CPU-bound workloads (heavy computation, data transformation)
- Host application already uses platform threads and you want full sharing
- Java 17–20 environments (virtual threads are unavailable; Embabel falls back automatically)

> **Java 25 note:** On Java 25+, containers with cgroup CPU limits may see `availableProcessors() = 1`, which can serialize ForkJoinPool-based parallelism. Embabel core is safe (see below), but custom code using `CompletableFuture.supplyAsync()` or `Dispatchers.Default` may be affected.

## Configuration Properties

All properties go in `agent-platform.properties` or `application.yml`.

### Properties

| Property | Type | Default | Description |
|---|---|---|---|
| `embabel.agent.platform.threading.override` | Boolean | `false` | Flip the inherited threading model (virtual→platform or platform→virtual) |
| `embabel.agent.platform.threading.shared` | Boolean | `false` | Share the app's `applicationTaskExecutor` when both models match |

### YAML Examples

Enable virtual threads with an isolated executor (common default):

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

Flip to platform threads (e.g., for CPU-bound agents):

```yaml
spring:
  threads:
    virtual:
      enabled: true

embabel:
  agent:
    platform:
      threading:
        override: true   # flip virtual → platform
        shared: false    # keep executor isolated
```

Share the app's executor (only when both use the same model):

```yaml
spring:
  threads:
    virtual:
      enabled: true

embabel:
  agent:
    platform:
      threading:
        override: false  # keep inherited model
        shared: true     # share app's executor
```

## Behavior Matrix

| App Threads | Override | Shared | Embabel Model | Executor Type |
|-------------|----------|--------|---------------|---------------|
| Platform | false | false | Platform (inherit) | Isolated platform executor |
| Platform | false | true | Platform (inherit) | Shared app's platform executor |
| Platform | true | false | Virtual (flip) | Isolated virtual executor |
| Platform | true | true | Virtual (flip) | Isolated virtual executor (models differ) |
| Virtual | false | false | Virtual (inherit) | Isolated virtual executor |
| Virtual | false | true | Virtual (inherit) | Shared app's virtual executor |
| Virtual | true | false | Platform (flip) | Isolated platform executor |
| Virtual | true | true | Platform (flip) | Isolated platform executor (models differ) |

**Rule:** `shared=true` enables executor sharing only when both app and Embabel use the same threading model. When models differ, Embabel always creates an isolated executor.

## Java 25 & Container CPU Limits

Java 25 accurately reads container cgroup CPU limits, which can cause `availableProcessors() = 1` in constrained containers and serialize ForkJoinPool-based parallelism.

**Embabel core is safe** — it uses a dedicated executor via `ExecutorAsyncer`, not `ForkJoinPool.commonPool()`.

If you see serialization issues in custom code, check:

1. `CompletableFuture.supplyAsync()` without an explicit executor
2. Kotlin `Dispatchers.Default` (use `Dispatchers.IO` instead)
3. Spring `applicationTaskExecutor` misconfiguration
4. Third-party libraries depending on `ForkJoinPool.commonPool()`

**Workarounds:**

Set container CPU limits (Kubernetes):

```yaml
resources:
  limits:
    cpu: "4"
  requests:
    cpu: "2"
```

Or override ForkJoinPool parallelism at startup:

```bash
java -Djava.util.concurrent.ForkJoinPool.common.parallelism=4 -jar agent.jar
```
---

*Source: Embabel Agent v1.0.0 documentation*
