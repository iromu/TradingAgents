# Knowledge Retrieval (RAG) Pattern

**Source:
** [promptadvisers/agentic-design-patterns-docs](https://github.com/promptadvisers/agentic-design-patterns-docs) — MIT
License

## Pattern

```
Index → Query → Retrieve → [Augment Prompt] → Generate → Cite Sources
```

Ground AI responses in external knowledge sources by retrieving relevant
documents and augmenting the prompt with retrieved context before generation.

## When to Use

- Dynamic knowledge needs: Accessing up-to-date information
- Large document collections: Querying extensive knowledge bases
- Domain-specific applications: Specialized knowledge integration
- Factual accuracy requirements: Grounding responses in sources
- Citation requirements: Providing verifiable references
- Reducing hallucinations: Ensuring factual responses

## Where It Fits

- Enterprise search: Internal document retrieval systems
- Customer support: Knowledge base querying
- Research assistants: Academic paper retrieval
- Legal research: Case law and statute searching
- Technical documentation: API and product documentation access

## Pros

- **Accuracy** — Responses grounded in real sources
- **Verifiability** — Citations enable fact-checking
- **Scalability** — Handle vast document collections
- **Currency** — Access to latest information
- **Domain expertise** — Specialized knowledge integration
- **Reduced hallucination** — Less fabrication of facts
- **Flexibility** — Easy to update knowledge base

## Cons

- **Infrastructure needs** — Requires vector databases and storage
- **Processing overhead** — Embedding and indexing costs
- **Retrieval quality** — Dependent on chunking and matching
- **Context limitations** — Retrieved chunks may lack context
- **Latency** — Additional retrieval step adds delay
- **Maintenance** — Knowledge base needs regular updates
- **Relevance challenges** — May retrieve irrelevant information

## Implementation

```
# Example: RAG pipeline
class RAGSystem:
    def __init__(self, vector_db, embedding_model, llm):
        self.db = vector_db
        self.embedder = embedding_model
        self.llm = llm

    def ingest(self, documents):
        """Index documents into vector store."""
        for doc in documents:
            chunks = chunk(doc.content)
            embeddings = self.embedder.encode(chunks)
            self.db.upsert(doc.id, chunks, embeddings)

    def retrieve(self, query, top_k=5):
        """Retrieve relevant documents for a query."""
        query_embedding = self.embedder.encode(query)
        return self.db.search(query_embedding, k=top_k)

    def generate(self, query, source_documents=None):
        """Generate response grounded in retrieved sources."""
        if source_documents:
            context = "\n\n".join(
                f"[{i}] {doc.content}" for i, doc in enumerate(source_documents, 1)
            )
            prompt = f"Context:\n{context}\n\nQuestion: {query}\n\nAnswer with citations:"
        else:
            prompt = query

        return self.llm.generate(prompt, source_documents=source_documents)
```

## Real-World Examples

1. **Enterprise KM**: Index policies/procedures → Retrieve HR guidelines → Search tech docs → Access project data →
   Sourced answers
2. **Legal Research**: Index case law/statutes → Retrieve precedents → Search commentary → Find similar cases → Generate
   briefs with citations
3. **Medical Info**: Index medical literature → Retrieve treatment guidelines → Search drug interactions → Access
   clinical trials → Evidence-based recommendations
4. **Academic Research**: Index research papers → Retrieve relevant studies → Search across disciplines → Find citation
   networks → Generate literature reviews
5. **Tech Support**: Index product docs → Retrieve troubleshooting → Search error codes → Access config examples →
   Solution steps with references
6. **News Aggregation**: Index articles real-time → Retrieve relevant coverage → Search archives → Find related
   stories → Summaries with sources
