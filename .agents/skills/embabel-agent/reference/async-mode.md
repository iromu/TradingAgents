# Async Mode & Threading Reference

Threading model configuration for Embabel agents. See SKILL.md for the core workflow.

## Threading Model

Embabel uses a dedicated executor for all agent operations with an inheritance-based threading model.

> **Upgrade Note**: Prior to this version, Embabel set `spring.threads.virtual.enabled=true` by default. This has been removed to avoid interfering with host application threading. Embabel now inherits from `spring.threads.virtual.enabled` (defaults to platform threads if not set).

## Configuration Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `embabel.agent.platform.threading.override` | Boolean | `false` | Flip threading model (virtual↔platform) |
| `embabel.agent.platform.threading.shared` | Boolean | `false` | Share app's `applicationTaskExecutor` when models match |

```properties
# Flip inherited threading model (virtual-to-platform, platform-to-virtual)
embabel.agent.platform.threading.override=false

# Share app's executor when both use the same threading model
embabel.agent.platform.threading.shared=false
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

## Key Points

- Embabel inherits threading from `spring.threads.virtual.enabled` by default
- Set `override=true` to flip the model (virtual→platform or vice versa)
- Set `shared=true` to share the app's executor when both use the same model
- When models differ, a new isolated executor is always created
- To enable virtual threads: set `spring.threads.virtual.enabled=true`