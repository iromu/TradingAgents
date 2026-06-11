---
name: embabel-dice
description: Build proposition-based knowledge graphs that give AI agents structured, confidence-weighted memory. Use this skill whenever the user wants to extract facts or propositions from text or conversation history, configure a proposition pipeline with extraction and revision, set up entity resolution to prevent duplicate entities, create schema-validated knowledge graphs with DataDictionary and DynamicType, or project knowledge to backends like Neo4j, vector stores, Prolog, or agent memory. Also use when the user mentions Embabel DICE, DICE, memory maintenance, proposition extraction, confidence-weighted facts, agentic recall, proposition querying, Memory facade, ContextId scoping, or the embabel-dice Maven module.
---

# Embabel DICE — Domain-Integrated Context Engineering

Build proposition-based knowledge graphs that give agents structured, confidence-weighted memory. DICE (
Domain-Integrated Context Engineering) extends context engineering by emphasizing the importance of a domain model to
structure context, and considering LLM outputs as well as inputs.

## Output Quality

When producing code or documentation:

- **Be comprehensive** — Provide complete, multi-section outputs with thorough explanations
- **Include full pipeline setup** — Show the complete builder chain (extractor → reviser → filter → pipeline)
- **Show schema design** — Include full DataDictionary with DynamicType definitions and validation rules
- **Include configuration** — Show full `application.yml` blocks with DICE-specific properties
- **Provide querying examples** — Show PropositionQuery patterns for different use cases
- **Use latest API patterns** — Builder-style configuration, ContextId-based scoping, Memory facade

## Core Concepts

DICE is built on a proposition-based architecture inspired by the General User Models (GUM) research from
Stanford/Microsoft.

```
Input → Pipeline → Propositions (System of Record) → Projections
```

**Propositions** are confidence-weighted natural language statements. They are the system of record. Multiple
observations accumulate evidence, and high-confidence propositions project to typed backends.

**Projections** (materialized views):
| Projection | Backend | Use Case |
|------------|---------|----------|
| **Vector** | Vector store | Semantic retrieval |
| **Neo4j** | Graph database | Graph traversal, relationship queries |
| **Prolog** | Prolog engine | Inference, rules |
| **Memory** | Agent context | Agentic recall (Memory facade) |
| **Oracle** | NL QA | Natural language question answering |

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
  security:
    api-key:
      enabled: false
      keys: []
      header-name: X-API-Key
      path-patterns:
        - /api/v1/**
  resolver:
    in-memory:
      max-distance-ratio: 0.2
      min-length-for-fuzzy: 4
      min-part-length: 4
    escalating:
      heuristic-only: false
  memory:
    min-confidence: 0.5
    default-limit: 10
```

## Schema Design

DICE uses a `DataDictionary` with `DynamicType` definitions to define the structure of the knowledge graph.

### Defining Types with Validation

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
            .build(),
        ValidatedPropertyDefinition.builder()
            .name("department")
            .validationRules(List.of(new MinWordCount(1)))
            .build()
    ))
    .parents(List.of())
    .creationPermitted(true)
    .build();

DynamicType companyType = DynamicType.builder("Company")
    .description("A business organization")
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
    .parents(List.of())
    .creationPermitted(true)
    .build();

DataDictionary schema = DataDictionary.fromDomainTypes("my-schema", List.of(personType, companyType));
```

### Mention Validation Rules

| Rule                       | Description                                |
|----------------------------|--------------------------------------------|
| `NotBlank`                 | Rejects empty/whitespace mentions          |
| `NoVagueReferences()`      | Rejects demonstratives like "this company" |
| `LengthConstraint(max)`    | Enforces maximum length                    |
| `MinWordCount(min)`        | Requires minimum word count                |
| `PatternConstraint(regex)` | Enforces regex pattern                     |
| `AllOf(rules...)`          | Compose — all rules must pass              |
| `AnyOf(rules...)`          | Compose — any rule must pass               |

## Proposition Pipeline

The pipeline is the core of DICE. It extracts, resolves, revises, and stores propositions.

### Building a Pipeline

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

### Proposition Extractor (LLM-Powered)

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

### Proposition Reviser

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

### Processing Text

```java
// One-shot ingestion (with deduplication)
var historyStore = new InMemoryChunkHistoryStore();
var result = pipeline.processOnce(
    text,
    "source-id-123",
    context,
    historyStore
);

// Batch processing (no deduplication)
var results = pipeline.process(chunks, context);
```

## Content Deduplication

DICE prevents reprocessing identical content through hash-based deduplication.

### One-Shot Ingestion

```java
var historyStore = new InMemoryChunkHistoryStore();

// processOnce returns null if content was already seen
var result = pipeline.processOnce(
    documentText,
    "doc-123",
    context,
    historyStore
);

if (result == null) {
    // Content was already processed — skip
    return;
}
```

### Custom Content Hashing

```java
ContentHasher normalizingHasher = text -> {
    String normalized = text.trim().replaceAll("\\s+", " ");
    return Sha256ContentHasher.hash(normalized);
};

var historyStore = new InMemoryChunkHistoryStore(normalizingHasher);
```

### Incremental Analysis

For streaming or windowed analysis:

```java
@Bean
IncrementalAnalyzer<String, PropositionResult> incrementalAnalyzer(
        PropositionExtractor extractor,
        PropositionReviser reviser,
        PropositionRepository repository) {

    return AbstractIncrementalAnalyzer.<String, PropositionResult>builder()
        .withExtractor(extractor)
        .withRevision(reviser, repository)
        .withWindowConfig(WindowConfig.builder()
            .windowSize(20)
            .overlapSize(5)
            .build())
        .build();
}
```

## Entity Resolution

Entity resolution maps mentions to canonical entities, preventing duplicates. See `references/entity-resolution.md` for
full details on resolver types, candidate searchers, and bakeoff configuration.

**Recommended:** `EscalatingEntityResolver` with `LlmCandidateBakeoff` — uses heuristics first, falls back to LLM when
uncertain.

```java
@Bean
EntityResolver entityResolver(EntityRepository entityRepository, Ai ai, LlmOptions llmOptions) {
    var bakeoff = LlmCandidateBakeoff.builder().withAi(ai).withLlm(llmOptions).withPromptMode(PromptMode.COMPACT).build();
    return EscalatingEntityResolver.builder().withRepository(entityRepository).withCandidateBakeoff(bakeoff).build();
}
```

**Resolution outcomes:** `NewEntity` (no match), `ExistingEntity` (match found), `ReferenceOnlyEntity` (known entity),
`VetoedEntity` (non-creatable type).

**Entity extraction** (standalone, without proposition pipeline):

```java
var results = EntityPipeline.builder().withExtractor(extractor).build().process(chunks, context);
```

## Proposition Querying

Query propositions by context, entity, confidence, or importance:

```java
// Query by context
var contextProps = repository.query(
    PropositionQuery.forContextId(contextId)
);

// High-importance facts only
var criticalFacts = repository.query(
    PropositionQuery.forContextId(contextId)
        .withMinImportance(0.8)
        .orderedByImportance()
);

// Query by entity
var aliceProps = repository.query(
    PropositionQuery.forContextId(contextId)
        .mentioningEntity("alice-123")
);

// Query by multiple entities
var either = repository.query(
    PropositionQuery.forContextId(contextId)
        .mentioningAnyEntity("alice-123", "bob-456")
);

// With confidence threshold
var confidentFacts = repository.query(
    PropositionQuery.forContextId(contextId)
        .withMinEffectiveConfidence(0.7)
        .orderedByEffectiveConfidence()
        .withLimit(10)
);
```

## Memory — Agentic Recall

The `Memory` facade provides agents access to DICE knowledge. See `references/memory.md` for eager search patterns,
context scoping, and configuration.

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

**Key settings:** `withMinConfidence` (default 0.5), `withDefaultLimit` (default 10), `withTopic`,
`withEagerSearchAbout`, `narrowedBy`.

## Memory Maintenance

Automatically maintain the knowledge graph — retire low-confidence propositions, create abstractions:

```java
@Bean
MemoryMaintenanceOrchestrator memoryMaintenanceOrchestrator(
        PropositionRepository repository, MemoryConsolidator consolidator, PropositionAbstractor abstractor) {
    return MemoryMaintenanceOrchestrator.builder()
        .withRepository(repository).withConsolidator(consolidator).withAbstractor(abstractor)
        .withAbstractionThreshold(5).withAbstractionTargetCount(3)
        .withRetireBelow(0.1).withRetireDecayK(2.0).build();
}
// Usage
memoryMaintenanceOrchestrator.maintain(contextId, sessionProps);
```

## Projections

Projections materialize propositions to typed backends. See `references/projections.md` for full setup details on
Vector, Neo4j, Prolog, Memory, and Oracle projections.

### Relation-Based Graph Projection (Neo4j)

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

### Prolog Projection

```java
@Bean
GraphProjector prologProjector(PrologEngine prologEngine) {
    return new PrologProjector(prologEngine, prologSchema);
}
```

### Memory Projection

```java
@Bean
GraphProjector memoryProjector() {
    return DefaultMemoryProjector.DEFAULT;
}
```

## Integration with Embabel Agents

DICE integrates with Embabel agents through the `Memory` facade and `LlmReference` interface:

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

## Common Pitfalls

1. **Not setting a schema** — Without a DataDictionary, the pipeline accepts any proposition structure. Always define
   types with validation rules.
2. **Skipping entity resolution** — Without an EntityResolver, the same entity mentioned differently creates duplicates.
   Use `EscalatingEntityResolver` with `LlmCandidateBakeoff`.
3. **Not using deduplication** — Without `processOnce` or a `ChunkHistoryStore`, identical content is reprocessed.
   Always use deduplication for production.
4. **Ignoring mention filters** — Without `SchemaValidatedMentionFilter`, low-quality mentions pollute the knowledge
   graph. Always validate.
5. **Not scoping by ContextId** — Without context scoping, propositions from different users/sessions mix together.
   Always use `ContextId`.
6. **Not configuring memory eager search** — Without `withEagerSearchAbout`, the agent must search on every turn.
   Preload context for better latency.
7. **Not maintaining the knowledge graph** — Over time, low-confidence propositions accumulate. Run
   `MemoryMaintenanceOrchestrator` periodically.
8. **Using SchemaAdherence.LOOSE in production** — Loose adherence allows the LLM to create arbitrary proposition
   structures. Use `STRICT` for schema compliance.
9. **Not setting min-confidence on Memory** — Without `withMinConfidence`, low-quality propositions flood the agent's
   context. Set an appropriate threshold.
10. **Forgetting to persist results** — `processOnce` and `process` return results but don't auto-persist. Call
    `result.persist(repository, entityRepository)` or configure auto-persist.

## Reference Files

- `references/pipeline.md` — Proposition pipeline architecture, extractor/reviser templates, deduplication, incremental
  analysis
- `references/projections.md` — Projection backends (Vector, Neo4j, Prolog, Memory, Oracle) setup and configuration
- `references/entity-resolution.md` — Entity resolver types, candidate searchers, bakeoff configuration, resolution
  outcomes
- `references/memory.md` — Memory facade, eager search patterns, context scoping, integration with LLM calls
