---
name: embabel-dice
description: Build proposition-based knowledge graphs and agent memory with Embabel DICE (Domain-Integrated Context Engineering), the Kotlin/Apache-2.0 library on the embabel-agent framework. Use when the user mentions Embabel DICE, the dice or dice-mcp-autoconfigure or dice-metamodel Maven modules, proposition extraction or revision, PropositionPipeline, processOnce, PropositionQuery, the Memory facade or MemoryProjector, agentic recall, confidence-weighted facts, entity resolution with EscalatingEntityResolver or LlmCandidateBakeoff, graph or Prolog or memory projection, Drivine or Neo4j proposition storage, exposing dice_recall / dice_list / dice_store / dice_get over MCP, or metamodel versioning with MetamodelVersion, DeclaredSchema and DeclaredSchemaSource. Covers the real module list, property prefixes and configuration defaults.
metadata:
  author: "Iván Rodríguez Murillo <wantez@gmail.com>"
  version: 0.2.0
  category: ai-agents
  tags: [embabel, dice, knowledge-graph, agent-memory, propositions, mcp, kotlin, java]
---

# Embabel DICE - Domain-Integrated Context Engineering

DICE extracts confidence-weighted propositions from text, stores them as the system of record, and
projects them to typed backends (graph, Prolog, agent memory, reports). Kotlin, Apache-2.0, built on
the Embabel agent framework.

```
Source -> PropositionPipeline -> Propositions (system of record) -> Projections
```

## Instructions

1. Treat "Upstream State (verify against this, not against the README)" as the source of truth for what exists in current DICE; where this guide and a README/blog disagree, trust the version pins and module list stated there.
2. Work through Steps 1–10 in order (Schema → Pipeline → Entity resolution → Extract/persist → Query → Agent memory → Projections → Maintenance → MCP exposure → Metamodel versioning); each step builds on the previous, so don't jump ahead except when fixing an existing app.
3. Use only the two annotations in "Annotation Surface - only two DICE annotations exist" and the property prefixes in "Configuration - real prefixes only" — anything else is invented.
4. Before finalizing, cross-check "Common Pitfalls" and the "Verification Checklist", and confirm scope against "When NOT to Use".

## Upstream state

The verified upstream drift notes moved to `references/upstream-state.md`. Check the pinned upstream docs before trusting an API name.


## Modules and configuration

The full module list moved to `references/modules.md`.

## Configuration - real prefixes only

The old `dice.memory.*` / `dice.resolver.*` block matches **no documented property prefix** and must
not be reproduced. The prefixes are genuinely split, and upstream flags the split itself
(`reference/configuration-properties.adoc` heads a section "Read by `dice`. Note the prefix: `dice.`,
not `embabel.dice.`"). Do not "unify" them.

| Prefix | Owner class | Read by |
|--------|-------------|---------|
| `embabel.dice.store.*` | `DiceStoreProperties` | `dice-storage-autoconfigure` |
| `embabel.dice.collector.*` | `CollectorProperties` | `dice-storage-autoconfigure` |
| `embabel.dice.source-analyzer` | bound by `LlmSourceAnalyzer` | `dice` |
| `embabel.dice.mcp.*` | `DiceMcpProperties` | `dice-mcp-autoconfigure` |
| `dice.security.api-key.*` | `DiceApiKeyProperties` | `dice` - **`dice.`, not `embabel.dice.`** |

```yaml
embabel:
  dice:
    store:
      type: in-memory        # or graph (Drivine over Neo4j/FalkorDB/Memgraph)
      decay:
        enabled: true
        interval-ms: 3600000
        k: 2.0
        prune-stale: false
      vector-index:
        enabled: true        # graph backend only
    mcp:
      enabled: true          # default false
      min-confidence: 0.5    # default 0.5
      default-limit: 10      # default 10; must be 1..100 or startup fails
      writes-enabled: false  # default false; gates dice_store
dice:
  security:
    api-key:
      enabled: true
      keys:
        - ${DICE_API_KEY}
```

The `0.5` / `10` defaults belong to `embabel.dice.mcp.*` (`DiceMcpProperties`), not to any
`dice.memory.*` key. LLM provider configuration belongs to embabel-agent and Neo4j connection
settings to Drivine - "DICE reads neither".

## Step 1 - Schema (Embabel Agent types, not DICE types)

`DataDictionary`, `DynamicType`, `ValidatedPropertyDefinition`, `ContextId`, `Cardinality`,
`DomainType`, `PropertyDefinition` are all **`com.embabel.agent.core.*`** - proven by the imports in
the collected DICE sources (`MetamodelDsl.kt:18`, `MetamodelVersion.kt:18-22`). They are not DICE
types and their full guidance belongs to the `embabel-agent` skill.

```kotlin
import com.embabel.agent.core.*
import com.embabel.dice.common.validation.*

val personType = DynamicType(
    name = "Person",
    description = "A person",
    ownProperties = listOf(
        ValidatedPropertyDefinition(
            name = "name",
            validationRules = listOf(
                NotBlank,                     // an object - no parentheses
                NoVagueReferences(),
                LengthConstraint(maxLength = 150)
            )
        )
    ),
    parents = emptyList(),
    creationPermitted = true
)

val schema = DataDictionary.fromDomainTypes("my-schema", listOf(personType))
```

Shipped validation rules: `NotBlank`, `NoVagueReferences()`, `LengthConstraint(maxLength)`,
`MinWordCount(n)`, `PatternConstraint()`, and the combinators `AllOf()` / `AnyOf()`.

## Step 2 - Pipeline (immutable, static factories - no builder)

`PropositionPipeline` is immutable; every `with*` returns a new instance. There is **no**
`PropositionPipeline.builder()`.

```java
var extractor = LlmPropositionExtractor
    .withLlm(llmOptions)
    .withAi(ai)
    .withPropositionRepository(repository)
    .withSchemaAdherence(SchemaAdherence.DEFAULT);

var pipeline = PropositionPipeline
    .withExtractor(extractor)
    .withRevision(reviser, repository)
    .withMentionFilter(new SchemaValidatedMentionFilter(schema));
```

`SchemaAdherence` has exactly three values - `STRICT`, `DEFAULT`, `RELAXED` (`LOOSE` is not one of
them): `STRICT` locks entity types and predicates; `DEFAULT` locks types, allows any predicate (the
usual choice); `RELAXED` prefers the schema without requiring it.

## Step 3 - Entity resolution

```java
var resolver = EscalatingEntityResolver.create(entityRepository, llmCandidateBakeoff);  // ambiguity -> LLM
var noMint   = EscalatingEntityResolver.create(entityRepository, null);                 // ambiguity -> new entity
var chain    = new ChainedEntityResolver(List.of(
    new KnownEntityResolver(knownEntities),
    EscalatingEntityResolver.create(entityRepository, bakeoff)));
```

Searchers (cheapest first): `ByIdCandidateSearcher`, `ByExactNameCandidateSearcher`,
`NormalizedNameCandidateSearcher`, `PartialNameCandidateSearcher`, `FuzzyNameCandidateSearcher`,
`VectorCandidateSearcher`, `AgenticCandidateSearcher`. Assemble them with
`DefaultCandidateSearchers.create(repository)` or `.withoutVector(repository)` - do not add
`InMemoryEntityResolver` by hand for a `process()` run; the pipeline adds it when minting is on.
`LevelResult` reports the `ResolutionLevel` (`EXACT_MATCH` / `LLM_BAKEOFF` / `NO_MATCH`) per answer -
that distribution is the tuning diagnostic. Tune towards **under**-merging: a duplicate can be
collected later, a bad merge cannot be unpicked.

## Step 4 - Extract and persist

```java
var context = new SourceAnalysisContext(schema, resolver, new ContextId("user:42"))
    .withKnownEntities(KnownEntity.asCurrentUser(user))
    .withRelations(relations)
    .withMintNewEntities(true);            // off by default

var chunk = new Chunk("chunk-1", "Ada Lovelace worked with Charles Babbage.");
var results = pipeline.process(List.of(chunk), context);
results.persist(propositionRepository, namedEntityDataRepository);   // inside your own transaction
```

`process*` returns **unsaved** `PersistablePropositions` on purpose; dropping it discards the work.
`persist` saves only referenced entities, then the propositions, then the structural links.

Dedup: `processOnce(text, sourceId, context, historyStore)` returns `null` on a repeat of the same
content; omit the store for no dedup. For growing sources use
`PropositionIncrementalAnalyzer(pipeline, historyStore, formatter, WindowConfig(...), contentHasher)`
and `analyzer.analyze(source, context)`. `InMemoryChunkHistoryStore` is for tests - production should
persist (`docs/design/durable-storage.md`).

## Step 5 - Query

Every query starts from a scope: `PropositionQuery.forContextId(ctxId)` or the Java-friendly
`PropositionQuery.againstContext(ctxId)` (that spelling is upstream's). There is deliberately **no**
`create()` factory - it would make "load everything" the shortest thing to write.

```java
var base = PropositionQuery.againstContext(new ContextId("user:42"));
repository.query(base.withMinEffectiveConfidence(0.7)
                     .withStatus(PropositionStatus.ACTIVE)
                     .withAllEntities(adaId, babbageId)     // claims about the pair
                     .withMinReinforceCount(3)
                     .withLimit(20));
```

Filter axes: scope (`entityId`, `anyEntityIds`, `allEntityIds`), lifecycle (`statuses`, `pinned`,
`minLevel`, `maxLevel`), time (`createdAfter`, `revisedAfter`, `accessedAfter` - `revisedAfter` is the
decay anchor and the right one for "what changed"), belief (`minConfidence`,
`minEffectiveConfidence`, `belowEffectiveConfidence`, `minImportance`, `minReinforceCount`,
`minTrustScore`), shape (`orderBy`, `limit`). Capabilities are optional per backend - check before
relying on one: `VectorSearchCapable`, `GraphTraversalCapable`, `GraphQueryCapable`,
`TemporalQueryCapable`. `RetrievalRouter` picks a `RetrievalMode` when you would rather not choose.

## Step 6 - Agent memory

```java
var memory = Memory.forContext(contextId)
    .withRepository(propositionRepository)
    .withProjector(DefaultMemoryProjector.DEFAULT)
    .withTopic("the user's project context")
    .withEagerSearchAbout(recentConversationText, 10)
    .withEagerTopicSearch(5)
    .withEagerQuery(q -> q.orderedByEffectiveConfidence().withLimit(3))
    .narrowedBy(q -> q.withEntityId("alice-123"));

ai.withReferences(memory).respond(prompt);
```

Two tiers: eager (`contribution()` preloads into the system prompt) plus on-demand (`tools()`
exposes a search tool, auto-deduplicated against the eager set). Defaults: `withMinConfidence`
`0.5`, `withDefaultLimit` `10`. `narrowedBy` constrains **every** query and the LLM cannot escape it.
For the bucketed read side use `MemoryProjector.project(...)` -> `MemoryProjection`, which sorts
propositions into the four `KnowledgeType` buckets `SEMANTIC` / `EPISODIC` / `PROCEDURAL` / `WORKING`
(`README.md:2108`); the retrieval collaborator is `MemoryRetriever` (similarity + entity + recency,
`README.md:2163`). `propositionRepository.pin(id)` exempts a claim from decay-driven reclamation.

## Step 7 - Projections (four, and they are rebuildable views)

| Projection | Types | Note |
|------------|-------|------|
| Graph | `GraphProjector`, `RelationBasedGraphProjector`, `LlmGraphProjector`, `GraphProjectionService` | edges carry lineage; `Reconciler` is idempotent |
| Prolog | `PrologProjector`, `DefaultPrologProjector`, `PrologEngine`, `PrologTypes` | experimental; tuProlog |
| Memory | `MemoryProjector`, `MemoryProjection`, `DefaultMemoryProjector` | four knowledge-type buckets |
| Report | `ReportProjector`, `StructuredReportProjector`, `RationaleProjector` | in `dice-report` |

```java
var projector = RelationBasedGraphProjector.from(relations).withLenientPolicy();
var outcome = projector.projectAll(propositions, schema);

var service = GraphProjectionService.create(graphProjector, persister, schema);
var result = service.projectAndPersist(propositions);
```

Policy withers: `withPolicy(policy)`, `withLenientPolicy([threshold])` (`LenientProjectionPolicy`,
default 0.7), `withDefaultPolicy([threshold])` (`DefaultProjectionPolicy`, default 0.85). There is **no**
`StrictProjectionPolicy`, `KnowledgeTypeProjectionPolicy`, `VectorProjector` or `OracleProjector`.
Vector is a store capability (`VectorSearchCapable`) plus a fixed `@VectorIndex` on
`PropositionNode.embedding`; NL question answering is `Oracle` / `LlmOracle` / `ToolOracle` under
`com.embabel.dice.query.oracle`, not a projection. If the graph and the propositions disagree, the
propositions win.

## Step 8 - Maintenance

```java
var orchestrator = MemoryMaintenanceOrchestrator
    .withRepository(propositionRepository)
    .withConsolidator(new DefaultMemoryConsolidator())
    .withAbstractor(LlmPropositionAbstractor.withLlm(llm).withAi(ai))
    .withRetireBelow(0.1);
MaintenanceResult result = orchestrator.maintain(contextId, sessionPropositions);
```

Options: `withAbstractionThreshold` (default 5), `withAbstractionTargetCount` (default 3),
`withRetireDecayK` (default 2.0). For scheduled hygiene prefer the threshold-gated dream loop -
`DefaultDreamLoopOrchestrator`, returning a `DreamLoopReport` - over
`DefaultMemoryMaintenanceOrchestrator`, which is the older four-step pipeline kept for compatibility.
Both live in `com.embabel.dice.projection.memory`; the consolidation passes they compose
(`SessionConsolidationPass`, `AbstractionPass`, `ContradictionResolutionPass`, `DecaySweepPass`) are
under `com.embabel.dice.operations.consolidation`.

## Step 9 - Expose DICE over MCP

Step 9 (expose DICE over MCP: dice_recall / dice_list / dice_store / dice_get, transport and endpoint details) moved to `references/mcp-exposure.md`.

## Step 10 - Metamodel versioning (EXPERIMENTAL)

Step 10 (metamodel versioning: `MetamodelVersion`, `DeclaredSchema`, `DeclaredSchemaSource`) moved to `references/metamodel.md`.

## Annotation Surface - only two DICE annotations exist

`annotation class MetamodelDsl` (`MetamodelDsl.kt:27`, `@DslMarker` +
`@Target(AnnotationTarget.CLASS)`) and, as *usage*, `@VectorIndex` on `PropositionNode.embedding`
(fixes the vector index label, property, name and similarity as constants on
`DrivinePropositionRepository` - not configurable). Everything else that looks like a DICE annotation
is inherited: `@LlmTool` / `@LlmTool.Param` and `@ApiStatus` (JetBrains), plus the Spring pair
`@ConfigurationProperties` / `@AutoConfiguration`. Do not present a broad DICE annotation set;
`@Agent` / `@Action` / `@State` / `@Tool` belong to the `embabel-agent` skill.

## Examples

**"Persist chat history as queryable knowledge."** Step 6 -> the Memory facade (agent memory over the proposition store), then Step 7 -> a rebuildable projection for the read side; never hand-edit the store.

**"Expose DICE tools over MCP."** Step 9 -> `dice_recall`/`dice_list`/`dice_store`/`dice_get` via the MCP autoconfigure module; Step 10 (metamodel versioning) is EXPERIMENTAL - flag that to the user before recommending it.

**"The README shows a pipeline builder class."** "Upstream State" -> the pipeline is immutable with static factories only; if the README conflicts with the pinned version, the pin wins.

## Common Pitfalls

1. **Copying the README's install coordinates** - they are stale on both groupId and version.
2. **Inventing `embabel-dice-*` artifactIds** - use the nine real module names.
3. **Reusing the old `dice.memory.*` / `dice.resolver.*` YAML** - no such prefix exists; the 0.5/10
   defaults are `embabel.dice.mcp.*`.
4. **"Unifying" `dice.security.api-key` under `embabel.dice.`** - upstream really does split it.
5. **Importing `DataDictionary` / `ContextId` from a `dice` package** - they are
   `com.embabel.agent.core.*`.
6. **`PropositionPipeline.builder()`** - use `PropositionPipeline.withExtractor(...)` and reassign;
   the pipeline is immutable.
7. **Writing `SchemaAdherence.LOOSE`** - the values are `STRICT` / `DEFAULT` / `RELAXED`.
8. **Skipping `persist()`** - the pipeline returns unsaved results by design.
9. **`PropositionQuery.create()`** - does not exist; always scope by `ContextId`.
10. **Assuming vector/graph/temporal capability** - `instanceof` the capability interface first.
11. **Turning on `dice_store` without saying what it costs** - empty mentions, no provenance.
12. **Presenting the metamodel DSL as stable** - it is EXPERIMENTAL and nothing is released.

## When NOT to Use

- Authoring agents, actions, goals, planners, providers, `@Agent`/`@Action`/`@State`/`@Tool`, or
  publishing an agent as an MCP server - use the `embabel-agent` skill. DICE only consumes it.
- Neo4j object mapping, `@Node`/`@Relationship` entities, Cypher repositories - `embabel-drivine4j`.
- Generic RAG over documents with no proposition store, or plain vector search - use a vector store
  directly; DICE is about the domain model plus the proposition system of record.
- Building DICE itself, or anything in `dice-integration-tests` / `dice-user-guide` / `docs/design/`.
- Anything you cannot attest from `raw/dice-docs-0.2.0/` - the collected corpus carries 2 of the
  `dice` module's 297 main-source files, so absence there is **not** absence from the library. Verify
  a code-level identifier against `src/main` before adding or dropping it.

## Verification Checklist

- [ ] Coordinates taken from `pom.xml`, not from `README.md:2630-2632`
- [ ] No tag or release implied; `main` / `-SNAPSHOT` only
- [ ] Only the nine real module names appear in dependency snippets
- [ ] Every property key sits under one of the five real prefixes, split preserved
- [ ] No `embabel-dice-*` artifactId, no `VectorProjector`/`OracleProjector`/`StrictProjectionPolicy`
- [ ] `SchemaAdherence` uses STRICT / DEFAULT / RELAXED
- [ ] Metamodel material carries the EXPERIMENTAL caveat; `DeclaredSchema` cited at
      `DeclaredSchemaSource.kt:47`
- [ ] The MCP dependency pair is presented as UNRESOLVED, not silently narrowed to one artifact

## References

- `references/pipeline.md` - modules, coordinates, pipeline/extractor/reviser shapes, dedup, query API
- `references/entity-resolution.md` - resolver and searcher surface, resolution levels, tuning
- `references/memory.md` - Memory facade, knowledge types, projection buckets, retrieval, maintenance
- `references/projections.md` - the four projections, policies, Drivine storage switch, reclamation

The MCP surface (Step 9) and the metamodel DSL (Step 10) are documented inline above rather than in
separate reference files - both are small enough to live in the main body, and their identifiers were
verified against `raw/dice-docs-0.2.0/` (`dice_recall` / `dice_list` / `dice_store` / `dice_get` in
`DiceMcpTools.kt`; `MetamodelVersion` / `DeclaredSchema` / `DeclaredSchemaSource`).
