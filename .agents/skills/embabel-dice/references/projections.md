# DICE Projections — Reference Guide

Projections materialize high-confidence propositions to typed backends. They are materialized views of the proposition store.

## Table of Contents

- [Projection Types](#projection-types)
- [Vector Projection](#vector-projection)
- [Neo4j Graph Projection](#neo4j-graph-projection)
- [Prolog Projection](#prolog-projection)
- [Memory Projection](#memory-projection)
- [Oracle Projection](#oracle-projection)
- [Projection Policy](#projection-policy)
- [Configuration](#configuration)

## Projection Types

| Projection | Backend | Use Case |
|------------|---------|----------|
| **Vector** | Vector store | Semantic retrieval |
| **Neo4j** | Graph database | Graph traversal, relationship queries |
| **Prolog** | Prolog engine | Inference, rules |
| **Memory** | Agent context | Agentic recall (Memory facade) |
| **Oracle** | NL QA | Natural language question answering |

## Vector Projection

Stores proposition embeddings for semantic similarity search.

```java
@Bean
GraphProjector vectorProjector(VectorStore vectorStore) {
    return new VectorProjector(vectorStore);
}
```

**When to use:** When you need to find semantically similar propositions, e.g., "find all facts about the user's preferences."

**Dependencies:** `embabel-dice-vector`

## Neo4j Graph Projection

Maps propositions to a graph database for relationship traversal.

```java
@Bean
GraphProjector graphProjector() {
    return RelationBasedGraphProjector.builder()
        .withRelations(Relations.empty()
            .withSemantic("is-a", "related-to")
            .withEpisodic("occurred-at", "involved")
            .withProcedural("uses", "requires"))
        .withPolicy(new LenientProjectionPolicy())
        .build();
}
```

**When to use:** When you need graph traversal, relationship queries, or multi-hop inference over propositions.

**Dependencies:** `embabel-dice-neo4j`

### Relation Mapping

The `Relations` configuration maps knowledge types to relationship labels:

| Knowledge Type | Relationships |
|----------------|---------------|
| `SEMANTIC` | `is-a`, `related-to` |
| `EPISODIC` | `occurred-at`, `involved` |
| `PROCEDURAL` | `uses`, `requires` |

## Prolog Projection

Exposes propositions as Prolog facts for rule-based inference.

```java
@Bean
GraphProjector prologProjector(PrologEngine prologEngine, PrologSchema prologSchema) {
    return new PrologProjector(prologEngine, prologSchema);
}
```

**When to use:** When you need logical inference, rule engines, or Prolog-based queries over proposition data.

**Dependencies:** `embabel-dice-prolog`

## Memory Projection

Exposes propositions to the agent's context via the `Memory` facade.

```java
@Bean
GraphProjector memoryProjector() {
    return DefaultMemoryProjector.DEFAULT;
}
```

**When to use:** When agents need to recall propositions during conversation. This is the projection used by the `Memory` facade for agentic recall.

**Dependencies:** `embabel-dice-agent`

## Oracle Projection

Enables natural language question answering over propositions.

```java
@Bean
GraphProjector oracleProjector(OracleService oracleService) {
    return new OracleProjector(oracleService);
}
```

**When to use:** When you want to ask questions in natural language and get answers derived from proposition data.

## Projection Policy

Controls which propositions get projected.

| Policy | Behavior |
|--------|----------|
| `LenientProjectionPolicy` | Projects propositions with any confidence |
| `StrictProjectionPolicy` | Only projects propositions above a confidence threshold |
| `KnowledgeTypeProjectionPolicy` | Projects based on knowledge type (SEMANTIC, EPISODIC, etc.) |

```java
// Strict: only project high-confidence propositions
.withPolicy(new StrictProjectionPolicy(0.7))
```

## Configuration

Key projection-related properties in `application.yml`:

```yaml
dice:
  projections:
    vector:
      enabled: true
      dimension: 1536
    neo4j:
      enabled: true
      uri: bolt://localhost:7687
    prolog:
      enabled: false
    memory:
      enabled: true
    oracle:
      enabled: false
```

## Common Pitfalls

1. **Not configuring projection policies** — Without a policy, all propositions project regardless of confidence. Use `StrictProjectionPolicy` in production.
2. **Missing dependencies** — Each projection type requires its corresponding module (`embabel-dice-neo4j`, `embabel-dice-vector`, etc.).
3. **Not matching relations to knowledge types** — The `Relations` configuration should map relationship labels to the knowledge types you actually use.
