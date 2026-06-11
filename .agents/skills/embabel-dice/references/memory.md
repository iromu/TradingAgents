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

```java
var contextId = ContextId.of("user-123-session-456");
var memory = Memory.forContext(contextId)
    .withRepository(propositionRepository)
    .withProjector(memoryProjector);
```

**Without context scoping**, propositions from different users/sessions mix together, causing data leakage and incorrect recall.

### Pre-populating Blackboard

Memory can pre-populate the agent's blackboard with relevant propositions:

```java
memory.withEagerSearchAbout(conversationHistory, 10)
    .withEagerQuery(q -> q.mentioningEntity("alice-123").withLimit(5));
```

## Integration with LLM Calls

Use the Memory facade in LLM calls via `Ai.withReferences()`:

```java
ai.withReferences(memory).respond("What do you know about Alice?");
```

The LLM receives relevant propositions as context in its prompt, enabling fact-grounded responses.

### Agent Integration Example

```java
@Agent
public class KnowledgeAgent {

    @Action
    public Answer queryKnowledge(UserQuestion question, OperationContext context) {
        var memory = Memory.forContext(question.contextId())
            .withRepository(propositionRepository)
            .withProjector(memoryProjector)
            .withEagerSearchAbout(question.text(), 5);

        return context.ai()
            .withReferences(memory)
            .creating(Answer.class)
            .fromPrompt("Answer based on known facts: " + question.text());
    }
}
```

## Configuration

Key memory-related properties in `application.yml`:

```yaml
dice:
  memory:
    min-confidence: 0.5
    default-limit: 10
    eager-search:
      enabled: true
      max-results: 20
```

## Common Pitfalls

1. **Not setting min-confidence** — Without `withMinConfidence`, low-quality propositions flood the agent's context. Set an appropriate threshold (default `0.5`).
2. **Not scoping by ContextId** — Without context scoping, propositions from different users/sessions mix together. Always use `ContextId`.
3. **Not configuring eager search** — Without `withEagerSearchAbout`, the agent must search on every turn. Preload context for better latency and response quality.
4. **Not limiting eager search results** — Without `withLimit` or `withDefaultLimit`, eager search can return too many propositions, bloating the prompt. Set appropriate limits.
5. **Using Memory without a projector** — The `Memory` facade needs a projector (typically `DefaultMemoryProjector.DEFAULT`) to materialize propositions.
