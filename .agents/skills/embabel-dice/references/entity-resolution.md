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

`EscalatingEntityResolver` climbs a ladder and stops at the cheapest rung that works; the rung it
stopped at comes back as a `ResolutionLevel`:

| Level | How it matches | LLM call |
|---|---|---|
| `EXACT_MATCH` | Exact name match against the repository | No |
| `HEURISTIC_MATCH` | Normalised name, then fuzzy and partial name matching | No |
| `EMBEDDING_MATCH` | High-confidence embedding similarity | No |
| `LLM_VERIFICATION` | One candidate, verified yes/no by an LLM | Yes |
| `LLM_BAKEOFF` | Several candidates, an LLM picks the best | Yes |
| `NO_MATCH` | Nothing matched at any level | — |

Each attempt returns a `LevelResult` carrying the level, the resolution, a confidence, and how many
candidates were considered — that is what you look at when tuning.

Construction is a static factory, **not** a builder chain (verbatim from
`concepts/entity-resolution.adoc`):

```java
// Full chain, including the vector searcher.
var resolver = EscalatingEntityResolver.create(entityRepository, candidateBakeoff);

// Same, minus the vector searcher — for stores with no vector index.
var resolver = EscalatingEntityResolver.withoutVector(entityRepository, candidateBakeoff);
```

Passing `null` for the bake-off means ambiguous cases mint a new entity rather than asking an LLM.
To stop unresolved mentions being invented at all, use `context.withMintNewEntities(false)` — they are
then vetoed rather than minted.

> Earlier revisions of this guide showed `EscalatingEntityResolver.builder()…build()`, a
> `withCandidateSearchers(List.of(...))` wither, and an `EntityRepository` parameter **type**. None of
> those are attested: there is no builder form and no `withCandidateSearchers` wither, and
> `entityRepository` is upstream's *variable name*, not a verified type — so take the parameter type
> from the shipped factory rather than assuming one. `withPromptMode` is likewise unattested, though
> `PromptMode.COMPACT` itself appears in the corpus.

**Resolution flow:**
1. **Exact match** — check by entity ID / exact name
2. **Normalized name match** — check by normalized name (lowercase, trimmed)
3. **Fuzzy match** — check by Levenshtein distance
4. **Embedding match** — vector similarity, no LLM
5. **LLM verification / bakeoff** — only if no cheaper rung matched

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
// The chain above is what `create(...)` assembles for you; there is no public
// wither to hand it a list yourself.
var resolver = EscalatingEntityResolver.create(entityRepository, candidateBakeoff);
```

The searcher class names in the table are attested, but the assembly shown in earlier revisions —
`EscalatingEntityResolver.builder().withRepository(...).withCandidateSearchers(List.of(...))…build()` —
is **not**: there is no builder form and no `withCandidateSearchers` wither, and the individual
searcher constructors (including the `FuzzyNameCandidateSearcher(entityRepository, 0.2, 4, 4)` arity
shown previously) are not documented in this corpus. Use `create(...)` / `withoutVector(...)`, and read
the `com.embabel.dice.resolution` package before constructing a searcher directly.

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
