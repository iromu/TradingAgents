# Proposition Pipeline Reference

## Pipeline Architecture

```
Source → IncrementalAnalyzer → PropositionPipeline → Repository
```

The pipeline is the core of DICE. It processes text sources, extracts propositions, resolves entities, revises existing
propositions, and stores results.

## Core Components

### PropositionExtractor

Extracts typed propositions from text using an LLM.

```java
public interface PropositionExtractor {
    PropositionResult extract(String text, SourceAnalysisContext context);
}
```

#### LlmPropositionExtractor

```java
@Bean
PropositionExtractor propositionExtractor(Ai ai, LlmOptions llmOptions) {
    return LlmPropositionExtractor.builder()
        .withLlm(llmOptions)
        .withAi(ai)
        .withPropositionRepository(propositionRepository)
        .withSchemaAdherence(SchemaAdherence.STRICT)
        .withTemplate("dice/extract_user_propositions")
        .build();
}
```

Key configuration:

- **withLlm** — LLM options (model, temperature, etc.)
- **withAi** — Ai instance for LLM interaction
- **withPropositionRepository** — Repository for context-aware extraction
- **withSchemaAdherence** — STRICT (enforce schema) or LOOSE (flexible)
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
    return LlmPropositionReviser.builder()
        .withLlm(llmOptions)
        .withAi(ai)
        .withPropositionRepository(propositionRepository)
        .build();
}
```

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
        new PropositionDuplicateFilter(repository),
        new ObservableMentionFilter(new LoggingMentionFilterObserver())
    ));
}
```

## Building the Pipeline

### Minimal Pipeline

```java
@Bean
PropositionPipeline propositionPipeline(PropositionExtractor extractor) {
    return PropositionPipeline.builder()
        .withExtractor(extractor)
        .build();
}
```

### Full Pipeline (Recommended)

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

## Processing Sources

### process() — Process All Content

```java
PropositionResult result = pipeline.process(fullText, context);
result.persist(propositionRepository, entityRepository);
```

Processes all content in the source. May extract many propositions.

### processOnce() — Deduplicated Processing

```java
InMemoryChunkHistoryStore historyStore = new InMemoryChunkHistoryStore();

PropositionResult result = pipeline.processOnce(
    text,
    "source-123",
    context,
    historyStore
);
```

Uses hash-based deduplication to prevent reprocessing identical content. The `historyStore` tracks which content has
been processed.

### Incremental Processing

For streaming or windowed analysis:

```java
@Bean
IncrementalAnalyzer<String, PropositionResult> incrementalAnalyzer(
        PropositionExtractor extractor,
        PropositionReviser reviser,
        PropositionRepository repository) {

    return AbstractIncrementalAnalyzer.builder(String.class, PropositionResult.class)
        .withExtractor(extractor)
        .withReviser(reviser, repository)
        .withWindowConfig(WindowConfig.builder()
            .withWindowSize(20)
            .withOverlapSize(5)
            .build())
        .build();
}
```

## Source Analysis Context

```java
SourceAnalysisContext context = SourceAnalysisContext.builder()
    .withContextId(ContextId.of("user-123-session-456"))
    .withEntityResolver(entityResolver)
    .withSchema(dataDictionary)
    .withRelations(relations)
    .withKnownEntities(KnownEntity.asCurrentUser(currentUser))
    .build();
```

Key context properties:

- **contextId** — Scopes propositions to a user/session
- **entityResolver** — Resolves entity mentions to canonical entities
- **schema** — DataDictionary for type validation
- **relations** — Known relationships between entities
- **knownEntities** — Pre-defined entities (e.g., current user)

## Event-Driven Integration

Process propositions from Spring events:

```java
@Async
@Transactional
@EventListener
public void onDocumentProcessed(DocumentProcessedEvent event) {
    var context = SourceAnalysisContext.builder()
        .withContextId(ContextId.of(event.userId()))
        .withEntityResolver(entityResolver)
        .withSchema(dataDictionary)
        .build();

    var result = pipeline.process(event.getContent(), context);
    result.persist(propositionRepository, entityRepository);
}
```

## Common Pitfalls

1. **Not calling persist()** — process() returns results but doesn't auto-persist. Call
   `result.persist(repository, entityRepository)`.
2. **Not providing an entity resolver** — Without one, entity mentions create new entities every time.
3. **Not scoping by contextId** — Without context scoping, propositions from different users mix together.
4. **Using LOOSE schema adherence** — Allows arbitrary proposition structures. Use STRICT for production.
5. **Not using deduplication** — processOnce with a historyStore prevents reprocessing identical content.
