# Agentic RAG

Embabel Agent provides agentic RAG through `LlmReference`, enabling LLMs to autonomously control retrieval. Key classes: `ToolishRag` (facade exposing `SearchOperations` as LLM tools), `SearchOperations` (search capability interfaces), and `ChunkTransformer` (content enrichment).

## Agentic RAG Architecture

Unlike traditional single-shot RAG, Embabel's RAG is **entirely agentic and tool-based**:

- **Autonomous Search**: The LLM decides when, what, and how many times to search
- **Iterative Refinement**: Multiple searches with different queries until relevant info is found
- **Cross-Reference Discovery**: Expand chunks, zoom out to parent sections, follow references
- **HyDE Support**: The LLM can generate hypothetical documents for better semantic search

The LLM can start broad and narrow down, try different phrasings, expand promising results, and combine information from multiple chunks.

## Facade Pattern

`ToolishRag` inspects the underlying `SearchOperations` and only exposes tools for supported operations:

- A Lucene store exposes vector search, text search, regex search, AND result expansion
- A Spring AI VectorStore adapter exposes only vector search tools
- A basic text-only store exposes only text search tools

The LLM sees only tools that actually work, preventing runtime errors from unsupported operations.

## Getting Started

Add `rag-lucene` and `rag-tika` to your project:

```xml
<dependency>
    <groupId>com.embabel.agent</groupId>
    <artifactId>embabel-agent-rag-lucene</artifactId>
    <version>${embabel-agent.version}</version>
</dependency>
<dependency>
    <groupId>com.embabel.agent</groupId>
    <artifactId>embabel-agent-rag-tika</artifactId>
    <version>${embabel-agent.version}</version>
</dependency>
```

`rag-lucene` provides vector and text search; `rag-tika` provides document parsing (PDF, Word, HTML, Markdown).

### Lucene Store Configuration

```java
@Bean
LuceneSearchOperations luceneSearchOperations(ModelProvider modelProvider, RagbotProperties properties) {
    var embeddingService = modelProvider.getEmbeddingService(DefaultModelSelectionCriteria.INSTANCE);
    return LuceneSearchOperations.withName("docs")
        .withEmbeddingService(embeddingService)
        .withChunkerConfig(properties.chunkerConfig())
        .withIndexPath(Paths.get("./.lucene-index"))
        .buildAndLoadChunks();
}
```

Omit `.withIndexPath()` for in-memory-only storage.

### Creating ToolishRag

```java
public ChatActions(SearchOperations searchOperations) {
    this.toolishRag = new ToolishRag("sources", "Sources for answering user questions", searchOperations);
}
```

### Using with LLM Calls

Attach via `.withReference()`:

```java
@Action(canRerun = true, trigger = UserMessage.class)
void respond(Conversation conversation, ActionContext context) {
    var assistantMessage = context.ai()
        .withLlm(properties.chatLlm())
        .withReference(toolishRag)
        .rendering("ragbot")
        .respondWithSystemPrompt(conversation, Map.of());
    context.sendMessage(conversation.addMessage(assistantMessage));
}
```

```kotlin
@Action(canRerun = true, trigger = UserMessage::class)
fun respond(conversation: Conversation, context: ActionContext) {
    val assistantMessage = context.ai()
        .withLlm(properties.chatLlm())
        .withReference(toolishRag)
        .rendering("ragbot")
        .respondWithSystemPrompt(conversation, emptyMap())
    context.sendMessage(conversation.addMessage(assistantMessage))
}
```

The LLM autonomously decides when to use search tools based on user queries.

## Content Model

Embabel uses a hierarchical content model beyond flat chunks:

- **`Datum`** is the root sealed interface for all data objects
- **`ContentElement`** contains structural content (documents, sections) that is NOT embedded:
  - `ContentRoot` / `NavigableDocument` — root with URI and title
  - `ContainerSection` — section containing other sections
  - `LeafSection` — section containing actual text
- **`Retrievable`** contains searchable content with embeddings:
  - `Chunk` — text, parentId, embedding (primary vector-search unit)
  - `NamedEntity` — domain entity contract (Person, Product, etc.)
- **`Chunk`** has `text`, a `parentId` linking to its source section, and metadata — it can compute `pathFromRoot`, enabling "zoom out" to parent sections and expansion to adjacent chunks

## SearchOperations

`SearchOperations` is a tag interface. Stores implement only the subinterfaces that fit their capabilities:

| Interface | Purpose |
|---|---|
| `VectorSearch` | Semantic vector similarity search |
| `TextSearch` | Full-text search (Lucene query syntax) |
| `RegexSearchOperations` | Pattern-based regex search |
| `ResultExpander` | Expand results to surrounding context |
| `CoreSearchOperations` | Convenience: combines `VectorSearch` + `TextSearch` |

### VectorSearch

```java
public interface VectorSearch extends SearchOperations {
    <T extends Retrievable> List<SimilarityResult<T>> vectorSearch(TextSimilaritySearchRequest request, Class<T> clazz);
}
```

Exposes the `vectorSearch(query, topK, threshold)` tool to the LLM.

### TextSearch

Full-text search using Lucene query syntax:

| Syntax | Meaning |
|---|---|
| `+term` | term must appear |
| `-term` | term must not appear |
| `"phrase"` | exact phrase match |
| `term*` | prefix wildcard |
| `term~` | fuzzy match |

### ResultExpander

- `SEQUENCE` — expand to previous and next chunks
- `ZOOM_OUT` — expand to enclosing section

Exposes `broadenChunk(chunkId, chunksToAdd)` and `zoomOut(id)` tools.

### RegexSearchOperations

Pattern-based search for error codes, identifiers, structured content:

```java
public interface RegexSearchOperations extends SearchOperations {
    <T extends Retrievable> List<SimilarityResult<T>> regexSearch(Pattern regex, int topK, Class<T> clazz);
}
```

### Exposed LLM Tools

`vectorSearch(query, topK, threshold)`, `textSearch(query, topK, threshold)`, `regexSearch(regex, topK)`, `broadenChunk(chunkId, chunksToAdd)`, `zoomOut(id)`.
## Eager Search

By default, `ToolishRag` is entirely agentic. For known topics, preload results before the LLM starts:

```java
ToolishRag eagerRag = toolishRag.withEagerSearchAbout("Kotlin coroutines", 10);
context.ai().withReference(eagerRag).respondWithSystemPrompt(conversation, Map.of());
```

For more control, pass a `TextSimilaritySearchRequest` directly: `new TextSimilaritySearchRequest("Kotlin coroutines", 0.7, 10)`.

Eager search requires `VectorSearch` support. Preloaded results are included as hints — the LLM still has full access to search tools for follow-up queries.

## Result Filtering

Filters provide per-request scoping in multi-tenant apps without creating separate RAG stores per user. Filters are applied transparently — the LLM cannot see or bypass them.

### PropertyFilter

Type-safe filter expressions for map-based properties:

| Filter | Description | Example |
|---|---|---|
| `Eq` / `Ne` | Equals / Not equals | `eq("owner", "alice")` |
| `Gt`/`Gte` / `Lt`/`Lte` | Comparison | `gte("score", 0.8)` |
| `In` / `Nin` | List membership | `in("category", "tech", "science")` |
| `Contains` | String contains substring | `contains("tags", "important")` |
| `And` / `Or` / `Not` | Logical combinators | `and(f1, f2)`, `or(f1, f2)`, `not(f)` |

### EntityFilter

Extends `PropertyFilter` with label-based filtering:

```java
EntityFilter personFilter = EntityFilter.hasAnyLabel("Person");
PropertyFilter complexFilter = EntityFilter.hasAnyLabel("Person")
    .and(PropertyFilter.eq("status", "active")).and(PropertyFilter.gte("score", 0.8));
```

### Applying Filters

```java
ToolishRag scopedRag = toolishRag
    .withMetadataFilter(PropertyFilter.eq("ownerId", currentUserId))
    .withEntityFilter(EntityFilter.hasAnyLabel("Person"));
```

Filters are applied transparently — the LLM cannot see or bypass them. Neo4j translates both filter types to native Cypher WHERE clauses; Spring AI VectorStore handles metadata natively, entities in-memory; Lucene applies both as post-filters.

## Ingestion

### Document Parsing with Tika

```java
@ShellMethod("Ingest URL or file path")
String ingest(@ShellOption(defaultValue = "./data/document.md") String location) {
    var uri = location.startsWith("http://") || location.startsWith("https://")
        ? location : Path.of(location).toAbsolutePath().toUri().toString();
    var ingested = NeverRefreshExistingDocumentContentPolicy.INSTANCE
        .ingestUriIfNeeded(luceneSearchOperations, new TikaHierarchicalContentReader(), uri);
    return ingested != null ? "Ingested document with ID: " + ingested
        : "Document already exists, no ingestion performed.";
}
```

### Chunking Configuration

```yaml
ragbot:
  chunker-config:
    max-chunk-size: 800
    overlap-size: 100
```

| Option | Default | Description |
|---|---|---|
| `maxChunkSize` | 1500 | Max characters per chunk |
| `overlapSize` | 200 | Character overlap between chunks |
| `includeSectionTitleInChunk` | true | Include section title in chunk text |

### Chunk Transformation

Every `Chunk` has **`text`** (indexed, may be transformed) and **`urtext`** (original, for citations).

#### AddTitlesChunkTransformer (recommended default)

```java
@Bean
ChunkTransformer chunkTransformer() { return AddTitlesChunkTransformer.INSTANCE; }
```

Transforms `This approach improves performance by 40%` into a chunk with `# Title: Performance Optimization Guide`, `# URI:`, and `# Section:` headers prepended.

#### Custom and Chained Transformers

Extend `AbstractChunkTransformer` to add metadata or modify text:

```java
public class MetadataEnrichingTransformer extends AbstractChunkTransformer {
    @Override
    public Map<String, Object> additionalMetadata(Chunk chunk, ChunkTransformationContext context) {
        return Map.of("documentType", context.getDocument().getMetadata().get("type"));
    }
}
```

Chain: `new ChainedChunkTransformer(List.of(AddTitlesChunkTransformer.INSTANCE, new MetadataEnrichingTransformer()))`.

#### Configuring the Store

```java
@DependsOn("onnxEmbeddingInitializer")
@Bean
DrivineStore drivineStore(PersistenceManager pm, EmbeddingService es, ChunkTransformer ct, MyProperties props) {
    return new DrivineStore(pm, props.neoRag(), props.chunkerConfig(), ct, es, platformTransactionManager,
        new DrivineCypherSearch(pm));
}
```

## Supported Stores

- **Lucene** (`embabel-agent-rag-lucene`): Vector + text + regex + expansion
- **Neo4j** (`embabel-agent-rag-neo-drivine`, `embabel-agent-rag-neo-ogm`): Graph-based with relationships
- **PostgreSQL pgvector** (`embabel-rag-pgvector` separate repo): Hybrid vector + full-text + fuzzy
- **Spring AI VectorStore** (built-in adapter): Any Spring AI vector store (Pinecone, Weaviate, Milvus, Chroma)

## Implementing a Custom Store

Implement only the `SearchOperations` subinterfaces that fit your store. A vector database implements only `VectorSearch`; a full-text engine implements `TextSearch` + `RegexSearchOperations`; a full-featured store like Lucene implements all. `ToolishRag` automatically exposes only the tools your store supports — no stub implementations needed. For ingestion support, extend `ChunkingContentElementRepository`.
---

*Source: Embabel Agent v1.0.0 documentation*
