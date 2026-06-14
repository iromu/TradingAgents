Source: https://docs.embabel.com/embabel-agent/guide/0.5.0-SNAPSHOT/

### 4.12. RAG (Retrieval-Augmented Generation)
Retrieval-Augmented Generation (RAG) is a technique that enhances LLM responses by retrieving relevant information from a knowledge base before generating answers.
This grounds LLM outputs in specific, verifiable sources rather than relying solely on training data.For more background on RAG concepts, see:
- Wikipedia: Retrieval-Augmented Generation
- AWS: What is RAG?Embabel Agent provides RAG support through the `LlmReference` interface, which allows you to attach references (including RAG stores) to LLM calls.
The key classes are `ToolishRag` for exposing search operations as LLM tools, and `SearchOperations` for the underlying search functionality.
#### 4.12.1. Agentic RAG Architecture
Unlike traditional RAG implementations that perform a single retrieval step, Embabel Agent’s RAG is **entirely agentic and tool-based**.
The LLM has full control over the retrieval process:
- **Autonomous Search**: The LLM decides when to search, what queries to use, and how many results to retrieve
- **Iterative Refinement**: The LLM can perform multiple searches with different queries until it finds relevant information
- **Cross-Reference Discovery**: The LLM can follow references, expand chunks to see surrounding context, and zoom out to parent sections
- **HyDE Support**: The LLM can generate hypothetical documents (HyDE queries) to improve semantic search resultsThis agentic approach produces better results than single-shot RAG because the LLM can:
1.Start with a broad search and narrow down
1.Try different phrasings if initial queries return poor results
1.Expand promising results to get more context
1.Combine information from multiple chunks
#### 4.12.2. Facade Pattern for Safe Tool Exposure
Embabel Agent uses a facade pattern to expose RAG capabilities safely and consistently across different store implementations.
The `ToolishRag` class acts as a facade that:
1.**Inspects Store Capabilities**: Examines which `SearchOperations` subinterfaces the store implements
1.**Exposes Appropriate Tools**: Only creates tool wrappers for supported operations
1.**Provides Consistent Interface**: All tools use the same parameter patterns regardless of underlying storeJavaKotlinThis means:
- A Lucene store exposes vector search, text search, regex search, AND result expansion tools
- A Spring AI VectorStore adapter exposes only vector search tools
- A basic text-only store exposes only text search tools
- A directory-based text search exposes text search and regex searchThe LLM sees only the tools that actually work with the configured store, preventing runtime errors from unsupported operations.
#### 4.12.3. Getting Started
To use RAG in your Embabel Agent application, add the `rag-core` module and a store implementation to your `pom.xml`:The `embabel-agent-rag-lucene` module provides Lucene-based vector and text search.
The `embabel-agent-rag-tika` module provides Apache Tika integration for parsing various document formats.
#### 4.12.4. Our Model
Embabel Agent uses a hierarchical content model that goes beyond traditional flat chunk storage:
```
Datum (sealed interface)
│ Core: id, uri, metadata, labels()
│
├── ContentElement ─────────────────────────────────────┐
│ Structural content (not embedded) │
│ ┌───────────────────────────────────────────────┐ │
│ │ ContentRoot / NavigableDocument │ │
│ │ Documents with URI and title │ │
│ └───────────────────────────────────────────────┘ │
│ ┌───────────────────────────────────────────────┐ │
│ │ ContainerSection / LeafSection │ │
│ │ Hierarchical document sections │ │
│ └───────────────────────────────────────────────┘ │
│ │
└── Retrievable ────────────────────────────────────────┤
 Embeddable/searchable content │
 ┌───────────────────────────────────────────────┐ │
 │ Chunk │ │
 │ text, parentId, embedding │ │
 │ Primary unit for vector search │ │
 └───────────────────────────────────────────────┘ │
 ┌───────────────────────────────────────────────┐ │
 │ NamedEntity │ │
 │ Domain entity contract (Person, Product) │ │
 │ name, description + domain properties │ │
 │ │ │
 │ └── NamedEntityData │ │
 │ Storage format with properties map │ │
 │ Hydration via toTypedInstance() │ │
 └───────────────────────────────────────────────┘ │
 │
────────────────────────────────────────────────────────┘
```
**Key Design Points:**
- `Datum` is the root sealed interface for all data objects
- `ContentElement` branch contains structural content (documents, sections) that is NOT embedded
- `Retrievable` branch contains searchable content with embeddings (chunks, entities)
- `NamedEntity` is the domain contract for typed entities
- `NamedEntityData` is the storage format with generic `properties` map and hydration support
##### Content Elements
The `ContentElement` interface is the supertype for all content in the RAG system.
Key subtypes include:
- **`ContentRoot`** / **`NavigableDocument`**: The root of a document hierarchy, with a required URI and title
- **`Section`**: A hierarchical division of content with a title
- **`ContainerSection`**: A section containing other sections
- **`LeafSection`**: A section containing actual text content
- **`Chunk`**: Traditional RAG text chunks, created by splitting `LeafSection` content
##### Chunks
`Chunk` is the primary unit for vector search.
Each chunk:
- Contains a `text` field with the content
- Has a `parentId` linking to its source section
- Includes `metadata` with information about its origin (root document, container section, leaf section)
- Can compute its `pathFromRoot` through the document hierarchyThis hierarchical model enables advanced RAG capabilities like "zoom out" to parent sections or expansion to adjacent chunks.
#### 4.12.5. SearchOperations
`SearchOperations` is the tag interface for search functionality.
Concrete implementations implement one or more subinterfaces based on their capabilities.
This design allows stores to implement only what’s natural and efficient for them—a vector database need not pretend to support full-text search, and a text search engine need not fake vector similarity.
##### VectorSearch
Classic semantic vector search:JavaKotlin
##### TextSearch
Full-text search using Lucene query syntax:JavaKotlinSupported query syntax includes:
- `+term` - term must appear
- `-term` - term must not appear
- `"phrase"` - exact phrase match
- `term*` - prefix wildcard
- `term~` - fuzzy match
##### ResultExpander
Expand search results to surrounding context:JavaKotlinExpansion methods:
- `SEQUENCE` - expand to previous and next chunks
- `ZOOM_OUT` - expand to enclosing section
##### RegexSearchOperations
Pattern-based search across content:JavaKotlinUseful for finding specific patterns like error codes, identifiers, or structured content that doesn’t match well with semantic or keyword search.
##### CoreSearchOperations
A convenience interface combining the most common search capabilities:JavaKotlinStores that support both vector and text search can implement this single interface for convenience.
#### 4.12.6. ToolishRag
`ToolishRag` is an `LlmReference` that exposes `SearchOperations` as LLM tools.
This gives the LLM fine-grained control over RAG searches.
##### Configuration
Create a `ToolishRag` by wrapping your `SearchOperations`:JavaKotlin
##### Using with LLM Calls
Attach `ToolishRag` to an LLM call using `.withReference()`:JavaKotlinBased on the capabilities of the underlying `SearchOperations`, `ToolishRag` exposes:
- **VectorSearchTools**: `vectorSearch(query, topK, threshold)` - semantic similarity search
- **TextSearchTools**: `textSearch(query, topK, threshold)` - BM25 full-text search with Lucene syntax
- **RegexSearchTools**: `regexSearch(regex, topK)` - pattern-based search using regular expressions
- **ResultExpanderTools**: `broadenChunk(chunkId, chunksToAdd)` - expand to adjacent chunks, `zoomOut(id)` - expand to parent sectionThe LLM autonomously decides when to use these tools based on user queries.
##### Eager Search
By default, `ToolishRag` is entirely agentic—the LLM decides when to search and what queries to use.
However, when the topic of the conversation is already known, you can **preload** relevant results before the LLM starts, giving it a head start and reducing the number of tool calls needed.`ToolishRag` implements the `EagerSearch` interface, which provides `withEagerSearchAbout()`:JavaKotlinThe preloaded results are included in the prompt as hints. The LLM still has access to all the usual search tools and can perform additional searches as needed.For more control over the search parameters, pass a `TextSimilaritySearchRequest` directly:JavaKotlin
| | Combining eager search with agentic tools is the sweet spot: preloaded results give the LLM an immediate head start (no round-trip needed), while the tools remain available for follow-up searches if the preloaded results aren’t sufficient. You get the latency benefit of traditional RAG with the quality benefit of agentic RAG. |

| | Eager search requires `VectorSearch` support in the underlying `SearchOperations`. If the store does not support vector search, `withEagerSearchAbout()` throws `UnsupportedOperationException` eagerly at configuration time. |

| | The `EagerSearch` Interface`EagerSearch<T>` is a general-purpose interface in the `com.embabel.agent.api.reference` package for any `LlmReference` that can preload context via similarity search.
`ToolishRag` is one implementation, but other reference types can implement `EagerSearch` to provide the same consistent pattern for preloading relevant context before an LLM call. |

##### ToolishRag lifecycle
It is safe to create a `ToolishRag` instance and reuse across many LLM calls.
However, instances are not expensive to create, so you can create a new instance per LLM call.
You might choose to do this if you provide a `ResultListener`
that will collect queries and results for logging or analysis: for example, to track which queries were most useful for answering user questions and the complexity in terms of number of searches performed.
This can be useful for implementing a learning feedback loop, for example to discern which queries performed badly, indicating that content such as documentation needs to be enhanced.
##### Result Filtering
In multi-tenant applications or scenarios where searches should be scoped to specific data subsets, `ToolishRag` supports **result filtering**.
Filters are applied transparently to all searches—the LLM does not see or control them, ensuring security and data isolation.Embabel Agent provides two types of filters:
- **Metadata Filters**: Filter on the `metadata` map of `Datum` objects (chunks, sections, etc.)
- **Property Filters**: Filter on object properties of typed entities (e.g., fields of `NamedEntityData` or custom entity classes)Both use the same `PropertyFilter` type but are applied at different levels.
###### Motivation
Consider a document management system where:
- Each document belongs to an owner (user or organization)
- Some documents are shared reference data accessible to all users
- The LLM should only search documents the current user is authorized to accessWithout filtering, you would need separate RAG stores per user or risk data leakage.
With filtering, a single `ToolishRag` instance can be scoped per-request to the current user’s data.
###### Filter API
Embabel Agent provides two filter interfaces for RAG searches:
- **`PropertyFilter`**: Filters on map-based properties (metadata, entity properties)
- **`EntityFilter`**: Extends `PropertyFilter` to add entity-specific filtering, particularly label-based filtering
###### PropertyFilter
The `PropertyFilter` sealed class hierarchy provides type-safe filter expressions for map-based properties:
| Filter Type | Description | Example || `Eq` | Equals | `PropertyFilter.eq("owner", "alice")` || `Ne` | Not equals | `PropertyFilter.ne("status", "deleted")` || `Gt`, `Gte` | Greater than (or equal) | `PropertyFilter.gte("score", 0.8)` || `Lt`, `Lte` | Less than (or equal) | `PropertyFilter.lt("priority", 5)` || `In` | Value in list | `PropertyFilter.in("category", "tech", "science")` || `Nin` | Value not in list | `PropertyFilter.nin("status", "deleted", "archived")` || `Contains` | String contains substring | `PropertyFilter.contains("tags", "important")` || `And` | Logical AND | `PropertyFilter.and(filter1, filter2)` || `Or` | Logical OR | `PropertyFilter.or(filter1, filter2)` || `Not` | Logical NOT | `PropertyFilter.not(filter)` |

###### EntityFilter
`EntityFilter` extends `PropertyFilter` to add entity-specific filtering. Currently, it adds label-based filtering via `HasAnyLabel`:
| Filter Type | Description | Example || `HasAnyLabel` | Matches entities with any of the specified labels | `EntityFilter.hasAnyLabel("Person", "Organization")` |
`HasAnyLabel` is particularly useful for:
- **Type-safe entity searches**: Filter results to only include specific entity types
- **Multi-type queries**: Search across multiple entity types in one queryJavaKotlinSince `EntityFilter` extends `PropertyFilter`, all filter types share the same `and`, `or`, `not` operators and can be freely combined.
| | `EntityFilter.HasAnyLabel` is typically handled via in-memory filtering as most vector stores don’t have native label support. When using Neo4j backends, labels can be translated to native Cypher label predicates for optimal performance. |

| | **Limitation: Nested Properties Not Supported**Filters currently operate on top-level properties only. Nested property paths like `"address.city"` or `"metadata.source"` are **not** supported.
The filter key must match a direct key in the metadata map or a top-level property on the entity object.For example:
- `PropertyFilter.eq("owner", "alice")` - **Supported**: filters on top-level `owner` property
- `PropertyFilter.eq("address.city", "London")` - **Not supported**: nested path will not match |

###### Kotlin Operator Syntax
Kotlin users can use operator and infix functions for a more natural DSL syntax:JavaKotlin
###### Metadata vs Entity Filters
`ToolishRag` accepts two separate filter parameters:
- **`metadataFilter`**: A `PropertyFilter` that filters on the `metadata` map of `Datum` objects. Metadata is typically ingestion-time information like source URI, ingestion date, owner ID, etc.
- **`entityFilter`**: An `EntityFilter` that filters on entity properties and labels. For `NamedEntityData`, this filters on the `properties` map and `labels()`. For typed entities, reflection is used to access top-level fields.JavaKotlinIn most cases, you’ll use metadata filters for access control and entity filters for type-based and business logic filtering.
##### Neo4j Cypher Filtering
When using Neo4j via the Drivine module, metadata filters are automatically converted to Cypher WHERE clauses using `CypherFilterConverter`:JavaKotlinThe converter produces parameterized queries for safety and handles all filter types including nested logical expressions.For both `DrivineStore` (chunks) and `DrivineNamedEntityDataRepository` (named entities), **both** metadata and property filters are translated to native Cypher WHERE clauses. This is because Neo4j stores all data as node properties - metadata is simply the set of properties that aren’t core fields like `id`, `text`, `parentId`, etc. This provides optimal performance by filtering at the database level rather than in-memory.
###### Basic Usage
Apply a metadata filter to scope all searches to a specific owner:JavaKotlin
###### Complex Filters
Combine filters for more sophisticated access control:JavaKotlin
###### Per-Request Scoping Pattern
A common pattern is to create a scoped `ToolishRag` per request in a web application:JavaKotlin
###### Backend Implementation
Filters are applied at different levels depending on the backend:
- **Spring AI VectorStore**: Metadata filters are translated to `Filter.Expression` for native filtering; entity filters (including `HasAnyLabel`) are applied in-memory
- **Neo4j (Drivine)**: Both metadata and entity filters (including `HasAnyLabel`) are translated to native Cypher WHERE clauses and label predicates (optimal performance)
- **Lucene**: Both filter types are applied as post-filters with inflated `topK` to compensate for filtered-out results
- **Custom stores**: Can implement `FilteringVectorSearch` / `FilteringTextSearch` for native translation, or fall back to in-memory filteringThe `InMemoryPropertyFilter` utility class provides fallback filtering for any store implementation:JavaKotlinFor `EntityFilter.HasAnyLabel`, the in-memory filter checks if the entity has any of the specified labels via `NamedEntityData.labels()`.This ensures filtering works across all backends, with native optimization for metadata filters where available.
#### 4.12.7. Ingestion

##### Document Parsing with Tika
Embabel Agent uses Apache Tika for document parsing. `TikaHierarchicalContentReader` reads various formats (Markdown, HTML, PDF, Word, etc.) and extracts a hierarchical structure:JavaKotlin
##### Chunking Configuration
Content is split into chunks with configurable parameters:Configuration options:
- `maxChunkSize` - Maximum characters per chunk (default: 1500)
- `overlapSize` - Character overlap between consecutive chunks (default: 200)
- `includeSectionTitleInChunk` - Include section title in chunk text (default: true)
##### Chunk Transformation
When chunks are created from documents, they often lack the context needed for effective retrieval.
A chunk containing "This approach improves performance by 40%" is not useful unless the reader knows what "this approach" refers to.
The `ChunkTransformer` interface allows you to enrich chunks with additional context before they are indexed.
###### The urtext Field
Every `Chunk` has two text fields:
- `text` - The indexed content, which may be transformed with additional context
- `urtext` - The original, unmodified chunk textThe `urtext` field preserves the original content for accurate citations.
When displaying search results to users, use `urtext` to show exactly what appeared in the source document, while using the enriched `text` for vector embeddings and search.
###### AddTitlesChunkTransformer
The recommended default transformer is `AddTitlesChunkTransformer`, which prepends document and section titles to each chunk:JavaKotlinThis transforms a chunk like:
```
This approach improves performance by 40% compared to the baseline.
```
Into:
```
# Title: Performance Optimization Guide
# URI: https://docs.example.com/performance
# Section: Caching Strategies

This approach improves performance by 40% compared to the baseline.
```
Now the chunk carries its context, improving both retrieval accuracy and LLM understanding.
###### Custom Transformers
You can create custom transformers by implementing `ChunkTransformer` or extending `AbstractChunkTransformer`:JavaKotlinThe `ChunkTransformationContext` provides access to:
- `section` - The `Section` containing this chunk
- `document` - The `ContentRoot` (may be null for orphan sections)
###### Chaining Transformers
Use `ChainedChunkTransformer` to apply multiple transformations in sequence:JavaKotlinTransformers are applied in order, with each receiving the output of the previous transformer.
###### Configuring the Store
Pass your `ChunkTransformer` to the store implementation:JavaKotlin
| **1** | Ensure the `EmbeddingService` bean is registered before this configuration is wired (see note below) || **2** | Inject the `ChunkTransformer` bean || **3** | Pass it to the store constructor |

| | `EmbeddingService` beans are registered dynamically by model provider auto-configurations via `registerSingleton`.
If your `@Configuration` class injects `EmbeddingService` directly (as above), you should add `@DependsOn` on the provider’s initializer bean — e.g. `@DependsOn("onnxEmbeddingInitializer")` for the ONNX provider.
Without it, Spring may resolve the dependency before the initializer has run, resulting in a `NoSuchBeanDefinitionException`.
This is only necessary when consuming model beans directly; framework beans like `ModelProvider` handle this internally. |

| | For most use cases, `AddTitlesChunkTransformer` is all you need.
It adds essential context that significantly improves retrieval quality without adding complexity. |

##### Using Docling for Markdown Conversion
While we believe that you should write your Gen AI **applications** in Java or Kotlin, ingestion is more in the realm of data science, and Python is indisputably strong in this area.For complex documents like PDFs, consider using Docling to convert to Markdown first:Markdown is easier to parse hierarchically and produces better chunks than raw PDF extraction.
#### 4.12.8. Supported Stores
Embabel Agent provides several RAG store implementations:
##### Lucene (embabel-agent-rag-lucene)
Full-featured store with vector search, text search, and result expansion.
Supports both in-memory and file-based persistence:JavaKotlinOmit `.withIndexPath()` for in-memory only storage.
##### Neo4j
Graph database store for RAG (available in separate modules `embabel-agent-rag-neo-drivine` and `embabel-agent-rag-neo-ogm`).
Ideal when you need graph relationships between content elements.
##### PostgreSQL pgvector (embabel-rag-pgvector)
PostgreSQL-based RAG store using the pgvector extension (available in the separate `embabel/embabel-rag-pgvector` repository).
Supports hybrid search combining vector similarity, full-text search via tsvector/tsquery, and fuzzy matching via pg_trgm.
Ideal when you already use PostgreSQL and want a familiar, battle-tested database for RAG.
##### Spring AI VectorStore (SpringVectorStoreVectorSearch)
Adapter that wraps any Spring AI `VectorStore`, enabling use of any vector database Spring AI supports:JavaKotlinThis allows integration with Pinecone, Weaviate, Milvus, Chroma, and other stores via Spring AI.
#### 4.12.9. Implementing Your Own RAG Store
To implement a custom RAG store, implement only the `SearchOperations` subinterfaces that are natural and efficient for your store.
This is a key design principle: **stores should only implement what they can do well**.For example:
- A **vector database** like Pinecone might implement only `VectorSearch` since that’s its strength
- A **full-text search engine** might implement `TextSearch` and `RegexSearchOperations`
- A **hierarchical document store** might add `ResultExpander` for context expansion
- A **full-featured store** like Lucene can implement all interfacesThe `ToolishRag` facade automatically exposes only the tools that your store supports.
This means you don’t need to provide stub implementations or throw "not supported" exceptions—simply don’t implement interfaces that don’t fit your store’s capabilities.JavaKotlinFor ingestion support, extend `ChunkingContentElementRepository` to handle document storage and chunking.
#### 4.12.10. Complete Example
See the rag-demo project for a complete working example including:
- Lucene-based RAG store configuration
- Document ingestion via Tika
- Chatbot with RAG-powered responses
- Jinja prompt templates for system prompts
- Spring Shell commands for interactive testing