# Reasoning Patterns Taxonomy

Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research

---

## Pattern Hierarchy

```
                    Graph of Thoughts (GoT)
                           |
          +----------------+----------------+
          |                |                |
    Chain-of-Thought   Tree-of-Thoughts   Self-Discover
    (Linear)           (Branching)        (Meta-reasoning)
```

## Core Reasoning Patterns

### 1. Chain-of-Thought (CoT)
- **Origin:** Wei et al. (2022), NeurIPS
- **arXiv:** 2201.11903
- **Pattern:** Linear reasoning: Thought 1 → Thought 2 → Thought 3 → Solution
- **Best for:** Simple, linear problems where one path suffices
- **Cost:** 1x baseline

### 2. Tree-of-Thoughts (ToT)
- **Origin:** Yao et al. (2023), NeurIPS 2023
- **arXiv:** 2305.10601
- **Pattern:** Branching exploration: Root → Branch 1/2/3 → Sub-branches → Solutions
- **Best for:** Problems with multiple valid solution paths
- **Cost:** 3-10x baseline

### 3. Graph of Thoughts (GoT)
- **Origin:** Besta et al. (2024), AAAI 2024
- **arXiv:** 2308.09687
- **Pattern:** Arbitrary graph structure with branching, aggregation, refinement, and looping
- **Best for:** Complex, interdependent reasoning; insight synthesis from multiple angles
- **Cost:** 5-20x baseline

### 4. Language Agent Tree Search (LATS)
- **Origin:** Zhou et al. (2023), University of Illinois
- **arXiv:** 2310.04406
- **Pattern:** Monte Carlo Tree Search with LLM self-reflection for node evaluation
- **Best for:** High-value complex reasoning where systematic exploration is needed
- **Cost:** 5-20x baseline (emerging, limited production adoption)

### 5. Self-Discover: Self-Composed Reasoning Structures
- **Origin:** Google DeepMind (2024)
- **arXiv:** 2402.03620
- **Pattern:** LLMs discover their own reasoning structures through self-analysis
- **Best for:** Adapting reasoning strategy to problem type automatically

### 6. ReAct: Reasoning + Acting
- **Origin:** Yao et al. (2022), ICLR 2023
- **arXiv:** 2210.03629
- **Pattern:** Thought → Action → Observation (TAO) loop
- **Best for:** Foundational pattern; tasks requiring reasoning traces with action execution
- **Citations:** 9,518+

## Performance Comparison

| Pattern | Token Usage | Latency | Best For |
|---------|-------------|---------|----------|
| CoT | 1x | Low | Simple linear problems |
| ToT | 3-10x | Medium | Multiple solution paths |
| GoT | 5-20x | High | Complex interdependent reasoning |
| LATS | 5-20x | High | Systematic search with evaluation |
| ReAct | 1-3x | Low-Medium | Reasoning with action execution |

## Key Academic Sources

| Paper | Authors | Year | Link |
|-------|---------|------|------|
| Chain-of-Thought Prompting | Wei et al. | 2022 | [arXiv:2201.11903](https://arxiv.org/abs/2201.11903) |
| Self-Consistency Improves CoT | Wang et al. | 2022 | [arXiv:2203.11171](https://arxiv.org/abs/2203.11171) |
| Tree of Thoughts | Yao et al. | 2023 | [arXiv:2305.10601](https://arxiv.org/abs/2305.10601) |
| Graph of Thoughts | Besta et al. | 2024 | [arXiv:2308.09687](https://arxiv.org/abs/2308.09687) |
| Language Agent Tree Search | Zhou et al. | 2023 | [arXiv:2310.04406](https://arxiv.org/abs/2310.04406) |
| ReAct | Yao et al. | 2022 | [arXiv:2210.03629](https://arxiv.org/abs/2210.03629) |
| Self-Discover | Google DeepMind | 2024 | [arXiv:2402.03620](https://arxiv.org/abs/2402.03620) |

## Framework Support

| Framework | GoT | LATS | ToT | ReAct |
|-----------|-----|------|-----|-------|
| LangGraph | Native | Partial | Partial | Native |
| LlamaIndex | Partial | None | Partial | Partial |
| AutoGen | Partial | None | Partial | Partial |
| CrewAI | Partial | None | Partial | Partial |

---

*Source: https://github.com/nibzard/awesome-agentic-patterns/tree/main/research*
*Research reports: graph-of-thoughts-report.md, language-agent-tree-search-lats-report.md, self-discover-reasoning-structures.md*
