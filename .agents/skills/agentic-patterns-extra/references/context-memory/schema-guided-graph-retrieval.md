---
title: "Schema-Guided Graph Retrieval for Multi-Hop Reasoning"
status: "emerging"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["Youtu-GraphRAG (Tencent)"]
category: "Context & Memory"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/schema-guided-graph-retrieval.md"
tags: [graphrag, schema, multi-hop, retrieval, knowledge-graph]
---

## Problem

Complex QA over private or domain-specific corpora often needs more structure than flat chunk retrieval, but naive GraphRAG systems still fail in predictable ways:
- **Retrieval is too broad:** entity, relation, keyword, and summary nodes all compete during search
- **Question decomposition is disconnected from storage:** planner doesn't know which entity types exist
- **Domain transfer is expensive:** each new corpus needs hand-tuned ontology work
- **Large graphs become hard to navigate:** lack higher-level abstractions for routing

## Solution

Treat the schema as the control surface for the entire GraphRAG pipeline:

1. **Graph construction:** Define seed entity types, relations, and attributes
2. **Schema evolution:** Let extraction propose high-confidence additions for new domains
3. **Hierarchical graph organization:** Build keyword/community layers for multi-level navigation
4. **Query decomposition:** Prompt agent with schema to produce focused sub-questions
5. **Typed retrieval:** Filter/bias retrieval toward schema types before scoring
6. **Parallel evidence gathering:** Run decomposed sub-questions concurrently, merge evidence

**Key insight:** Reuse one schema across ingestion, planning, and retrieval so the system can ask better sub-questions, search a narrower part of the graph, and adapt to new domains without redesigning the whole stack.

## Evidence

- **Evidence Grade:** `emerging`
- **Production Implementation:** Youtu-GraphRAG (Tencent)

## How to use it

1. Start with a **small seed schema** — define only entity/relation types that materially improve retrieval
2. Store `schema_type` on extracted nodes and relations
3. Have the decomposer return both **sub-questions** and **involved schema types**
4. Apply typed filtering or ranking before global semantic search
5. Add keyword/community layers only after the base graph works
6. Put strict thresholds around schema evolution

## Trade-offs

**Pros:** Improves retrieval precision, makes multi-hop questions easier, cleaner domain-transfer path, more interpretable reasoning traces.
**Cons:** Requires upfront schema design and governance, bad schema choices can hide evidence, more moving parts than simple vector search.

## References

- [Youtu-GraphRAG repository](https://github.com/TencentCloudADP/youtu-graphrag)
- [Youtu-GraphRAG paper on arXiv](https://arxiv.org/abs/2508.19855)
- Related: [Agentic Search Over Vector Embeddings](../tool-use-environment/agentic-search-over-vector-embeddings.md)
