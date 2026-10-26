# DICE Projections — Reference Guide

A projection is a **rebuildable, typed view** over the proposition store. Nothing in a projection is
authoritative: if a projection and the propositions disagree, **the propositions win** — you rebuild
the view rather than editing it. Design your projections so they can be dropped and recreated.

> **Verified against** the `embabel/dice` collection at `main` / `870b9ab`, version
> `com.embabel.dice:dice-parent:0.2.0-SNAPSHOT`. There are **no tags and no releases**, so never
> write "pin v0.2.0". Note the build parent is `com.embabel.build:embabel-build-parent:2.0.0-SNAPSHOT`
> — that `2.0.0` is the *parent*, not DICE.

## Table of Contents

- [The four projections](#the-four-projections)
- [Graph projection](#graph-projection)
- [Prolog projection](#prolog-projection)
- [Memory projection](#memory-projection)
- [Report projection](#report-projection)
- [Decorators and gate decisions](#decorators-and-gate-decisions)
- [Dependencies — there are no per-backend modules](#dependencies--there-are-no-per-backend-modules)
- [Configuration — there is no projections block](#configuration--there-is-no-projections-block)
- [Common Pitfalls](#common-pitfalls)

## The four projections

| Projection | Types | What it produces |
|---|---|---|
| **Graph** | `GraphProjector`, `RelationBasedGraphProjector`, `LlmGraphProjector` | A typed entity-relationship graph |
| **Prolog** | `PrologProjector`, `DefaultPrologProjector` | A Prolog fact base you can run rules against (experimental) |
| **Memory** | `MemoryProjector`, `DefaultMemoryProjector` | Propositions sorted into agent-context buckets |
| **Report** | `ReportProjector`, `StructuredReportProjector`, `RationaleProjector`, `LlmRationaleProjector` | Human-readable output, incl. "why is this believed" with evidence |

`RelationBasedGraphProjector` projects from the `Relations` you declared and is deterministic, cheap
and predictable; `LlmGraphProjector` infers edges instead. Edges carry lineage, so a changed source
cascades a stale marking rather than silently leaving a wrong edge in place, and reconciliation is
idempotent.

## Graph projection

```kotlin
// Kotlin — constructor with named arguments, not a builder chain
val projector = LlmGraphProjector(
    ai = ai,
    relations = relations,
    policy = LenientProjectionPolicy(),
    llmOptions = llmOptions,
)
```

Both graph projectors take `relations` and a `ProjectionPolicy`, and both expose wither methods
(`SourceAnalysisContext` is a separate `with*` data class — its `withRelations(...)` is **not** a
projector method, and there is no `RelationBasedGraphProjector.builder()` in this corpus):

| Wither | Effect |
|---|---|
| `withPolicy(policy)` | Set any `ProjectionPolicy` |
| `withLenientPolicy()` | `LenientProjectionPolicy` at the default threshold **0.7** |
| `withLenientPolicy(threshold)` | `LenientProjectionPolicy` at a custom threshold |
| `withDefaultPolicy()` | `DefaultProjectionPolicy` at the default threshold **0.85** |
| `withDefaultPolicy(threshold)` | `DefaultProjectionPolicy` at a custom threshold |

`Relations` declares the predicates that may hold between entities. Extraction uses them to keep
claims consistent, and `RelationBasedGraphProjector` uses them to decide which edges to create —
declaring relations is what turns free-text claims into a graph you can traverse rather than a pile
of sentences. Map the labels to the knowledge types you actually use:

| Knowledge type | Typical labels |
|---|---|
| `SEMANTIC` | `is-a`, `related-to` |
| `EPISODIC` | `occurred-at`, `involved` |
| `PROCEDURAL` | `uses`, `requires` |

**Use it for:** graph traversal, relationship queries, multi-hop inference over propositions.

## Prolog projection

```java
@Bean
PrologProjector prologProjector(/* collaborators */) {
    return new DefaultPrologProjector(/* ... */);
}
```

**Activation is a `PrologProjector` bean — nothing is wired by default**, and the feature is marked
**experimental** upstream. Treat it as opt-in and expect the surface to move.

**Use it for:** logical inference and rule-based queries over proposition data.

## Memory projection

```java
@Bean
MemoryProjector memoryProjector() {
    return DefaultMemoryProjector.DEFAULT;
}
```

Sorts propositions into `semantic`, `episodic`, `procedural` and `working` buckets, ready to drop into
an agent's context. This is the projection behind the `Memory` facade used for agentic recall.

**Use it for:** letting agents recall propositions during a conversation.

## Report projection

Lives in the **`dice-report`** module.

| Type | Shape |
|---|---|
| `ReportProjector` | `report(propositions, title)` → `Report`. Single method, deterministic. |
| `StructuredReportProjector` | The shipped `ReportProjector`: groups by status and level, takes top-N by effective confidence. |
| `RationaleProjector` | `rationale(proposition)` / `rationale(group)` → `RationaleArtifact`. |
| `LlmRationaleProjector` | The shipped `RationaleProjector`, built via `withLlm(options).withAi(ai)`. |

**Use it for:** human-readable output and provenance — answering "why is this believed?" with the
supporting evidence.

## Decorators and gate decisions

`EventEmittingProjector` is a generic decorator over **any** `Projector`: it wraps one and publishes
Spring application events on projection. Its sibling `EventEmittingPropositionRepository` publishes on
save. Wire either in when something else needs to react; neither is on by default.

`GateDecision.SkipProjection` is **not** a projector — it is a gate decision that persists the fact
but keeps it out of the typed graph.

## Dependencies — there are no per-backend modules

There is **no** `embabel-dice-neo4j`, `embabel-dice-vector`, `embabel-dice-prolog`, `embabel-dice-agent`
or any other per-backend artifact. The nine modules declared at `pom.xml:25-35` are:
`dice`, `dice-storage`, `dice-storage-autoconfigure`, `dice-report`, `dice-ingestion`,
`dice-metamodel`, `dice-integration-tests`, `dice-user-guide`, `dice-mcp-autoconfigure`.

Projection work needs `dice`, plus `dice-storage` (which is **Drivine-parameterised** — graph storage
is Drivine's `DrivinePropositionRepository` and friends, not a DICE module of its own), plus
`dice-report` for the report projections, and `dice-storage-autoconfigure` /
`dice-mcp-autoconfigure` for Spring wiring.

## Configuration — there is no projections block

**There is no `dice.projections.*` tree.** Per-backend projection config keys such as
`dice.projections.vector.enabled` or `dice.projections.neo4j.uri` do not exist. The prefixes DICE
actually binds, per the user guide's configuration-properties page, are:

| Prefix | Bound by | Read by |
|---|---|---|
| `embabel.dice.store` | `DiceStoreProperties` | `dice-storage-autoconfigure` |
| `embabel.dice.collector` | `CollectorProperties` | `dice-storage-autoconfigure` |
| `embabel.dice.source-analyzer` | — (bound by `LlmSourceAnalyzer`) | `dice` |
| `dice.security.api-key` | `DiceApiKeyProperties` | `dice` |

Note the last one uses prefix **`dice.`**, not `embabel.dice.` — the split is upstream's, so do not
"unify" the prefixes.

Two things people look for in the wrong place:

- **Vector indexing is a storage concern, not a projection.** The key is
  `embabel.dice.store.vector-index.enabled` (default `true`, **graph backend only**). The index's
  label, property, name and similarity are *not* configurable — they are fixed by the `@VectorIndex`
  annotation on `PropositionNode.embedding` and live as constants on `DrivinePropositionRepository`.
- **Neo4j connection settings belong to Drivine, and DICE reads neither**, so put no `bolt://` URI in
  DICE config. Likewise an LLM provider is configured through embabel-agent, not DICE.

```yaml
embabel:
  dice:
    store:
      type: graph          # graph (Drivine/Neo4j) or in-memory
      decay:
        enabled: true
        interval-ms: 3600000
        k: 2.0
        prune-stale: false
      vector-index:
        enabled: true
    collector:
      enabled: true
      match-threshold: 0.65
      signals:
        vector:
          weight: 0.4
          similarity-threshold: 0.85
          top-k: 20
        lexical:
          weight: 0.2
      trace:
        enabled: true
        detail-retention-days: 30

dice:
  security:
    api-key:
      enabled: true
      keys:
        - ${DICE_API_KEY}
```

Valid built-in collector signal names: `vector`, `lexical`, `entity-overlap`, `grounding-overlap`,
`provenance-overlap`, `polarity-veto`. `match-threshold` must lie in `[0.0, 1.0]` and signal weights
must be non-negative — validation fails startup otherwise. A `weight` of `null` keeps the scorer's own
default and is **not** the same as `0`.

## Common Pitfalls

1. **Treating a projection as the source of truth.** It is a rebuildable view; when it and the
   propositions disagree, rebuild it.
2. **Inventing per-backend modules.** Use `dice` + `dice-storage` (+ `dice-report`); see the nine
   real modules above.
3. **Inventing projection types or policies.** `VectorProjector`, `VectorStore`, `OracleProjector`,
   `OracleService`, `StrictProjectionPolicy` and `KnowledgeTypeProjectionPolicy` do **not** exist in
   this corpus. The attested policies are `LenientProjectionPolicy` and `DefaultProjectionPolicy`;
   confidence gating is a property of the pipeline and the store (`min-confidence`, decay/`k`), not a
   policy class you name.
4. **Expecting Prolog or event emission to be on by default.** Prolog needs a `PrologProjector` bean;
   events need an `EventEmitting*` decorator. Nothing is wired by default.
5. **Looking for a projections config block.** There is none — see the real prefixes above.
6. **Putting a Neo4j URI or an LLM provider key in DICE config.** Drivine owns the former,
   embabel-agent owns the latter.
