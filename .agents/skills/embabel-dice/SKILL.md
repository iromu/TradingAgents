---
name: embabel-dice
description: Build proposition-based knowledge graphs that give AI agents structured, confidence-weighted memory. Use this skill whenever the user wants to extract facts or propositions from text or conversation history, configure a proposition pipeline with extraction and revision, set up entity resolution to prevent duplicate entities, create schema-validated knowledge graphs with DataDictionary and DynamicType, or project knowledge to backends like Neo4j, vector stores, Prolog, or agent memory. Also use when the user mentions Embabel DICE, DICE, memory maintenance, proposition extraction, confidence-weighted facts, agentic recall, proposition querying, Memory facade, ContextId scoping, or the embabel-dice Maven module.
---

# Embabel DICE — Domain-Integrated Context Engineering

Build proposition-based knowledge graphs that give agents structured, confidence-weighted memory. DICE (Domain-Integrated Context Engineering) extends context engineering by emphasizing the importance of a domain model to structure context, and considering LLM outputs as well as inputs.

## Core Concepts

DICE is built on a proposition-based architecture inspired by the General User Models (GUM) research from Stanford/Microsoft.

```
Input → Pipeline → Propositions (System of Record) → Projections
```

**Propositions** are confidence-weighted natural language statements. They are the system of record. Multiple observations accumulate evidence, and high-confidence propositions project to typed backends.

**Knowledge Types** (`KnowledgeType`):
- `SEMANTIC` — General facts and knowledge
- `EPISODIC` — Specific events and experiences
- `PROCEDURAL` — How-to knowledge
- `WORKING` — Temporary working memory

## Setup

### Dependencies (Maven)

```xml
<dependency>
    <groupId>com.embabel.dice</groupId>
    <artifactId>embabel-dice-core</artifactId>
    <version>${embabel-dice.version}</version>
</dependency>
```

Additional modules:
- `embabel-dice-neo4j` — Neo4j projections
- `embabel-dice-prolog` — Prolog projections
- `embabel-dice-vector` — Vector store projections
- `embabel-dice-agent` — Agent integration (Memory facade)
- `embabel-dice-rest` — REST API with security

### Configuration

Key properties in `application.yml`:

```yaml
dice:
  memory:
    min-confidence: 0.5
    default-limit: 10
  resolver:
    in-memory:
      max-distance-ratio: 0.2
      min-length-for-fuzzy: 4
      min-part-length: 4
    escalating:
      heuristic-only: false
```

## Step 1 — Define a Schema

DICE uses a `DataDictionary` with `DynamicType` definitions to define the structure of the knowledge graph.

```java
DynamicType personType = DynamicType.builder("Person")
    .description("A person entity")
    .ownProperties(List.of(
        ValidatedPropertyDefinition.builder()
            .name("name")
            .validationRules(List.of(
                new NotBlank(),
                new NoVagueReferences(),
                new LengthConstraint(150)
            ))
            .build()
    ))
    .creationPermitted(true)
    .build();

DataDictionary schema = DataDictionary.fromDomainTypes("my-schema", List.of(personType));
```

> **Why:** Without a schema, the pipeline accepts any proposition structure. Always define types with validation rules. See `references/entity-resolution.md` for mention validation rules.

## Step 2 — Build the Pipeline

The pipeline extracts, resolves, revises, and stores propositions.

```java
@Bean
PropositionPipeline propositionPipeline(
        PropositionExtractor propositionExtractor,
        PropositionReviser propositionReviser,
        PropositionRepository propositionRepository,
        MentionFilter mentionFilter) {

    return PropositionPipeline.builder()
        .withExtractor(propositionExtractor)
        .withRevision(propositionReviser, propositionRepository)
        .withMentionFilter(mentionFilter)
        .build();
}
```

### Configure the Extractor

```java
@Bean
PropositionExtractor propositionExtractor(Ai ai, LlmOptions llmOptions) {
    return LlmPropositionExtractor.builder()
        .withLlm(llmOptions)
        .withAi(ai)
        .withPropositionRepository(propositionRepository)
        .withSchemaAdherence(SchemaAdherence.STRICT)
        .withTemplate("dice/extract_propositions")
        .build();
}
```

### Configure the Reviser

```java
@Bean
PropositionReviser propositionReviser(Ai ai, LlmOptions llmOptions) {
    return LlmPropositionReviser.builder()
        .withLlm(llmOptions)
        .withAi(ai)
        .withTemplate("dice/revise_propositions")
        .build();
}
```

> **Why:** Use `SchemaAdherence.STRICT` in production. `LOOSE` allows the LLM to create arbitrary proposition structures. See `references/pipeline.md` for full extractor/reviser details.

## Step 3 — Set Up Entity Resolution

Entity resolution maps mentions to canonical entities, preventing duplicates.

```java
@Bean
EntityResolver entityResolver(EntityRepository entityRepository, Ai ai, LlmOptions llmOptions) {
    var bakeoff = LlmCandidateBakeoff.builder()
        .withAi(ai)
        .withLlm(llmOptions)
        .withPromptMode(PromptMode.COMPACT)
        .build();
    return EscalatingEntityResolver.builder()
        .withRepository(entityRepository)
        .withCandidateBakeoff(bakeoff)
        .build();
}
```

> **Recommended:** `EscalatingEntityResolver` with `LlmCandidateBakeoff` — uses heuristics first, falls back to LLM when uncertain. See `references/entity-resolution.md` for resolver types, candidate searchers, and resolution outcomes.

## Step 4 — Process Text

```java
// One-shot ingestion (with deduplication)
var historyStore = new InMemoryChunkHistoryStore();
var result = pipeline.processOnce(
    text, "source-id-123", context, historyStore
);

// Batch processing (no deduplication)
var results = pipeline.process(chunks, context);

// Persist results
result.persist(propositionRepository, entityRepository);
```

> **Why:** Always use deduplication for production. Without `processOnce` or a `ChunkHistoryStore`, identical content is reprocessed. See `references/pipeline.md` for incremental analysis and custom content hashing.

## Step 5 — Query Propositions

```java
// By context
var props = repository.query(PropositionQuery.forContextId(contextId));

// High-importance only
var critical = repository.query(
    PropositionQuery.forContextId(contextId).withMinImportance(0.8).orderedByImportance()
);

// By entity
var aliceProps = repository.query(
    PropositionQuery.forContextId(contextId).mentioningEntity("alice-123")
);

// With confidence threshold
var confident = repository.query(
    PropositionQuery.forContextId(contextId)
        .withMinEffectiveConfidence(0.7)
        .orderedByEffectiveConfidence()
        .withLimit(10)
);
```

## Step 6 — Enable Agentic Recall (Memory Facade)

The `Memory` facade provides agents access to DICE knowledge.

```java
var memory = Memory.forContext(contextId)
    .withRepository(propositionRepository)
    .withProjector(memoryProjector)
    .withTopic("the user's project context")
    .withEagerSearchAbout(recentConversationText, 10)
    .withEagerTopicSearch(5)
    .withEagerQuery(q -> q.orderedByEffectiveConfidence().withLimit(3))
    .narrowedBy(q -> q.withEntityId("alice-123"));

ai.withReferences(memory).respond(prompt);
```

> **Key settings:** `withMinConfidence` (default 0.5), `withDefaultLimit` (default 10), `withEagerSearchAbout`. Always scope by `ContextId`. See `references/memory.md` for eager search patterns and full agent integration examples.

## Step 7 — Project to Backends

Projections materialize propositions to typed backends.

```java
// Neo4j graph projection
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

// Memory projection (for agentic recall)
@Bean
GraphProjector memoryProjector() {
    return DefaultMemoryProjector.DEFAULT;
}
```

> **Projection backends:** Vector (semantic retrieval), Neo4j (graph traversal), Prolog (inference), Memory (agentic recall), Oracle (NL QA). See `references/projections.md` for full setup on each backend.

## Step 8 — Maintain the Knowledge Graph

Automatically maintain — retire low-confidence propositions, create abstractions:

```java
@Bean
MemoryMaintenanceOrchestrator memoryMaintenanceOrchestrator(
        PropositionRepository repository, MemoryConsolidator consolidator, PropositionAbstractor abstractor) {
    return MemoryMaintenanceOrchestrator.builder()
        .withRepository(repository)
        .withConsolidator(consolidator)
        .withAbstractor(abstractor)
        .withAbstractionThreshold(5)
        .withAbstractionTargetCount(3)
        .withRetireBelow(0.1)
        .withRetireDecayK(2.0)
        .build();
}
// Usage
memoryMaintenanceOrchestrator.maintain(contextId, sessionProps);
```

## Common Pitfalls

1. **Not setting a schema** — Always define `DataDictionary` types with validation rules.
2. **Skipping entity resolution** — Use `EscalatingEntityResolver` with `LlmCandidateBakeoff`.
3. **Not using deduplication** — Always use `processOnce` with a `ChunkHistoryStore` for production.
4. **Ignoring mention filters** — Always use `SchemaValidatedMentionFilter` to validate.
5. **Not scoping by ContextId** — Without context scoping, propositions from different users/sessions mix.
6. **Not configuring memory eager search** — Preload context with `withEagerSearchAbout` for better latency.
7. **Not maintaining the knowledge graph** — Run `MemoryMaintenanceOrchestrator` periodically to retire low-confidence propositions.
8. **Using `SchemaAdherence.LOOSE` in production** — Use `STRICT` for schema compliance.
9. **Not setting min-confidence on Memory** — Without `withMinConfidence`, low-quality propositions flood the agent's context.
10. **Forgetting to persist results** — Call `result.persist(repository, entityRepository)` or configure auto-persist.

## Reference Files

- `references/pipeline.md` — Pipeline architecture, extractor/reviser templates, deduplication, incremental analysis
- `references/projections.md` — Projection backends (Vector, Neo4j, Prolog, Memory, Oracle) setup and configuration
- `references/entity-resolution.md` — Entity resolver types, candidate searchers, bakeoff configuration, resolution outcomes
- `references/memory.md` — Memory facade, eager search patterns, context scoping, integration with LLM calls
