# DICE Memory — Reference Guide

The `Memory` facade provides a clean interface for agents to access DICE knowledge graph propositions.

## Table of Contents

- [Memory Facade](#memory-facade)
- [Memory Configuration](#memory-configuration)
- [Eager Search Patterns](#eager-search-patterns)
- [Context Scoping](#context-scoping)
- [Integration with LLM Calls](#integration-with-llm-calls)
- [Configuration](#configuration)

## Memory Facade

The `Memory` facade wraps proposition queries and projections into a unified interface for agents:

```java
var memory = Memory.forContext(contextId)
    .withRepository(propositionRepository)
    .withProjector(memoryProjector)
    .withTopic("the user's project context")
    .withEagerSearchAbout(recentConversationText, 10)
    .withEagerTopicSearch(5)
    .withEagerQuery(q -> q.orderedByEffectiveConfidence().withLimit(3))
    .narrowedBy(q -> q.withEntityId("alice-123"));
```

**When to use:** Whenever an agent needs to recall facts, preferences, or events from the knowledge graph during conversation.

## Memory Configuration

| Setting | Method | Description | Default |
|---------|--------|-------------|---------|
| Min confidence | `withMinConfidence(double)` | Minimum effective confidence threshold | `0.5` |
| Default limit | `withDefaultLimit(int)` | Max results per search | `10` |
| Topic | `withTopic(String)` | Memory topic description | `"the user & context"` |
| Eager search | `withEagerSearchAbout(String, int)` | Preload relevant context by text | none |
| Eager topic | `withEagerTopicSearch(int)` | Preload by topic count | none |
| Eager query | `withEagerQuery(PropositionQuery)` | Preload with custom query | none |
| Narrowed by | `narrowedBy(PropositionQuery)` | Filter results | none |

### Eager Search Patterns

**Search by text** — Find propositions similar to a given text:

```java
.withEagerSearchAbout("user's preference for Python over Java", 10)
```

**Search by topic** — Preload top-N most confident propositions:

```java
.withEagerTopicSearch(5)
```

**Custom query** — Preload with full query control:

```java
.withEagerQuery(q -> q
    .orderedByEffectiveConfidence()
    .withLimit(3)
    .withMinEffectiveConfidence(0.7))
```

## Context Scoping

Always scope memory queries by `ContextId` to separate different users, sessions, or domains:

```kotlin
// Kotlin — ContextId is a value class, so construct it directly
val memory = Memory.forContext(ContextId("user-123-session-456"))
    .withRepository(propositionRepository)
    .withProjector(memoryProjector)
```

```java
// Java — value classes are not directly constructible from Java; use the
// strongly-typed builder pattern, which takes the raw string.
var memory = Memory.forContext(contextId)   // obtained from your context
    .withRepository(propositionRepository)
    .withProjector(memoryProjector);
```

`ContextId` is a **Kotlin value class** living in `com.embabel.agent.core` — it is not a DICE type, and
there is **no** `ContextId.of(...)` factory (that appeared in earlier revisions of this guide and has 0
hits upstream). Typical scopes: `ContextId("user-alice-123")`, `ContextId("team-engineering")`,
`ContextId("session-abc")`, `ContextId("batch-2025-01-09")`. From Java, access the value via
`getContextIdValue()`.

**Without context scoping**, propositions from different users/sessions mix together, causing data
leakage and incorrect recall.

### Pre-populating the Agent's Context

Memory can preload the agent's context with relevant propositions — this is the `withEager*` family
above, not a separate "blackboard" API (that term has 0 hits upstream):

```kotlin
Memory.forContext(contextId)
    .withRepository(propositionRepository)
    .withEagerSearchAbout(conversationText, 10)
    .narrowedBy { it.withEntityId("alice-123") }
```

## Integration with LLM Calls

Use the Memory facade in LLM calls via `Ai.withReferences()` — verbatim from `README.md:1770-1793`:

```java
// Java — eager search about recent conversation
var recentContext = new WindowingConversationFormatter(
        SimpleMessageFormatter.INSTANCE, 5, 0
    ).format(conversation);

var memory = Memory.forContext(contextId)
        .withRepository(propositionRepository)
        .withProjector(memoryProjector)
        .withEagerSearchAbout(recentContext, 10);

// Use as a reference — contribution() adds key memories to the prompt,
// tools() provides the search tool
ai.withReferences(memory).respond(...);
```

`withReferences(memory)` puts the key memories into the system prompt and automatically adds the
search tool, so the model can ask for more. `withEagerSearchAbout` is the recommended eager mode for
chat agents — it uses the actual conversation content as the search query, so the preloaded memories
are relevant to what the user is talking about right now.

> Earlier revisions of this guide showed an `@Agent`-annotated `KnowledgeAgent` with an `@Action
> queryKnowledge(UserQuestion, OperationContext)` method returning `context.ai()…creating(Answer.class)
> .fromPrompt(...)`. None of those names occur anywhere in the DICE corpus (`KnowledgeAgent`,
> `UserQuestion`, `queryKnowledge`, `fromPrompt`, `creating(` all return 0 hits) — that was an invented
> agent-framework facade. The attested integration point is `ai.withReferences(memory).respond(...)`.
> Agent-side constructs belong to embabel-agent, not DICE.

## Configuration

**There is no `dice.memory.*` configuration block.** The settings that look "memory-shaped" are the
MCP recall defaults, bound by `DiceMcpProperties` under the **`embabel.dice.mcp`** prefix:

```yaml
embabel:
  dice:
    mcp:
      enabled: false        # default
      min-confidence: 0.5   # minimum effective confidence for recall/list
      default-limit: 10    # max results per recall/list call
```

The `0.5` / `10` values are `DiceMcpProperties.minConfidence` / `.defaultLimit` — they are *not* keys
under a `dice.memory` tree (that prefix matches nothing documented). For everything else, the prefixes
DICE actually binds are `embabel.dice.store.*`, `embabel.dice.collector.*`,
`embabel.dice.source-analyzer` and `dice.security.api-key.*`.

The per-`Memory` knobs are **code-side withers**, not config keys: `withMinConfidence(double)`
(default `0.5`), `withDefaultLimit(int)` (default `10`), `withTopic(String)`, `withEagerSearchAbout`,
`withEagerTopicSearch`, `withEagerQuery`, `narrowedBy`.

## Common Pitfalls

1. **Not setting min-confidence** — Without `withMinConfidence`, low-quality propositions flood the agent's context. Set an appropriate threshold (default `0.5`).
2. **Not scoping by ContextId** — Without context scoping, propositions from different users/sessions mix together. Always use `ContextId`.
3. **Not configuring eager search** — Without `withEagerSearchAbout`, the agent must search on every turn. Preload context for better latency and response quality.
4. **Not limiting eager search results** — Without `withLimit` or `withDefaultLimit`, eager search can return too many propositions, bloating the prompt. Set appropriate limits.
5. **Using Memory without a projector** — The `Memory` facade needs a projector (typically `DefaultMemoryProjector.DEFAULT`) to materialize propositions.
