# RAG Architecture Reference

Source: [embabel/embabel-agent-docs/rag.md](https://github.com/embabel/embabel/blob/main/embabel-agent-docs/rag.md)

## RAGBuilder

Fluent builder for constructing RAG systems.

```java
RagBuilder.builder()
    .ragSources(List.of(fileRagSource))
    .filterBuilder(filterBuilder)
    .promptTemplate(promptTemplate)
    .build();
```

## RAGSource

A source of documents for RAG retrieval. Common implementations:

| Implementation | Use Case |
|---------------|----------|
| `FileRagSource` | Documents on the filesystem |
| `WebRagSource` | URLs to scrape |
| `DatabaseRagSource` | Data from a database |
| `ApiRagSource` | Data from an external API |
| `CompositeRagSource` | Multiple sources combined |

### FileRagSource

```java
RagSource fileSource = FileRagSource.builder()
    .directory(Paths.get("/path/to/documents"))
    .fileExtensions(List.of(".pdf", ".txt", ".md"))
    .build();
```

### WebRagSource

```java
RagSource webSource = WebRagSource.builder()
    .urls(List.of("https://example.com/docs"))
    .build();
```

## FilterBuilder

Builds structured filters for targeted RAG retrieval.

```java
// Filter by metadata
Filter filter = FilterBuilder.builder()
    .addFilter("source", "products.pdf")
    .addFilter("category", "technical")
    .build();

// Filter by content similarity (semantic)
Filter filter = FilterBuilder.builder()
    .addSimilarityFilter("What is RAG?", 0.7)
    .build();

// Composite filter
Filter filter = FilterBuilder.builder()
    .addFilter("source", "products.pdf")
    .addSimilarityFilter("pricing", 0.7)
    .build();
```

## Filter

Structured filter for RAG retrieval.

```java
// Check if a document matches a filter
boolean matches = filter.matches(documentMetadata);

// Get filter as JSON for API calls
String filterJson = filter.toJson();
```

## PromptTemplate

Reusable templates for RAG prompts using Jinja2 syntax.

```java
PromptTemplate template = PromptTemplate.builder()
    .template("""
        Context:
        {% for source in sources %}
        [Source: {{ source.metadata.source }}]
        {{ source.content }}
        {% endfor %}

        Question: {{ question }}

        Answer based on the context above:
        """)
    .build();

// Render with variables
String rendered = template.render(Map.of(
    "sources", sources,
    "question", "What is RAG?"
));
```

## RAG Execution

```java
// Build the RAG system
RagSystem rag = RagBuilder.builder()
    .ragSources(List.of(fileSource))
    .filterBuilder(filterBuilder)
    .promptTemplate(promptTemplate)
    .build();

// Execute RAG
RagResult result = rag.execute("What is RAG?");

// Access results
List<RagSource> sources = result.sources();  // Retrieved sources
String context = result.context();            // Combined context text
String answer = result.answer();              // LLM-generated answer
Map<String, Object> metadata = result.metadata(); // Retrieval metadata
```
