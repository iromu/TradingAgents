# RAG Reference

Agentic Retrieval-Augmented Generation. See SKILL.md for the core workflow.

## Agentic RAG Architecture

Unlike traditional RAG that performs a single retrieval step, Embabel's RAG is **entirely agentic and tool-based**. The LLM has full control over the retrieval process:

- **Autonomous Search**: LLM decides when to search, what queries to use, how many results
- **Iterative Refinement**: LLM can perform multiple searches with different queries
- **Cross-Reference Discovery**: LLM can follow references, expand chunks, zoom out to parent sections
- **HyDE Support**: LLM can generate hypothetical documents (HyDE queries) for better semantic search

## Facade Pattern

`ToolishRag` acts as a facade that:

1. **Inspects Store Capabilities**: Examines which `SearchOperations` subinterfaces the store implements
2. **Exposes Appropriate Tools**: Only creates tool wrappers for supported operations
3. **Provides Consistent Interface**: All tools use the same parameter patterns

Supported search operations:
| Operation | Tool Class | Description |
|-----------|-----------|-------------|
| `VectorSearch` | `VectorSearchTools` | Semantic vector search |
| `TextSearch` | `TextSearchTools` | Full-text search |
| `ResultExpander` | `ResultExpanderTools` | Expand chunks, see surrounding context |
| `RegexSearchOperations` | `RegexSearchTools` | Regex-based search |

## LlmReference Integration

Attach RAG stores to LLM calls via `LlmReference`:

```java
LlmReference ragRef = LlmReference.builder()
    .description("Search the knowledge base")
    .rag(toolishRag)
    .build();

var response = ai.withDefaultLlm()
    .withReference(ragRef)
    .respond(userPrompt);
```

## Key Points

- RAG is agentic: LLM controls search, not code
- `ToolishRag` facade auto-discovers store capabilities
- Use `LlmReference` with `.rag()` to attach RAG stores
- Supports vector, text, regex search, and result expansion
- LLM can iteratively refine queries until it finds relevant information