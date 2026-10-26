# Proposition Pipeline Reference

## Table of Contents

- [Pipeline Architecture](#pipeline-architecture)
- [Core Components](#core-components) — `PropositionExtractor` / `LlmPropositionExtractor`,
  `PropositionReviser` / `LlmPropositionReviser`, the `MentionFilter` implementations
- [Building the Pipeline](#building-the-pipeline) — the immutable `with*` construction, minimal and full
- [Processing Sources](#processing-sources) — `process()` vs `processOnce()`, incremental processing
- [Source Analysis Context](#source-analysis-context) — the `with*` data class, `ContextId`
- [Event-Driven Integration](#event-driven-integration) — `ConversationAnalysisRequestEvent`
- [Common Pitfalls](#common-pitfalls)

## Pipeline Architecture

```
Source → IncrementalAnalyzer → PropositionPipeline → Repository
```

The pipeline is the core of DICE. It processes text sources, extracts propositions, resolves entities, revises existing propositions, and stores results.

## Core Components

### PropositionExtractor

Extracts typed propositions from text using an LLM.

The pipeline type is `PropositionPipeline`, whose attested entry points are:

```
+process(chunks, context)                       PropositionResults
+processOnce(text, sourceId, context, historyStore)  ChunkPropositionResult?
```

Note the plural/singular split: the batch call yields `PropositionResults`, the deduplicated single
call yields a nullable `ChunkPropositionResult`. **There is no singular `PropositionResult` type.**
`process(...)` runs in two stages and calls `processChunk()` per chunk, isolating a failing chunk into
a typed failure rather than failing the whole run. `PropositionPipeline.process(...)` wraps your
resolver with an `InMemoryEntityResolver`. See `how-to/extract-from-documents.adoc`.

#### LlmPropositionExtractor

```java
@Bean
LlmPropositionExtractor llmPropositionExtractor(AiBuilder aiBuilder, ...) {
    return LlmPropositionExtractor
            .withLlm(llmOptions)
            .withAi(ai)
            .withPropositionRepository(propositionRepository)
            .withSchemaAdherence(SchemaAdherence.DEFAULT)
            .withTemplate("dice/extract_impromptu_user_propositions");
}
```

Verbatim from `README.md:143-150`. Note there is no `.builder()` and no terminal `.build()` — the
static `withX(...)` chain *is* the constructor.

Key configuration:
- **withLlm** — LLM options (model, temperature, etc.)
- **withAi** — Ai instance for LLM interaction
- **withPropositionRepository** — Repository for context-aware extraction
- **withSchemaAdherence** — the attested `SchemaAdherence` members are `STRICT`, `DEFAULT` and
  `RELAXED`. There is **no** `LOOSE` member; `RELAXED` is the permissive one.
- **withTemplate** — Resource path to extraction prompt template

### PropositionReviser

Revises existing propositions — merge identical, reinforce similar, contradict conflicting.

```java
public interface PropositionReviser {
    RevisionResult revise(Proposition newProp, Proposition existing, SourceAnalysisContext context);
}
```

Revision outcomes:
| Outcome | Meaning |
|---------|---------|
| `New` | No existing match — proposition is new |
| `Merged` | Identical — merged with existing |
| `Reinforced` | Similar — evidence accumulated |
| `Contradicted` | Conflicting — contradiction recorded |
| `Generalized` | Overlapping — generalized to cover both |

#### LlmPropositionReviser

```java
@Bean
PropositionReviser propositionReviser(Ai ai, LlmOptions llmOptions) {
    return LlmPropositionReviser
        .withLlm(llmOptions)
        .withAi(ai)
        .withPropositionRepository(propositionRepository);
}
```

Same shape as the extractor: a static `withX(...)` chain, no builder, no `.build()`.

### MentionFilter

Filters low-quality mentions before they enter the pipeline.

```java
public interface MentionFilter {
    boolean accept(EntityMention mention, SourceAnalysisContext context);
}
```

#### SchemaValidatedMentionFilter

Validates mentions against the schema's validation rules.

```java
@Bean
MentionFilter mentionFilter(DataDictionary schema) {
    return new SchemaValidatedMentionFilter(schema);
}
```

#### PropositionDuplicateFilter

Prevents duplicate propositions from entering the pipeline.

```java
@Bean
MentionFilter duplicateFilter(PropositionRepository repository) {
    return new PropositionDuplicateFilter(repository);
}
```

#### CompositeMentionFilter

Combines multiple filters.

```java
@Bean
MentionFilter mentionFilter(DataDictionary schema, PropositionRepository repository) {
    return new CompositeMentionFilter(List.of(
        new SchemaValidatedMentionFilter(schema),
        new PropositionDuplicateFilter(repository)
    ));
}
```

`ObservableMentionFilter` exists, but the `LoggingMentionFilterObserver` type shown in earlier
revisions of this guide is **not attested** anywhere in the upstream corpus, so no observer name is
given here — supply your own observer implementation. For reacting to analysis, the attested route is
the Spring events: `SourceAnalysisRequestEvent`, `ConversationAnalysisRequestEvent` and `DiceEvent`
(see `how-to/observe-and-react.adoc`).

## Building the Pipeline

### Minimal Pipeline

```java
@Bean
PropositionPipeline propositionPipeline(PropositionExtractor extractor) {
    return PropositionPipeline.withExtractor(extractor);
}
```

The pipeline is an immutable `with*` data class: each wither returns a **new** instance, so reassign
the result. There is **no** `PropositionPipeline.builder() … .build()` form and **no**
`LlmPropositionExtractor.builder()` / `LlmPropositionReviser.builder()` either (all 0 hits in the
corpus) — every one of these types is built by a static `withX(...)` chain with no terminal `.build()`.

### Full Pipeline (Recommended)

```java
@Bean
PropositionPipeline propositionPipeline(
        PropositionExtractor propositionExtractor,
        PropositionReviser propositionReviser,
        PropositionRepository propositionRepository,
        MentionFilter mentionFilter) {

    return PropositionPipeline
        .withExtractor(propositionExtractor)
        .withRevision(propositionReviser, propositionRepository)
        .withMentionFilter(mentionFilter);
}
```

## Processing Sources

### process() — Process All Content

```java
PropositionResults results = pipeline.process(chunks, context);
```

Processes all content in the source and may extract many propositions. The batch call returns
`PropositionResults` (plural). `process()` isolates a failing chunk into a typed failure, so one bad
chunk does not sink the run.

### processOnce() — Deduplicated Processing

```java
InMemoryChunkHistoryStore historyStore = new InMemoryChunkHistoryStore();

ChunkPropositionResult result = pipeline.processOnce(
    text,
    "source-123",
    context,
    historyStore
);
```

Returns a **nullable** `ChunkPropositionResult` — null when the chunk was already analysed. Uses
hash-based deduplication to prevent reprocessing identical content; `ChunkHistoryStore` records which
chunks have been analysed, and `dice-storage-autoconfigure` provides a bean for whichever backend you
selected.

### Incremental Processing

For a corpus that grows, use incremental analysis rather than reprocessing everything. The attested
types are `IncrementalAnalyzer<T,R>` (interface), `AbstractIncrementalAnalyzer` (the base with
windowing), `PropositionIncrementalAnalyzer`, `EntityIncrementalAnalyzer`, `IncrementalSource<T>`,
`IncrementalSourceFormatter<T>`, `ConversationSource`, `MessageFormatter`, `ChunkHistoryStore`,
`WindowConfig` and `ContentHasher` (package `com.embabel.dice.incremental`).

Construction is a plain **constructor with named arguments** — there is no `.builder()` on any of
these (0 hits). Verbatim from `README.md:326-331`:

```kotlin
val analyzer = PropositionIncrementalAnalyzer(
    pipeline = pipeline,
    historyStore = historyStore,
    formatter = MessageFormatter.INSTANCE,
    config = WindowConfig(windowSize = 20, overlapSize = 2, triggerInterval = 4),
    contentHasher = Sha256ContentHasher,  // pluggable
)

// Returns null if window content was already processed
val result = analyzer.analyze(source, context)
```

`AbstractIncrementalAnalyzer` applies dedup automatically inside its windowed processing: each
window's formatted text is hashed and checked against the `ChunkHistoryStore` before processing, and
`analyze(...)` returns **null** when the window content was already seen. An earlier revision of this
guide showed `AbstractIncrementalAnalyzer.builder(String.class, PropositionResults.class)…build()` and
withers named `withWindowConfig` / `withWindowSize` / `withOverlapSize` / `withReviser` — none of those
exist; windowing is configured by passing a `WindowConfig` to the constructor as above.

## Source Analysis Context

`SourceAnalysisContext` is an immutable `with*` data class. There is **no** `builder()…build()` form
and **no** `ContextId.of(...)` factory — `ContextId` is a Kotlin value class in
`com.embabel.agent.core`, constructed from its string (see `memory.md`). Attested form, verbatim from
`how-to/analyze-conversations.adoc`:

```java
var context = SourceAnalysisContext
        .withContextId(event.user.currentContext())
        .withEntityResolver(entityResolverForUser(event.user))
        .withSchema(dataDictionary)
        .withRelations(relations)
        .withKnownEntities(KnownEntity.asCurrentUser(event.user));
```

Each `withX` returns a new instance, so either start from `SourceAnalysisContext.withX(...)` or from
`new SourceAnalysisContext(dataDictionary, resolver, contextId)` and chain the withers. There is no
terminal `.build()` call.

Key context properties:
- **contextId** — Scopes propositions to a user/session
- **entityResolver** — Resolves entity mentions to canonical entities
- **schema** — DataDictionary for type validation (a `com.embabel.agent.core` type, not a DICE one)
- **relations** — Known relationships between entities
- **knownEntities** — Pre-defined entities (e.g., current user)

## Event-Driven Integration

Conversation analysis costs an LLM call, so it should not sit in the request path —
`ConversationAnalysisRequestEvent` exists for exactly this. Attested form, verbatim from
`how-to/analyze-conversations.adoc`:

```java
@Async
@Transactional
@EventListener
public void onConversationExchange(ConversationAnalysisRequestEvent event) {
    var context = SourceAnalysisContext
            .withContextId(event.user.currentContext())
            .withEntityResolver(entityResolverForUser(event.user))
            .withSchema(dataDictionary)
            .withRelations(relations)
            .withKnownEntities(KnownEntity.asCurrentUser(event.user));

    var source = new ConversationSource(event.conversation);
    var result = analyzer.analyze(source, context);
}
```

Two corrections against earlier revisions of this guide:

- The listener shown previously, `onDocumentProcessed(DocumentProcessedEvent)`, named a type that does
  **not** exist. The attested events are `SourceAnalysisRequestEvent`,
  `ConversationAnalysisRequestEvent` and `DiceEvent`.
- `SourceAnalysisContext` is a `with*` data class: either `SourceAnalysisContext.withX(...)` or
  `new SourceAnalysisContext(dataDictionary, resolver, contextId).withX(...)`. There is **no**
  `SourceAnalysisContext.builder() … .build()` form, and **no** `ContextId.of(...)` factory.

## Common Pitfalls

1. **Not persisting the results** — `process()` returns `PropositionResults` but does not auto-persist;
   persistence goes through the repository (`persist(...)` is attested as an operation).
2. **Not providing an entity resolver** — Without one, entity mentions create new entities every time.
   `PropositionPipeline.process(...)` wraps your resolver with an `InMemoryEntityResolver`.
3. **Not scoping by contextId** — Without context scoping, propositions from different users mix together.
4. **Using `SchemaAdherence.RELAXED` in production** — It admits arbitrary proposition structures. Use
   `STRICT` (or `DEFAULT`) for production. There is no `LOOSE` member.
5. **Not using deduplication** — processOnce with a historyStore prevents reprocessing identical content.
