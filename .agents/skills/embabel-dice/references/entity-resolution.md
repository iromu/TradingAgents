# DICE Entity Resolution — Reference Guide

Entity resolution maps mentions to canonical entities in the knowledge graph, preventing duplicate entities for the same real-world thing.

## Table of Contents

- [Entity Resolver Types](#entity-resolver-types)
- [Escalating Entity Resolver](#escalating-entity-resolver)
- [Candidate Searchers](#candidate-searchers)
- [Resolution Outcomes](#resolution-outcomes)
- [Entity Resolution Service](#entity-resolution-service)
- [Configuration](#configuration)

## Entity Resolver Types

| Resolver | Strategy | When to Use |
|----------|----------|-------------|
| `EscalatingEntityResolver` | Heuristics → LLM bakeoff | **Recommended** — best accuracy with performance tradeoff |
| `InMemoryEntityResolver` | Exact/normalized name match | Simple cases, no LLM dependency |
| `LlmCandidateBakeoff` | LLM-powered candidate selection | Fallback when heuristics fail |

## Escalating Entity Resolver

Recommended resolver — uses heuristics first, falls back to LLM bakeoff for ambiguous cases.

```java
@Bean
EntityResolver entityResolver(
        EntityRepository entityRepository,
        Ai ai,
        LlmOptions llmOptions) {

    var candidateBakeoff = LlmCandidateBakeoff.builder()
        .withAi(ai)
        .withLlm(llmOptions)
        .withPromptMode(PromptMode.COMPACT)
        .build();

    return EscalatingEntityResolver.builder()
        .withRepository(entityRepository)
        .withCandidateBakeoff(candidateBakeoff)
        .build();
}
```

**Resolution flow:**
1. **Exact match** — Check by entity ID
2. **Normalized name match** — Check by normalized name (lowercase, trimmed)
3. **Fuzzy match** — Check by Levenshtein distance
4. **LLM bakeoff** — If no heuristic match, ask LLM to select best candidate

## Candidate Searchers

DICE provides multiple candidate searchers for entity resolution:

| Searcher | Strategy | Example |
|----------|----------|---------|
| `ByIdCandidateSearcher` | Exact ID match | `"alice-123"` → entity with ID `alice-123` |
| `ByExactNameCandidateSearcher` | Exact name match | `"Alice Smith"` → entity named exactly `"Alice Smith"` |
| `NormalizedNameCandidateSearcher` | Normalized name match | `"  ALICE SMITH  "` → entity named `"Alice Smith"` |
| `PartialNameCandidateSearcher` | Partial name match | `"Alice"` → entity named `"Alice Smith"` |
| `FuzzyNameCandidateSearcher` | Fuzzy name match (Levenshtein) | `"Alic Smith"` → entity named `"Alice Smith"` |
| `VectorCandidateSearcher` | Vector similarity | Embedding-based matching |
| `AgenticCandidateSearcher` | LLM-powered candidate selection | Complex disambiguation |

### Configuring Candidate Searchers

```java
var resolver = EscalatingEntityResolver.builder()
    .withRepository(entityRepository)
    .withCandidateSearchers(List.of(
        new ByIdCandidateSearcher(entityRepository),
        new ByExactNameCandidateSearcher(entityRepository),
        new NormalizedNameCandidateSearcher(entityRepository),
        new FuzzyNameCandidateSearcher(entityRepository, 0.2, 4, 4),
        new VectorCandidateSearcher(entityRepository),
        new AgenticCandidateSearcher(ai, llmOptions)
    ))
    .build();
```

## Resolution Outcomes

| Outcome | When | Action |
|---------|------|--------|
| `NewEntity` | No matching entity found | Create new entity in repository |
| `ExistingEntity` | Match found in repository | Use existing entity ID |
| `ReferenceOnlyEntity` | Known entity (e.g., current user) | Use reference, don't create |
| `VetoedEntity` | Non-creatable type, no match | Skip, don't create |

## Entity Resolution Service

Higher-level service for entity assertion processing:

```java
@Bean
EntityResolutionService entityResolutionService(
        EntityResolver entityResolver,
        EntityRepository entityRepository,
        DataDictionary schema) {

    return new EntityResolutionService(entityResolver, entityRepository, schema);
}

// Usage
var result = entityResolutionService.resolve(new EntityAssertionRequest(
    List.of(new EntityAssertion("Alice Smith", List.of("Person", "Engineer"),
        "Senior backend engineer", Map.of("department", "Platform"))),
    List.of(new RelationshipAssertion("Alice Smith", "Acme Corp", "WORKS_AT",
        "Full-time employee since 2020", Map.of("since", 2020)))
));
```

## Configuration

Key entity resolution properties in `application.yml`:

```yaml
dice:
  resolver:
    in-memory:
      max-distance-ratio: 0.2      # Levenshtein max distance ratio
      min-length-for-fuzzy: 4       # Min name length for fuzzy matching
      min-part-length: 4            # Min part length for partial matching
    escalating:
      heuristic-only: false         # If true, skip LLM bakeoff
```

## Common Pitfalls

1. **Not using an entity resolver** — Without resolution, the same entity mentioned differently creates duplicates. Always configure at least `InMemoryEntityResolver`.
2. **Skipping fuzzy matching** — Without fuzzy matching, "Alice Smith" and "Alic Smith" create separate entities. Use `FuzzyNameCandidateSearcher` with appropriate distance thresholds.
3. **Not configuring max-distance-ratio** — Too high and false positives increase; too low and legitimate matches are missed. Start with `0.2`.
4. **Using `heuristic-only: true` in production** — This skips LLM bakeoff, which may miss matches that heuristics can't detect. Only use for performance-critical paths.
